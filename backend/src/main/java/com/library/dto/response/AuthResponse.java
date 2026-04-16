package com.library.dto.response;

import com.library.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private long expiresIn;     // access token lifetime in seconds

    // Basic user info returned after login/register
    // (avoids an extra /me call from the frontend)
    private String userId;
    private String email;
    private String username;
    private Role role;
}