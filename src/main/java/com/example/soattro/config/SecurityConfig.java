package com.example.soattro.config;

import com.example.soattro.security.CustomUserDetailsService;
import com.example.soattro.security.JwtAuthFilter;
import com.example.soattro.security.JwtUtil;
import com.example.soattro.security.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Cấu hình bảo mật: JWT stateless + CORS + BCrypt (pattern từ finance app).
 *
 * Điểm KHÁC finance app: /api/analyses/** sẽ cho phép ẩn danh (permitAll)
 * vì sản phẩm không bắt đăng nhập mới soát được hợp đồng — đăng ký chỉ để lưu
 * lịch sử. (Endpoint lịch sử /api/analyses GET sẽ tự kiểm tra auth ở chặng 6.)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    /** Danh sách origin được phép gọi API (CORS), đọc từ cấu hình. */
    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    public SecurityConfig(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    /** Định nghĩa luật bảo mật cho mọi HTTP request. */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Tự tạo filter JWT (không đánh @Component để tránh bị Spring đăng ký 2 lần)
        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtUtil, userDetailsService);

        http
                .cors(Customizer.withDefaults())
                // API REST dùng token, không dùng session/cookie -> tắt CSRF
                .csrf(AbstractHttpConfigurer::disable)
                // STATELESS: server KHÔNG lưu phiên; mỗi request tự mang token
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Công khai: auth, health, swagger
                        .requestMatchers("/api/auth/**", "/api/health", "/error").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // Soát hợp đồng KHÔNG bắt đăng nhập (zero-friction cho người thuê trọ).
                        // JwtAuthFilter vẫn chạy trước: ai có token thì phân tích được gắn vào user.
                        .requestMatchers("/api/analyses/**").permitAll()
                        // Mọi endpoint còn lại bắt buộc đăng nhập:
                        .anyRequest().authenticated())
                // Chưa xác thực mà gọi endpoint bảo vệ -> trả 401 (không phải 403)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new RestAuthenticationEntryPoint()))
                // Đặt filter JWT TRƯỚC filter đăng nhập username/password mặc định
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** CORS: cho phép trình duyệt từ origin frontend gọi API. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // "OriginPatterns" hỗ trợ cả origin chính xác lẫn wildcard (https://*.vercel.app)
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** BCrypt: băm mật khẩu một chiều + salt tự động. Chuẩn industry. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** AuthenticationManager: "bộ máy" kiểm tra email + mật khẩu khi login. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
