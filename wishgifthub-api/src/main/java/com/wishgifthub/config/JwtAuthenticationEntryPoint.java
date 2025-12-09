package com.wishgifthub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Point d'entrée d'authentification personnalisé pour gérer les erreurs JWT.
 * Retourne une réponse JSON 401 en cas d'échec d'authentification.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        logger.warn("Échec d'authentification : {}", authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        errorDetails.put("message", "Authentification requise ou token invalide/expiré");
        errorDetails.put("code", "UNAUTHORIZED");
        errorDetails.put("timestamp", System.currentTimeMillis());

        // Vérifier si l'exception est liée à un token expiré
        String exceptionMessage = (String) request.getAttribute("jwt_error");
        if (exceptionMessage != null && exceptionMessage.contains("expired")) {
            errorDetails.put("message", "Votre session a expiré. Veuillez vous reconnecter.");
            errorDetails.put("code", "TOKEN_EXPIRED");
        }

        response.getWriter().write(objectMapper.writeValueAsString(errorDetails));
    }
}

