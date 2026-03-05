package com.dtt.organization.security;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
public class SecurityConfig {
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/", "/login","/error",
                            "/eoi-redirect", "/logout",
                            "/icons/**", "/css/**", "/js/**", "/img/**", "/assets/**",
                            "/api/public/**"
                    ).permitAll()
                    .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                    .accessDeniedHandler((request, response, accessDeniedException) ->
                            response.sendRedirect(request.getContextPath() + "/error")
                    )
                    .authenticationEntryPoint((request, response, authException) ->
                            response.sendRedirect(request.getContextPath() + "/")
                    )
            )
            .formLogin(form -> form.disable())
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                    .maximumSessions(1)
                    .maxSessionsPreventsLogin(false)
                    .expiredUrl("/?sessionExpired=true")
            );

    return http.build();
}



}

