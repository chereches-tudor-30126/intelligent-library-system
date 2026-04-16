package com.library.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // enables @PreAuthorize on controllers
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CustomUserDetailsService userDetailsService;

    // -------------------------------------------------------------------------
    // Endpoints that do NOT require a valid JWT
    // -------------------------------------------------------------------------
    private static final String[] PUBLIC_POST = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh"
    };

    private static final String[] PUBLIC_GET = {
            "/api/books",           // browse catalog without login
            "/api/books/**",        // book details
            "/api/authors",
            "/api/authors/**",
            "/v3/api-docs/**",      // Swagger / OpenAPI
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health"
    };

    // -------------------------------------------------------------------------
    // Security filter chain
    // -------------------------------------------------------------------------

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — we use stateless JWT, not cookies
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless session — Spring Security will never create an HttpSession
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()

                        // Admin-only endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/audit/**").hasRole("ADMIN")

                        // Librarian + Admin can manage books
                        .requestMatchers(HttpMethod.POST, "/api/books/**")
                        .hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.PUT, "/api/books/**")
                        .hasAnyRole("ADMIN", "LIBRARIAN")
                        .requestMatchers(HttpMethod.DELETE, "/api/books/**")
                        .hasAnyRole("ADMIN", "LIBRARIAN")

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                // Plug in our custom AuthenticationProvider
                .authenticationProvider(authenticationProvider())

                // Add JWT filter BEFORE Spring's default username/password filter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // -------------------------------------------------------------------------
    // Beans
    // -------------------------------------------------------------------------

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt with strength 12 — good balance of security vs. performance
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}