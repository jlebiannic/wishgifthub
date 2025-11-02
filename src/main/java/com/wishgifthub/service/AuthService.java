package com.wishgifthub.service;

import com.wishgifthub.dto.AuthRequest;
import com.wishgifthub.dto.AuthResponse;
import com.wishgifthub.entity.User;
import com.wishgifthub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    @Transactional
    public AuthResponse registerAdmin(AuthRequest request) {
        if (userRepository.findByEmail(request.email).isPresent()) {
            throw new IllegalArgumentException("Email déjà utilisé");
        }
        User user = new User();
        user.setEmail(request.email);
        user.setPasswordHash(passwordEncoder.encode(request.password));
        user.setAdmin(true);
        userRepository.save(user);
        String token = jwtService.generateToken(user);
        AuthResponse response = new AuthResponse();
        response.userId = user.getId();
        response.isAdmin = true;
        response.token = token;
        return response;
    }

    public AuthResponse loginAdmin(AuthRequest request) {
        User user = userRepository.findByEmail(request.email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        if (!user.isAdmin() && user.getPasswordHash() == null) {
            throw new IllegalArgumentException("Accès refusé");
        }
        if (!passwordEncoder.matches(request.password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mot de passe incorrect");
        }
        String token = jwtService.generateToken(user);
        AuthResponse response = new AuthResponse();
        response.userId = user.getId();
        response.isAdmin = true;
        response.token = token;
        return response;
    }
}

