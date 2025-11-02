package com.wishgifthub.config;

import com.wishgifthub.entity.User;
import com.wishgifthub.repository.UserRepository;
import com.wishgifthub.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
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

                    // Ajoute les authorities basées sur isAdmin
                    List<SimpleGrantedAuthority> authorities = user.isAdmin()
                        ? Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        : Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ignored) {}
        }
        filterChain.doFilter(request, response);
    }
}
