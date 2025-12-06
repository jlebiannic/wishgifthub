package com.wishgifthub.dto;

import com.wishgifthub.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO représentant un utilisateur (pour usage interne).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private String id;
    private String email;
    private Boolean isAdmin;
    private String avatarId;
    private String pseudo;
    private LocalDateTime createdAt;

    /**
     * Convertit une entité User en UserDto.
     *
     * @param user l'entité utilisateur
     * @return le DTO utilisateur
     */
    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .isAdmin(user.isAdmin())
                .avatarId(user.getAvatarId())
                .pseudo(user.getPseudo())
                .createdAt(user.getCreatedAt().toLocalDateTime())
                .build();
    }
}

