package com.alin.lin.config;

import com.alin.lin.dto.ResponseBodyDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.context.annotation.Profile;
import javax.sql.DataSource;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.ArrayList;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final PosCorsProperties corsProperties;
    private final PosSecurityProperties securityProperties;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    public SecurityConfig(
            PosCorsProperties corsProperties,
            PosSecurityProperties securityProperties,
            ObjectMapper objectMapper,
            Environment environment
    ) {
        this.corsProperties = corsProperties;
        this.securityProperties = securityProperties;
        this.objectMapper = objectMapper;
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);

        if (!securityProperties.isEnabled()) {
            if (!environment.acceptsProfiles(Profiles.of("local", "test"))) {
                throw new IllegalStateException("POS Security 只能在 local 或 test profile 關閉");
            }
            return http
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }

        if (securityProperties.isRequireHttps()) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }

        return http
                .httpBasic(basic -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
                        .requestMatchers(HttpMethod.GET, "/api/user-authorizations/codes").hasAnyRole("MAKER", "REVIEWER")
                        .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("MAKER", "REVIEWER", "USER", "ADMIN")
                        .anyRequest().denyAll()
                )
                .build();
    }

    @Bean
    @Profile({"local", "test"})
    @ConditionalOnProperty(prefix = "pos.security", name = "enabled", havingValue = "true")
    public UserDetailsService userDetailsService() {
        requireCredentialPair(
                securityProperties.getMakerUsername(),
                securityProperties.getMakerPassword(),
                "POS_MAKER_USERNAME",
                "POS_MAKER_PASSWORD"
        );
        requireCredentialPair(
                securityProperties.getReviewerUsername(),
                securityProperties.getReviewerPassword(),
                "POS_REVIEWER_USERNAME",
                "POS_REVIEWER_PASSWORD"
        );
        requireOptionalCredentialPair(securityProperties.getUserUsername(), securityProperties.getUserPassword(), "POS_USER_USERNAME", "POS_USER_PASSWORD");
        requireOptionalCredentialPair(securityProperties.getAdminUsername(), securityProperties.getAdminPassword(), "POS_ADMIN_USERNAME", "POS_ADMIN_PASSWORD");
        if (securityProperties.getMakerUsername().equals(securityProperties.getReviewerUsername())) {
            throw new IllegalStateException("經辦與覆核帳號不可相同");
        }
        List<UserDetails> users = new ArrayList<>(List.of(
                User.withUsername(securityProperties.getMakerUsername())
                        .password(passwordEncoder().encode(securityProperties.getMakerPassword()))
                        .roles("MAKER")
                        .build(),
                User.withUsername(securityProperties.getReviewerUsername())
                        .password(passwordEncoder().encode(securityProperties.getReviewerPassword()))
                        .roles("REVIEWER")
                        .build()
        ));
        addOptionalUser(users, securityProperties.getUserUsername(), securityProperties.getUserPassword(), "USER");
        addOptionalUser(users, securityProperties.getAdminUsername(), securityProperties.getAdminPassword(), "ADMIN");
        return new InMemoryUserDetailsManager(users);
    }

    @Bean
    @Profile("prod")
    @ConditionalOnProperty(prefix = "pos.security", name = "enabled", havingValue = "true")
    public UserDetailsService jdbcUserDetailsService(DataSource dataSource) {
        JdbcUserDetailsManager manager = new JdbcUserDetailsManager(dataSource);
        manager.setJdbcTemplate(new JdbcTemplate(dataSource));
        provisionUser(manager, securityProperties.getMakerUsername(), securityProperties.getMakerPassword(), "ROLE_MAKER");
        provisionUser(manager, securityProperties.getReviewerUsername(), securityProperties.getReviewerPassword(), "ROLE_REVIEWER");
        provisionUser(manager, securityProperties.getUserUsername(), securityProperties.getUserPassword(), "ROLE_USER");
        provisionUser(manager, securityProperties.getAdminUsername(), securityProperties.getAdminPassword(), "ROLE_ADMIN");
        if (securityProperties.getMakerUsername().equals(securityProperties.getReviewerUsername())) {
            throw new IllegalStateException("經辦與覆核帳號不可相同");
        }
        return manager;
    }

    @Bean
    @Profile({"local", "test"})
    @ConditionalOnProperty(prefix = "pos.security", name = "enabled", havingValue = "false")
    public UserDetailsService localUserDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    private void provisionUser(JdbcUserDetailsManager manager, String username, String password, String role) {
        requireCredentialPair(username, password, role, role);
        UserDetails user = User.withUsername(username)
                .password(passwordEncoder().encode(password))
                .roles(role.substring("ROLE_".length()))
                .build();
        if (manager.userExists(username)) {
            manager.updateUser(user);
        } else {
            manager.createUser(user);
        }
    }

    private void addOptionalUser(List<UserDetails> users, String username, String password, String role) {
        if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            users.add(User.withUsername(username).password(passwordEncoder().encode(password)).roles(role).build());
        }
    }

    private void requireOptionalCredentialPair(String username, String password, String usernameEnvironment, String passwordEnvironment) {
        if ((username == null || username.isBlank()) && (password == null || password.isBlank())) return;
        requireCredentialPair(username, password, usernameEnvironment, passwordEnvironment);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void requireCredentialPair(String username, String password, String usernameEnvironment, String passwordEnvironment) {
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("啟用 POS Security 時必須設定 " + usernameEnvironment);
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("啟用 POS Security 時必須設定 " + passwordEnvironment);
        }
        if (password.length() < 12) {
            throw new IllegalStateException(passwordEnvironment + " 至少需要 12 個字元");
        }
        if (username.equals(password)) {
            throw new IllegalStateException(passwordEnvironment + " 不可與帳號相同");
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
