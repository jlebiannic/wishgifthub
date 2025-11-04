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
            u.setCreatedAt(java.time.OffsetDateTime.now());
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
        // Ajout du JWT dans le champ invitationLink pour retour API (adaptation)
        resp.invitationLink = jwt;
        return resp;
    }
}

