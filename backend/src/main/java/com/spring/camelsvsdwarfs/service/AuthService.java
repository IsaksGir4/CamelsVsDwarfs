package com.spring.camelsvsdwarfs.service;

import com.spring.camelsvsdwarfs.dto.RegisterRequest;
import com.spring.camelsvsdwarfs.entity.Role;
import com.spring.camelsvsdwarfs.entity.User;
import com.spring.camelsvsdwarfs.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.VIEWER);
        user.setRegisterDate(LocalDateTime.now());

        return userRepository.save(user);
    }
}