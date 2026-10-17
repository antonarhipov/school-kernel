package org.schoolkernel.workspace;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("demo")
public record DemoSettings(
        List<String> publicHosts,
        List<String> trustedProxyAddresses,
        boolean railwayIsolatedIngress,
        Duration idleTimeout,
        Duration absoluteTimeout,
        int maxWorkspaces) {
    /** Local development is selected only by an exclusively loopback host configuration. */
    boolean localOnly() {
        return !railwayIsolatedIngress && trustedProxyAddresses.stream().allMatch(String::isBlank)
                && publicHosts.stream().allMatch(host -> host.matches("(?:localhost|127\\.0\\.0\\.1)(?::[0-9]{1,5})?"));
    }

    public DemoSettings {
        publicHosts = publicHosts == null ? List.of() : List.copyOf(publicHosts);
        trustedProxyAddresses = trustedProxyAddresses == null ? List.of() : List.copyOf(trustedProxyAddresses);
        if (railwayIsolatedIngress && trustedProxyAddresses.stream().anyMatch(address -> !address.isBlank())) {
            throw new IllegalArgumentException("Isolated Railway ingress and proxy address trust are mutually exclusive");
        }
        if (publicHosts.isEmpty() || publicHosts.stream().anyMatch(host ->
                !host.matches("[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?(?::[0-9]{1,5})?"))) {
            throw new IllegalArgumentException("demo.public-hosts must explicitly list permitted lowercase host authorities");
        }
        if (idleTimeout == null || idleTimeout.isNegative() || idleTimeout.isZero()
                || absoluteTimeout == null || absoluteTimeout.isNegative() || absoluteTimeout.isZero()
                || maxWorkspaces < 1) {
            throw new IllegalArgumentException("Demo session limits must be positive");
        }
    }
}
