package com.wishgifthub.controller;

import com.wishgifthub.openapi.api.AuthentificationApi;
import com.wishgifthub.openapi.model.AuthRequest;
import com.wishgifthub.openapi.model.AuthResponse;
import com.wishgifthub.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController implements AuthentificationApi {
    @Autowired
    private AuthService authService;

    @Override
    public ResponseEntity<AuthResponse> register(AuthRequest authRequest) {
        return ResponseEntity.ok(authService.registerAdmin(authRequest));
    }

    @Override
    public ResponseEntity<AuthResponse> login(AuthRequest authRequest) {
        return ResponseEntity.ok(authService.loginAdmin(authRequest));
    }
}

