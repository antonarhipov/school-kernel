package org.schoolkernel.workspace;

import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, HostOriginFilter hostOriginFilter, ProblemResponder problems, WorkspaceStore repository,
            org.springframework.beans.factory.ObjectProvider<DemoWorkspaces> demoWorkspaces) throws Exception {
        DemoWorkspaces demo = demoWorkspaces.getIfAvailable();
        http
                .cors(cors -> cors.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(login -> login.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(request -> demo != null && "GET".equals(request.getMethod())
                                && java.util.Set.of("/healthz", "/api/demo/config").contains(request.getRequestURI())).permitAll()
                        .requestMatchers(request -> demo != null && "POST".equals(request.getMethod())
                                && java.util.Set.of("/api/demo/session", "/api/demo/definition").contains(request.getRequestURI())).permitAll()
                        .requestMatchers(request -> demo != null && "GET".equals(request.getMethod())
                                && (request.getRequestURI().equals("/api/samples") || request.getRequestURI().matches("/api/samples/[^/]+"))).permitAll()
                        .requestMatchers(request -> demo != null && "POST".equals(request.getMethod())
                                && request.getRequestURI().matches("/api/samples/[^/]+/use")).permitAll()
                        .requestMatchers(HttpMethod.GET, "/", "/workspace/**", "/api/csrf", "/api/workspace",
                                "/api/runs/*", "/api/proposal", "/api/accepted/export", "/api/definition-draft/requirements").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/import", "/api/initial-draft/replace",
                                "/api/repair-draft", "/api/repair-draft/bulk-pin-preview", "/api/repair-draft/return-to-authoring",
                                "/api/runs", "/api/proposal/accept",
                                "/api/workspace/clear", "/api/workspace/upload-definition",
                                "/api/manual-draft", "/api/manual-draft/publish",
                                "/api/definition-draft", "/api/definition-draft/preview", "/api/definition-draft/school", "/api/definition-draft/grid", "/api/definition-draft/grid/preview", "/api/definition-draft/subjects", "/api/definition-draft/teachers", "/api/definition-draft/cohorts", "/api/definition-draft/rooms", "/api/definition-draft/room-policies", "/api/definition-draft/preferences", "/api/definition-draft/finish", "/api/definition-draft/finish/preview", "/api/definition-draft/restoration", "/api/definition-draft/restoration/inspect", "/api/definition-draft/restoration/preview", "/api/definition-draft/removal", "/api/definition-draft/removal/inspect", "/api/definition-draft/removal/preview", "/api/definition-draft/catalog", "/api/definition-draft/catalog/preview", "/api/definition-draft/requirements", "/api/definition-draft/requirements/preview", "/api/definition-draft/occurrences", "/api/definition-draft/occurrences/preview").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/repair-draft", "/api/manual-draft").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/definition-draft", "/api/repair-draft", "/api/runs/*", "/api/proposal", "/api/workspace", "/api/manual-draft").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().denyAll())
                .csrf(Customizer.withDefaults())
                .exceptionHandling(errors -> errors
                        .accessDeniedHandler(accessDeniedHandler(problems))
                        .authenticationEntryPoint((request, response, exception) -> problems.write(
                                response,
                                HttpStatus.FORBIDDEN,
                                "ACCESS_DENIED",
                                "Access denied: " + exception.getMessage())))
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .referrerPolicy(policy -> policy.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'")))
                .addFilterBefore(hostOriginFilter, org.springframework.security.web.csrf.CsrfFilter.class)
                .addFilterAfter(new DefinitionDraftAccess(repository, problems),
                        org.springframework.security.web.access.intercept.AuthorizationFilter.class);
        if (demo != null) http.addFilterAfter(new DemoSessionFilter(demo, problems), HostOriginFilter.class);
        return http.build();
    }

    @Bean org.springframework.boot.web.servlet.FilterRegistrationBean<HostOriginFilter> hostFilterRegistration(HostOriginFilter filter) {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    private static AccessDeniedHandler accessDeniedHandler(ProblemResponder problems) {
        return (request, response, exception) -> problems.write(
                response,
                HttpStatus.FORBIDDEN,
                "REQUEST_FORBIDDEN",
                "The local request was not authorized.");
    }
}
