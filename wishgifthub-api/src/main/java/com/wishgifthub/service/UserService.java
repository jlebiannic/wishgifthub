package com.wishgifthub.service;

import com.wishgifthub.dto.UserDto;
import com.wishgifthub.entity.User;
import com.wishgifthub.exception.ResourceNotFoundException;
import com.wishgifthub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de gestion des utilisateurs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Met à jour l'avatar et/ou le pseudo d'un utilisateur.
     *
     * @param email    l'email de l'utilisateur
     * @param avatarId l'identifiant du nouvel avatar (peut être null)
     * @param pseudo   le nouveau pseudo (peut être null)
     * @return les informations de l'utilisateur mis à jour
     */
    @Transactional
    public UserDto updateUserAvatar(String email, String avatarId, String pseudo) {
        log.info("Mise à jour du profil pour l'utilisateur: {} avec avatarId: {} et pseudo: {}", email, avatarId, pseudo);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'email: " + email));

        if (avatarId != null || pseudo != null) {
            if (avatarId != null) {
                user.setAvatarId(avatarId);
            }
            if (pseudo != null) {
                user.setPseudo(pseudo);
            }
            User updatedUser = userRepository.save(user);
            log.info("Profil mis à jour avec succès pour l'utilisateur: {}", email);
            return UserDto.fromEntity(updatedUser);
        }

        return UserDto.fromEntity(user);
    }
}

