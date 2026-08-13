package com.spring.camelsvsdwarfs.controller;

import com.spring.camelsvsdwarfs.dto.RegisterRequest;
import com.spring.camelsvsdwarfs.entity.User;
import com.spring.camelsvsdwarfs.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/register")
    public User register(@Valid @RequestBody RegisterRequest request){
        return authService.register(request);
    }
}
