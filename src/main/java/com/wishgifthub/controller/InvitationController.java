package com.wishgifthub.controller;

import com.wishgifthub.dto.InvitationRequest;
import com.wishgifthub.dto.InvitationResponse;
import com.wishgifthub.entity.User;
import com.wishgifthub.service.InvitationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class InvitationController {
    @Autowired
    private InvitationService invitationService;

    @PostMapping("/groups/{groupId}/invite")
    public ResponseEntity<InvitationResponse> invite(@PathVariable UUID groupId, @RequestBody InvitationRequest request, @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(invitationService.createInvitation(groupId, request));
    }

    @GetMapping("/invite/{token}")
    public ResponseEntity<InvitationResponse> accept(@PathVariable UUID token) {
        return ResponseEntity.ok(invitationService.acceptInvitation(token));
    }
}
