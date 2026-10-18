package com.wishgifthub.service;

import com.wishgifthub.entity.Group;
import com.wishgifthub.entity.Invitation;
import com.wishgifthub.entity.User;
import com.wishgifthub.repository.InvitationRepository;
import com.wishgifthub.repository.UserGroupRepository;
import com.wishgifthub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {
    @Mock
    private InvitationRepository invitationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserGroupRepository userGroupRepository;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private InvitationService invitationService;

    private Invitation invitation;
    private User user;

    @BeforeEach
    void setUp() {
        Group group = new Group();
        group.setId(UUID.randomUUID());
        invitation = new Invitation();
        invitation.setToken(UUID.randomUUID());
        invitation.setEmail("guest@example.com");
        invitation.setGroup(group);
        invitation.setAccepted(true);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(invitation.getEmail());

        when(invitationRepository.findByToken(invitation.getToken())).thenReturn(Optional.of(invitation));
        when(userRepository.findByEmail(invitation.getEmail())).thenReturn(Optional.of(user));
        when(userGroupRepository.existsByUserIdAndGroupId(user.getId(), group.getId())).thenReturn(true);
        when(userGroupRepository.findByUserId(user.getId())).thenReturn(List.of());
        when(jwtService.generateToken(user, List.of())).thenReturn("jwt-token");
    }

    @Test
    void reconnectingReturnsCurrentProfileWhenInvitationHasNoProfile() {
        user.setAvatarId("avatar-17");
        user.setPseudo("Current name");

        var response = invitationService.acceptInvitation(invitation.getToken());

        assertEquals("avatar-17", response.getAvatarId());
        assertEquals("Current name", response.getPseudo());
        assertEquals("jwt-token", response.getJwtToken());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void reconnectingReturnsCurrentProfileInsteadOfOriginalInvitationProfile() {
        invitation.setAvatarId("avatar-1");
        invitation.setPseudo("Original name");
        user.setAvatarId("avatar-17");
        user.setPseudo("Current name");

        var response = invitationService.acceptInvitation(invitation.getToken());

        assertEquals("avatar-17", response.getAvatarId());
        assertEquals("Current name", response.getPseudo());
        assertEquals("avatar-17", user.getAvatarId());
        assertEquals("Current name", user.getPseudo());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void missingUserProfileIsStillInitializedFromInvitation() {
        invitation.setAccepted(false);
        invitation.setAvatarId("avatar-1");
        invitation.setPseudo("Invited name");

        var response = invitationService.acceptInvitation(invitation.getToken());

        assertEquals("avatar-1", response.getAvatarId());
        assertEquals("Invited name", response.getPseudo());
        assertEquals("avatar-1", user.getAvatarId());
        assertEquals("Invited name", user.getPseudo());
        verify(userRepository).save(user);
    }
}
