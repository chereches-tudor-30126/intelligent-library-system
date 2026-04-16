package com.library.service.auth;

import com.library.dto.request.LoginRequest;
import com.library.dto.request.RefreshTokenRequest;
import com.library.dto.request.RegisterRequest;
import com.library.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);

    void logoutAllSessions(String email);
}