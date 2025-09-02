package com.poisonedyouth.nota.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher

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
            }
            .addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .formLogin { form ->
                form.disable() // Disable Spring Security's default login form
            }
            .httpBasic { basic ->
                basic.disable() // Disable HTTP Basic authentication
            }
            .csrf { csrf ->
                val htmxRequestMatcher = RequestMatcher { request ->
                    val header = request.getHeader("HX-Request")
                    header != null && header.equals("true", ignoreCase = true)
                }
                csrf
                    .ignoringRequestMatchers(
                        AntPathRequestMatcher("/auth/**"),
                        AntPathRequestMatcher("/api/auth/**"),
                        htmxRequestMatcher,
                    )
            }
            .sessionManagement { session ->
                session
                    .maximumSessions(1).maxSessionsPreventsLogin(false)
                // Defensive: if future Spring-authenticated logins are used, migrate session on authentication
                session.sessionFixation { it.migrateSession() }
            }

        return http.build()
    }
}
