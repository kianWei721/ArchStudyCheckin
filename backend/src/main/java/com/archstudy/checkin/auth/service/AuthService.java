package com.archstudy.checkin.auth.service;

import com.archstudy.checkin.auth.dto.*;

public interface AuthService {

    LoginResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    CurrentUserResponse getCurrentUser(Long userId);
}
