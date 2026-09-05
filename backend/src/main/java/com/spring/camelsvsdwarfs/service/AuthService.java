package com.spring.camelsvsdwarfs.service;

import com.spring.camelsvsdwarfs.dto.RegisterRequest;
import com.spring.camelsvsdwarfs.entity.User;
import com.spring.camelsvsdwarfs.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public User register(RegisterRequest request){
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(request.getPassword());
        user.setRole(request.getRole());

        return userRepository.save(user);
    }
}
