package com.alin.lin.config;

import com.alin.lin.dto.ResponseBodyDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final PosCorsProperties corsProperties;
    private final PosSecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    public SecurityConfig(
            PosCorsProperties corsProperties,
            PosSecurityProperties securityProperties,
            ObjectMapper objectMapper
    ) {
        this.corsProperties = corsProperties;
        this.securityProperties = securityProperties;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);

        if (!securityProperties.isEnabled()) {
            return http
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }

        return http
                .httpBasic(basic -> {})
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeSecurityError(response, HttpServletResponse.SC_UNAUTHORIZED, "尚未登入或帳號密碼錯誤"))
                        .accessDeniedHandler((request, response, exception) ->
                                writeSecurityError(response, HttpServletResponse.SC_FORBIDDEN, "沒有執行此作業的權限"))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/change-cases/*/status").hasRole("REVIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/change-cases", "/api/change-cases/**").hasRole("MAKER")
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("MAKER", "REVIEWER")
                        .anyRequest().denyAll()
                )
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "pos.security", name = "enabled", havingValue = "true")
    public UserDetailsService userDetailsService() {
        requireCredential(securityProperties.getMakerUsername(), "POS_MAKER_USERNAME");
        requireCredential(securityProperties.getMakerPassword(), "POS_MAKER_PASSWORD");
        requireCredential(securityProperties.getReviewerUsername(), "POS_REVIEWER_USERNAME");
        requireCredential(securityProperties.getReviewerPassword(), "POS_REVIEWER_PASSWORD");
        return new InMemoryUserDetailsManager(
                User.withUsername(securityProperties.getMakerUsername())
                        .password(passwordEncoder().encode(securityProperties.getMakerPassword()))
                        .roles("MAKER")
                        .build(),
                User.withUsername(securityProperties.getReviewerUsername())
                        .password(passwordEncoder().encode(securityProperties.getReviewerPassword()))
                        .roles("REVIEWER")
                        .build()
        );
    }

    @Bean
    @ConditionalOnProperty(prefix = "pos.security", name = "enabled", havingValue = "false", matchIfMissing = true)
    public UserDetailsService localUserDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void requireCredential(String value, String environmentName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("啟用 POS Security 時必須設定 " + environmentName);
        }
    }

    private void writeSecurityError(HttpServletResponse response, int status, String errorMessage) throws java.io.IOException {
        response.setStatus(status);
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ResponseBodyDto.builder()
                .success(false)
                .message("")
                .massageCode("")
                .errorMessage(errorMessage)
                .data(null)
                .build());
    }
}
