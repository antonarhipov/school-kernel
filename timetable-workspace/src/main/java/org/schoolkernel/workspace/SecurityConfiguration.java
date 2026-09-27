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
            HttpSecurity http, HostOriginFilter hostOriginFilter, ProblemResponder problems) throws Exception {
        return http
                .cors(cors -> cors.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(login -> login.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/", "/workspace/**", "/api/csrf", "/api/workspace",
                                "/api/runs/*", "/api/proposal", "/api/accepted/export").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/import", "/api/initial-draft/replace",
                                "/api/repair-draft", "/api/repair-draft/bulk-pin-preview",
                                "/api/runs", "/api/proposal/accept",
                                "/api/workspace/clear", "/api/workspace/upload-definition",
                                "/api/manual-draft", "/api/manual-draft/publish").permitAll()
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
