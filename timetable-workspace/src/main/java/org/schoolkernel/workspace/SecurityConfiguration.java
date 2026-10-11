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
            HttpSecurity http, HostOriginFilter hostOriginFilter, ProblemResponder problems, WorkspaceRepository repository) throws Exception {
        return http
                .cors(cors -> cors.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(login -> login.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/", "/workspace/**", "/api/csrf", "/api/workspace",
                                "/api/runs/*", "/api/proposal", "/api/accepted/export", "/api/definition-draft/requirements").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/import", "/api/initial-draft/replace",
                                "/api/repair-draft", "/api/repair-draft/bulk-pin-preview",
                                "/api/runs", "/api/proposal/accept",
                                "/api/workspace/clear", "/api/workspace/upload-definition",
                                "/api/manual-draft", "/api/manual-draft/publish",
                                "/api/definition-draft", "/api/definition-draft/preview", "/api/definition-draft/school", "/api/definition-draft/grid", "/api/definition-draft/grid/preview", "/api/definition-draft/subjects", "/api/definition-draft/teachers", "/api/definition-draft/cohorts", "/api/definition-draft/rooms", "/api/definition-draft/room-policies", "/api/definition-draft/preferences", "/api/definition-draft/finish", "/api/definition-draft/finish/preview", "/api/definition-draft/restoration", "/api/definition-draft/restoration/inspect", "/api/definition-draft/restoration/preview", "/api/definition-draft/removal", "/api/definition-draft/removal/inspect", "/api/definition-draft/removal/preview", "/api/definition-draft/catalog", "/api/definition-draft/catalog/preview", "/api/definition-draft/requirements", "/api/definition-draft/requirements/preview", "/api/definition-draft/occurrences", "/api/definition-draft/occurrences/preview").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/repair-draft", "/api/manual-draft").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/repair-draft", "/api/runs/*", "/api/proposal", "/api/workspace", "/api/manual-draft").permitAll()
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
                .addFilterBefore(hostOriginFilter, BasicAuthenticationFilter.class)
                .addFilterAfter(new DefinitionDraftAccess(repository, problems),
                        org.springframework.security.web.access.intercept.AuthorizationFilter.class)
                .build();
    }

    private static AccessDeniedHandler accessDeniedHandler(ProblemResponder problems) {
        return (request, response, exception) -> problems.write(
                response,
                HttpStatus.FORBIDDEN,
                "REQUEST_FORBIDDEN",
                "The local request was not authorized.");
    }
}
