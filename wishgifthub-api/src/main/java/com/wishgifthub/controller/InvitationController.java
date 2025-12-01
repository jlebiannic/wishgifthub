package com.wishgifthub.controller;

import com.wishgifthub.entity.User;
import com.wishgifthub.openapi.api.InvitationsApi;
import com.wishgifthub.openapi.model.InvitationRequest;
import com.wishgifthub.openapi.model.InvitationResponse;
import com.wishgifthub.service.InvitationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class InvitationController implements InvitationsApi {
    @Autowired
    private InvitationService invitationService;

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<InvitationResponse> invite(UUID groupId, InvitationRequest invitationRequest) {
        User admin = getCurrentUser();
        return ResponseEntity.ok(invitationService.createInvitation(groupId, invitationRequest, admin.getId()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<List<InvitationResponse>> getInvitations(UUID groupId) {
        User admin = getCurrentUser();
        return ResponseEntity.ok(invitationService.getInvitationsByGroup(groupId, admin.getId()));
    }

    @Override
    public ResponseEntity<InvitationResponse> accept(UUID token) {
        return ResponseEntity.ok(invitationService.acceptInvitation(token));
    }

    private User getCurrentUser() {
        return (User) org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication().getPrincipal();
    }
}

