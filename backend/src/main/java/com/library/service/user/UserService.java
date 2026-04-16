package com.library.service.user;

import com.library.entity.Role;
import com.library.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    User getById(UUID id);

    User getByEmail(String email);

    Page<User> getAll(Pageable pageable);

    Page<User> getAllByRole(Role role, Pageable pageable);

    User updateProfile(UUID id, String firstName, String lastName, String phoneNumber, String profilePictureUrl);

    void changePassword(UUID id, String currentPassword, String newPassword);

    void deactivate(UUID id);

    void activate(UUID id);

    void changeRole(UUID id, Role newRole);

    long countByRole(Role role);
}