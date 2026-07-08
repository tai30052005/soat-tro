package com.example.soattro.repository;

import com.example.soattro.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository: Spring Data JPA tự sinh câu SQL từ TÊN method
 * (findByEmail -> SELECT ... WHERE email = ?).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
