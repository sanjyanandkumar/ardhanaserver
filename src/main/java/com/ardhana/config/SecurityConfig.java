package com.ardhana.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ardhana.filter.JwtAuthFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOriginsCsv;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**").disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))
            .authorizeHttpRequests(auth -> auth
            	    .requestMatchers("/api/auth/register").permitAll()
            	    .requestMatchers("/api/auth/login").permitAll()
            	    // Phone verification happens before an account exists, so it
            	    // can't require a JWT — MSG91 send/resend/verify OTP.
            	    .requestMatchers("/api/auth/send-otp").permitAll()
            	    .requestMatchers("/api/auth/resend-otp").permitAll()
            	    .requestMatchers("/api/auth/verify-otp").permitAll()
            	    .requestMatchers("/h2-console/**").permitAll()
            	    // Razorpay calls this server-to-server with no JWT; it's
            	    // authenticated instead via the X-Razorpay-Signature HMAC check.
            	    .requestMatchers("/api/payments/webhook").permitAll()
            	    // Spring's newer path matcher treats "/**" as requiring a trailing
            	    // segment, so the bare collection path ("/api/courses" with no
            	    // sub-path) needs to be listed explicitly alongside the wildcard.
            	    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/courses", "/api/courses/**").permitAll()
            	    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/schools", "/api/schools/**").permitAll()
            	    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/schools/students/all").authenticated()
            	    .requestMatchers("/api/lesson-attachments/**").authenticated()
            	    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/lessons", "/api/lessons/**").permitAll()
            	    .requestMatchers("/api/my-courses/**").authenticated()
            	    .requestMatchers("/api/admin/**").authenticated()
            	    .anyRequest().authenticated()
            	)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.endsWith("/") ? s.substring(0, s.length() - 1) : s)
                .toList();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
