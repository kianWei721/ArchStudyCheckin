package com.archstudy.checkin.auth.service.impl;

import com.archstudy.checkin.auth.dto.*;
import com.archstudy.checkin.auth.entity.AppUser;
import com.archstudy.checkin.auth.mapper.AppUserMapper;
import com.archstudy.checkin.auth.service.AuthService;
import com.archstudy.checkin.common.BusinessException;
import com.archstudy.checkin.common.ErrorCode;
import com.archstudy.checkin.security.JwtTokenProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public LoginResponse register(RegisterRequest request) {
        // Check if username already exists
        LambdaQueryWrapper<AppUser> usernameQuery = new LambdaQueryWrapper<>();
        usernameQuery.eq(AppUser::getUsername, request.getUsername());
        if (appUserMapper.selectCount(usernameQuery) > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // Check if email already exists
        LambdaQueryWrapper<AppUser> emailQuery = new LambdaQueryWrapper<>();
        emailQuery.eq(AppUser::getEmail, request.getEmail());
        if (appUserMapper.selectCount(emailQuery) > 0) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // Create user
        AppUser user = new AppUser();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setNickname(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeleted(0);
        appUserMapper.insert(user);

        // Generate token
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getNickname());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // Find user (including status check)
        LambdaQueryWrapper<AppUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppUser::getUsername, request.getUsername());
        AppUser user = appUserMapper.selectOne(queryWrapper);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // Check if user is disabled
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        // Generate token
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getNickname());
    }

    @Override
    public CurrentUserResponse getCurrentUser(Long userId) {
        AppUser user = appUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return new CurrentUserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getNickname());
    }
}
