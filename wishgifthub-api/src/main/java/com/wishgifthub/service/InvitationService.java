package com.wishgifthub.service;

import com.wishgifthub.entity.Group;
import com.wishgifthub.entity.Invitation;
import com.wishgifthub.entity.User;
import com.wishgifthub.entity.UserGroup;
import com.wishgifthub.exception.AccessDeniedException;
import com.wishgifthub.exception.ResourceNotFoundException;
import com.wishgifthub.openapi.model.InvitationRequest;
import com.wishgifthub.openapi.model.InvitationResponse;
import com.wishgifthub.repository.GroupRepository;
import com.wishgifthub.repository.InvitationRepository;
import com.wishgifthub.repository.UserGroupRepository;
import com.wishgifthub.repository.UserRepository;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
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
    public InvitationResponse createInvitation(UUID groupId, InvitationRequest request, UUID adminId) {
        Group group = groupRepository.findByIdAndAdminId(groupId, adminId)
                .orElseThrow(() -> new AccessDeniedException("Groupe non trouvé ou vous n'êtes pas le propriétaire"));
        Invitation invitation = new Invitation();
        invitation.setGroup(group);
        invitation.setEmail(request.getEmail());
        invitation.setToken(UUID.randomUUID());
        invitation.setAccepted(false);
        invitationRepository.save(invitation);

        InvitationResponse resp = new InvitationResponse();
        resp.setId(invitation.getId());
        resp.setEmail(invitation.getEmail());
        resp.setGroupId(group.getId());
        resp.setToken(invitation.getToken());
        resp.setAccepted(false);
        resp.setCreatedAt(invitation.getCreatedAt());
        try {
            resp.setInvitationLink(JsonNullable.of(new URI(invitationBaseUrl + invitation.getToken())));
        } catch (Exception e) {
            // Log error
        }
        return resp;
    }

    @Transactional
    public InvitationResponse acceptInvitation(UUID token) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation avec le token spécifié non trouvée"));

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

        // Récupération de tous les groupes du user
        java.util.List<UUID> groupIds = userGroupRepository.findByUserId(user.getId())
                .stream()
                .map(ug -> ug.getGroup().getId())
                .collect(java.util.stream.Collectors.toList());

        // Génération JWT avec les groupes
        String jwt = jwtService.generateToken(user, groupIds);

        InvitationResponse resp = new InvitationResponse();
        resp.setId(invitation.getId());
        resp.setEmail(invitation.getEmail());
        resp.setGroupId(invitation.getGroup().getId());
        resp.setToken(invitation.getToken());
        resp.setAccepted(true);
        resp.setCreatedAt(invitation.getCreatedAt());
        resp.setJwtToken(JsonNullable.of(jwt));
        return resp;
    }
}

