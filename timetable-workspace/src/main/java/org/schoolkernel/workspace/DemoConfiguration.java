package org.schoolkernel.workspace;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

@Configuration
@Profile("demo")
@EnableConfigurationProperties(DemoSettings.class)
class DemoConfiguration {
    @Bean org.springframework.beans.factory.InitializingBean isolatedIngressBoundary(
            DemoSettings settings, org.springframework.core.env.Environment environment) {
        return () -> {
            if (!settings.railwayIsolatedIngress()) return;
            for (String key : java.util.List.of("RAILWAY_PROJECT_ID", "RAILWAY_ENVIRONMENT_ID", "RAILWAY_SERVICE_ID")) {
                String value = environment.getProperty(key, "");
                if (!value.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                    throw new IllegalStateException("Isolated Railway ingress requires Railway deployment identity");
                }
            }
            String domain = environment.getProperty("RAILWAY_PUBLIC_DOMAIN", "");
            if (!domain.endsWith(".up.railway.app") || !settings.publicHosts().equals(java.util.List.of(domain))) {
                throw new IllegalStateException("Isolated Railway ingress requires its exact generated public domain");
            }
        };
    }

    /** Preserve the existing services' transaction boundaries without loading a datasource. */
    @Bean AbstractPlatformTransactionManager transactionManager(DemoWorkspaces workspaces) {
        return new AbstractPlatformTransactionManager() {
            private final ThreadLocal<Transaction> current = new ThreadLocal<>();
            @Override protected Object doGetTransaction() {
                Transaction active = current.get();
                return active != null ? active : new Transaction(workspaces.current());
            }
            @Override protected boolean isExistingTransaction(Object transaction) {
                return ((Transaction) transaction).active;
            }
            @Override protected void doBegin(Object transaction, TransactionDefinition definition) {
                Transaction tx = (Transaction) transaction;
                tx.entry.lock.lock();
                tx.before = tx.entry.aggregate;
                tx.active = true;
                current.set(tx);
            }
            @Override protected void doCommit(DefaultTransactionStatus status) {}
            @Override protected void doRollback(DefaultTransactionStatus status) {
                Transaction tx = (Transaction) status.getTransaction();
                tx.entry.aggregate = tx.before;
            }
            @Override protected void doSetRollbackOnly(DefaultTransactionStatus status) {
                ((Transaction) status.getTransaction()).rollbackOnly = true;
            }
            @Override protected void doCleanupAfterCompletion(Object transaction) {
                Transaction tx = (Transaction) transaction;
                current.remove();
                tx.active = false;
                tx.entry.lock.unlock();
            }
        };
    }

    private static final class Transaction implements org.springframework.transaction.support.SmartTransactionObject {
        final DemoWorkspaces.Entry entry;
        WorkspaceAggregate before;
        boolean active;
        boolean rollbackOnly;
        Transaction(DemoWorkspaces.Entry entry) { this.entry = entry; }
        @Override public boolean isRollbackOnly() { return rollbackOnly; }
    }
}
