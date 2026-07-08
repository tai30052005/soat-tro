package com.example.soattro.service;

import com.example.soattro.dto.request.LoginRequest;
import com.example.soattro.dto.request.RegisterRequest;
import com.example.soattro.dto.response.AuthResponse;
import com.example.soattro.entity.User;
import com.example.soattro.exception.EmailAlreadyUsedException;
import com.example.soattro.repository.UserRepository;
import com.example.soattro.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tầng Service: logic nghiệp vụ đăng ký/đăng nhập.
 * Controller chỉ gọi xuống đây, không tự xử lý logic.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    /** Đăng ký: kiểm tra email trùng -> băm mật khẩu -> lưu user -> phát token. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyUsedException(request.email());
        }

        // KHÔNG bao giờ lưu mật khẩu thô — luôn băm bằng BCrypt.
        String hashed = passwordEncoder.encode(request.password());
        User user = new User(request.email(), hashed);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, "Bearer", user.getEmail());
    }

    /**
     * Đăng nhập: AuthenticationManager kiểm tra email + mật khẩu.
     * Sai -> BadCredentialsException -> GlobalExceptionHandler trả 401.
     */
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        // Tới được đây nghĩa là email/mật khẩu hợp lệ.
        String token = jwtUtil.generateToken(request.email());
        return new AuthResponse(token, "Bearer", request.email());
    }
}
