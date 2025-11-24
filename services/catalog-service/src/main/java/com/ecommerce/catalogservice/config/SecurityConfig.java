package com.ecommerce.catalogservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("local") // only active when `spring.profiles.active=local` to allow dev convenience
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/catalog/**", "/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf.disable())
            // keep default httpBasic so other endpoints remain protected if needed in dev
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
