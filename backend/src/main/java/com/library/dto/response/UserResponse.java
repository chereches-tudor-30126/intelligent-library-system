package com.library.dto.response;

import com.library.entity.BookType;
import com.library.entity.Role;
import com.library.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private Role role;
    private String phoneNumber;
    private String profilePictureUrl;
    private Boolean isActive;
    private Boolean isEmailVerified;
    private OffsetDateTime lastLoginAt;
    private BookType[] preferredGenres;
    private OffsetDateTime createdAt;

    // Static factory — converts User entity to DTO safely
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .phoneNumber(user.getPhoneNumber())
                .profilePictureUrl(user.getProfilePictureUrl())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .lastLoginAt(user.getLastLoginAt())
                .preferredGenres(user.getPreferredGenres())
                .createdAt(user.getCreatedAt())
                .build();
    }
}