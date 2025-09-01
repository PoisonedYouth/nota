package com.poisonedyouth.nota.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val sessionAuthenticationFilter: SessionAuthenticationFilter,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/auth/**")
                    .permitAll() // Allow access to custom auth endpoints
                    .requestMatchers("/api/auth/**")
                    .permitAll() // Allow access to REST API auth endpoints
                    .requestMatchers("/css/**", "/js/**", "/images/**")
                    .permitAll() // Allow static resources
                    .requestMatchers("/actuator/**")
                    .permitAll() // Allow actuator endpoints
                    .anyRequest()
                    .authenticated()
            }.addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .formLogin { form ->
                form.disable() // Disable Spring Security's default login form
            }.httpBasic { basic ->
                basic.disable() // Disable HTTP Basic authentication
            }.csrf { csrf ->
                csrf.disable() // Disable CSRF for simplicity (consider enabling in production)
            }.sessionManagement { session ->
                session.maximumSessions(1).maxSessionsPreventsLogin(false)
            }

        return http.build()
    }
}
