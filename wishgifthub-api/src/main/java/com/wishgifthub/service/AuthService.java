package com.wishgifthub.service;

import com.wishgifthub.entity.User;
import com.wishgifthub.openapi.model.AuthRequest;
import com.wishgifthub.openapi.model.AuthResponse;
import com.wishgifthub.repository.UserGroupRepository;
import com.wishgifthub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserGroupRepository userGroupRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    @Transactional
    public AuthResponse registerAdmin(AuthRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email déjà utilisé");
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setAdmin(true);
        userRepository.save(user);
        String token = jwtService.generateToken(user);
        AuthResponse response = new AuthResponse();
        response.setUserId(user.getId());
        response.setIsAdmin(true);
        response.setAvatarId(user.getAvatarId());
        response.setToken(token);
        return response;
    }

    public AuthResponse loginAdmin(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        if (!user.isAdmin() && user.getPasswordHash() == null) {
            throw new IllegalArgumentException("Accès refusé");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mot de passe incorrect");
        }

        // Récupération de tous les groupes de l'utilisateur
        List<UUID> groupIds = userGroupRepository.findByUserId(user.getId())
                .stream()
                .map(userGroup -> userGroup.getGroup().getId())
                .collect(Collectors.toList());

        // Génération du token avec les groupIds
        String token = jwtService.generateToken(user, groupIds);

        AuthResponse response = new AuthResponse();
        response.setUserId(user.getId());
        response.setIsAdmin(true);
        response.setToken(token);
        return response;
    }
}

