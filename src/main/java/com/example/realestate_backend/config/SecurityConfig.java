package com.example.realestate_backend.config;

import com.example.realestate_backend.entity.User;
import com.example.realestate_backend.repository.UserRepository;
import com.example.realestate_backend.security.JwtAuthenticationFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {

        http
                // Ignore CSRF for REST API endpoints
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**")
                        // Only cryptographically validated Bearer uploads bypass CSRF.
                        // Browser sessions and invalid Bearer headers still require it.
                        .ignoringRequestMatchers(request ->
                                "POST".equals(request.getMethod())
                                && request.getRequestURI().equals(request.getContextPath() + "/properties/create")
                                && Boolean.TRUE.equals(request.getAttribute(
                                        JwtAuthenticationFilter.VALIDATED_BEARER_ATTRIBUTE)))
                )

                .authorizeHttpRequests(auth -> auth

                        // JWT authentication APIs are public
                        .requestMatchers("/api/auth/**").permitAll()

                        // Property creation requires authentication
                        .requestMatchers("/properties/create").authenticated()

                        // Public pages
                        .requestMatchers(
                                HttpMethod.GET,
                                "/",
                                "/properties/{id}",
                                "/css.css",
                                "/js.js",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        // Registration, login and error pages
                        .requestMatchers(
                                "/register",
                                "/login",
                                "/error"
                        ).permitAll()

                        // Admin routes
                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")

                        // Everything else requires authentication
                        .anyRequest()
                        .authenticated()
                )

                .exceptionHandling(errors -> errors
                        .defaultAuthenticationEntryPointFor((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Authentication required\"}");
                        }, request -> request.getRequestURI().startsWith(request.getContextPath() + "/api/"))
                        .defaultAuthenticationEntryPointFor(
                                new org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint("/login"),
                                request -> true)
                        .defaultAccessDeniedHandlerFor((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Access denied\"}");
                        }, request -> request.getRequestURI().startsWith(request.getContextPath() + "/api/"))
                )

                // Thymeleaf form login
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )

                // JWT filter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        CsrfFilter.class
                );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(
            UserRepository userRepository) {

        return email -> {

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() ->
                            new UsernameNotFoundException("User not found")
                    );

            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getEmail())
                    .password(user.getPassword())
                    .authorities(user.getRole().name())
                    .build();
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
