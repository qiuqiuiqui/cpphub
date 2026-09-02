package com.cpphub.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfig {
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // CSRF disabled for simplicity in MVP; enable in production
        .csrf(AbstractHttpConfigurer::disable)
        .securityContext(security
            -> security.securityContextRepository(new HttpSessionSecurityContextRepository()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .exceptionHandling(
            ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .authorizeHttpRequests(auth
            -> auth
                // Public endpoints
                .requestMatchers("/", "/index.html", "/assets/**")
                .permitAll()
                .requestMatchers("/api/posts", "/api/posts/**", "/api/posts/search", "/api/tags")
                .permitAll()
                // Agent (local-only, auth via JWT inside the WSS handshake / pairing code)
                .requestMatchers("/api/agent/pair", "/api/agent/ws/**")
                .permitAll()
                // Admin endpoints
                .requestMatchers("/api/admin/login")
                .permitAll()
                .requestMatchers("/api/admin/**")
                .hasRole("ADMIN")
                // SpringDoc
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html")
                .permitAll()
                // Default: permit all (frontend is served separately)
                .anyRequest()
                .permitAll())
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable);
    return http.build();
  }
}