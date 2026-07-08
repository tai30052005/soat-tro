package com.example.soattro.service;

import com.example.soattro.entity.User;
import com.example.soattro.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Lấy user đang đăng nhập từ SecurityContext.
 *
 * Khác finance app: ở đây trả Optional vì soát hợp đồng CHO PHÉP ẨN DANH —
 * có token thì phân tích gắn vào user (xem lịch sử được), không có thì thôi.
 */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetails userDetails)) {
            return Optional.empty();   // request ẩn danh
        }
        return userRepository.findByEmail(userDetails.getUsername());
    }
}
