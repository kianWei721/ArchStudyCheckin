package com.archstudy.checkin.auth.controller;

import com.archstudy.checkin.auth.dto.*;
import com.archstudy.checkin.auth.service.AuthService;
import com.archstudy.checkin.common.Result;
import com.archstudy.checkin.security.SecurityContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request);
        return Result.success(response);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    @GetMapping("/me")
    public Result<CurrentUserResponse> me() {
        Long userId = SecurityContext.getCurrentUserId();
        CurrentUserResponse response = authService.getCurrentUser(userId);
        return Result.success(response);
    }
}
