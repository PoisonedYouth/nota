package com.poisonedyouth.nota.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.header.writers.StaticHeadersWriter
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

                // Use CookieCsrfTokenRepository so frontend can read token from cookie
                val csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse().apply {
                    // Harden CSRF cookie attributes
                    setCookieCustomizer { builder ->
                        builder
                            .path("/")
                            .sameSite("Lax")
                            .secure(true)
                    }
                }

                csrf
                    .csrfTokenRepository(csrfRepository)
                    .ignoringRequestMatchers(
                        AntPathRequestMatcher("/auth/**"),
                        AntPathRequestMatcher("/api/auth/**"),
                        htmxRequestMatcher,
                    )
            }
            .headers { headers ->
                // X-Content-Type-Options: nosniff
                headers.contentTypeOptions { }
                // X-Frame-Options: DENY (prevent clickjacking)
                headers.frameOptions { it.deny() }
                // Strict CSP aligned with our static asset usage
                headers.addHeaderWriter(
                    StaticHeadersWriter(
                        "Content-Security-Policy",
                        cspPolicy()
                    )
                )
                // Defensive additional headers
                headers.referrerPolicy { it.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN) }
                // Note: Permissions-Policy toggled via custom writer to avoid deprecated API
                headers.addHeaderWriter(
                    StaticHeadersWriter(
                        "Permissions-Policy",
                        "geolocation=(), microphone=(), camera=()"
                    )
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

    private fun cspPolicy(): String = buildString {
        append("default-src 'self'; ")
        append("base-uri 'self'; ")
        append("object-src 'none'; ")
        append("frame-ancestors 'none'; ") // also enforces anti-framing like X-Frame-Options
        append("img-src 'self' data:; ")
        append("font-src 'self'; ")
        append("style-src 'self' 'unsafe-inline'; ") // allow inline styles if templates require; revisit to tighten
        append("script-src 'self'; ")
        append("connect-src 'self'; ")
        append("form-action 'self'; ")
    }
}
