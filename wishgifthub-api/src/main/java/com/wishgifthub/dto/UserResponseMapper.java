package com.wishgifthub.dto;

import com.wishgifthub.entity.User;
import com.wishgifthub.openapi.model.UserResponse;

public final class UserResponseMapper {
    private UserResponseMapper() {
    }

    public static UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setIsAdmin(user.isAdmin());
        response.setParentId(user.getParentId());
        response.setAvatarId(user.getAvatarId());
        response.setPseudo(user.getPseudo());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
