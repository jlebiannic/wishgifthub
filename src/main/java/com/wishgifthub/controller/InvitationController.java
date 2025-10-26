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
package com.wishgifthub.service;

import com.wishgifthub.dto.InvitationRequest;
import com.wishgifthub.dto.InvitationResponse;
import com.wishgifthub.entity.Group;
import com.wishgifthub.entity.Invitation;
import com.wishgifthub.entity.User;
import com.wishgifthub.entity.UserGroup;
import com.wishgifthub.repository.GroupRepository;
import com.wishgifthub.repository.InvitationRepository;
import com.wishgifthub.repository.UserGroupRepository;
import com.wishgifthub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvitationService {
    @Autowired
    private InvitationRepository invitationRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserGroupRepository userGroupRepository;
    @Autowired
    private JwtService jwtService;
    @Value("${wishgifthub.invitation.base-url:https://app.com/join/}")
    private String invitationBaseUrl;

    @Transactional
    public InvitationResponse createInvitation(UUID groupId, InvitationRequest request) {
        Group group = groupRepository.findById(groupId).orElseThrow();
        Invitation invitation = new Invitation();
        invitation.setGroup(group);
        invitation.setEmail(request.email);
        invitation.setToken(UUID.randomUUID());
        invitation.setAccepted(false);
        invitationRepository.save(invitation);
        InvitationResponse resp = new InvitationResponse();
        resp.id = invitation.getId();
        resp.email = invitation.getEmail();
        resp.groupId = group.getId();
        resp.token = invitation.getToken();
        resp.accepted = false;
        resp.createdAt = invitation.getCreatedAt();
        resp.invitationLink = invitationBaseUrl + invitation.getToken();
        return resp;
    }

    @Transactional
    public InvitationResponse acceptInvitation(UUID token) {
        Invitation invitation = invitationRepository.findByToken(token).orElseThrow();
        if (invitation.isAccepted()) throw new IllegalStateException("Invitation déjà acceptée");
        // Création user invité si inexistant
        User user = userRepository.findByEmail(invitation.getEmail()).orElseGet(() -> {
            User u = new User();
            u.setEmail(invitation.getEmail());
            u.setAdmin(false);
            return userRepository.save(u);
        });
        // Ajout dans user_groups
        if (!userGroupRepository.existsByUserIdAndGroupId(user.getId(), invitation.getGroup().getId())) {
            UserGroup ug = new UserGroup();
            ug.setUser(user);
            ug.setGroup(invitation.getGroup());
            userGroupRepository.save(ug);
        }
        invitation.setUser(user);
        invitation.setAccepted(true);
        invitationRepository.save(invitation);
        // Génération JWT
        String jwt = jwtService.generateToken(user);
        InvitationResponse resp = new InvitationResponse();
        resp.id = invitation.getId();
        resp.email = invitation.getEmail();
        resp.groupId = invitation.getGroup().getId();
        resp.token = invitation.getToken();
        resp.accepted = true;
        resp.createdAt = invitation.getCreatedAt();
        resp.invitationLink = null;
        // Ajout du JWT dans le champ invitationLink pour retour API (adaptation)
        resp.invitationLink = jwt;
        return resp;
    }
}

