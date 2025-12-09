package com.wishgifthub.config;

import com.wishgifthub.entity.User;
import com.wishgifthub.repository.UserRepository;
import com.wishgifthub.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        if (token != null) {
            try {
                UUID userId = jwtService.getUserIdFromToken(token);
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();

                    // Ajoute les authorities basées sur isAdmin et les groupes
                    List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();

                    // Ajout du rôle ADMIN ou USER
                    if (user.isAdmin()) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    } else {
                        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                    }

                    // Ajout des groupes comme authorities
                    List<UUID> groupIds = jwtService.getGroupIdsFromToken(token);
                    for (UUID groupId : groupIds) {
                        authorities.add(new SimpleGrantedAuthority("GROUP_" + groupId.toString()));
                    }

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (ExpiredJwtException e) {
                // Token expiré - stocker l'erreur dans un attribut de requête
                // L'AuthenticationEntryPoint se chargera de retourner la réponse 401
                request.setAttribute("jwt_error", "Token expired: " + e.getMessage());
            } catch (Exception e) {
                // Autres exceptions JWT (signature invalide, token malformé, etc.)
                request.setAttribute("jwt_error", "Invalid token: " + e.getMessage());
                log.error("Erreur JWT pour la requête {}: {}", request.getRequestURI(), e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}
