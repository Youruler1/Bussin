package com.busbooking.auth.dto;

import com.busbooking.auth.Role;
import com.busbooking.auth.User;

public record UserProfileResponse(
    Long id,
    String email,
    String fullName,
    String phone,
    Role role,
    boolean isActive
) {
    public static UserProfileResponse fromEntity(User user) {
        return new UserProfileResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getPhone(),
            user.getRole(),
            user.isActive()
        );
    }
}
