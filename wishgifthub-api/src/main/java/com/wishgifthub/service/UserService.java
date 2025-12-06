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
     * Met à jour l'avatar d'un utilisateur.
     *
     * @param email    l'email de l'utilisateur
     * @param avatarId l'identifiant du nouvel avatar (peut être null)
     * @return les informations de l'utilisateur mis à jour
     */
    @Transactional
    public UserDto updateUserAvatar(String email, String avatarId) {
        log.info("Mise à jour de l'avatar pour l'utilisateur: {} avec avatarId: {}", email, avatarId);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'email: " + email));

        user.setAvatarId(avatarId);
        User updatedUser = userRepository.save(user);

        log.info("Avatar mis à jour avec succès pour l'utilisateur: {}", email);

        return UserDto.fromEntity(updatedUser);
    }
}

