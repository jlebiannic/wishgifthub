package com.wishgifthub.controller;

import com.wishgifthub.entity.User;
import com.wishgifthub.openapi.api.UtilisateursApi;
import com.wishgifthub.openapi.model.GroupResponse;
import com.wishgifthub.openapi.model.UserResponse;
import com.wishgifthub.service.UserGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class UserGroupController implements UtilisateursApi {
    @Autowired
    private UserGroupService userGroupService;

    @PreAuthorize("hasAuthority('GROUP_' + #groupId)")
    @Override
    public ResponseEntity<List<UserResponse>> getUsersByGroup(UUID groupId) {
        User user = getCurrentUser();
        List<User> users = userGroupService.getUsersByGroup(groupId, user.getId());

        List<UserResponse> apiUsers = users.stream()
            .map(this::convertToUserResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(apiUsers);
    }

    @Override
    public ResponseEntity<List<GroupResponse>> getUserGroups() {
        User user = getCurrentUser();
        return ResponseEntity.ok(userGroupService.getGroupsByUser(user.getId()));
    }

    private User getCurrentUser() {
        return (User) org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication().getPrincipal();
    }

    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setIsAdmin(user.isAdmin());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}

