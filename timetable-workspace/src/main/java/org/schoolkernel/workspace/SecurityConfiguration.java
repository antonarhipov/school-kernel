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
                        .requestMatchers(HttpMethod.GET, "/", "/workspace/**", "/api/csrf", "/api/workspace").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/import").permitAll()
                        .anyRequest().denyAll())
                .csrf(Customizer.withDefaults())
                .exceptionHandling(errors -> errors.accessDeniedHandler(accessDeniedHandler(problems)))
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
