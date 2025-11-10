package com.wishgifthub.controller;

import com.wishgifthub.entity.User;
import com.wishgifthub.openapi.api.GroupesApi;
import com.wishgifthub.openapi.model.GroupRequest;
import com.wishgifthub.openapi.model.GroupResponse;
import com.wishgifthub.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class GroupController implements GroupesApi {
    @Autowired
    private GroupService groupService;

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<GroupResponse> createGroup(GroupRequest groupRequest) {
        User admin = getCurrentUser();
        return ResponseEntity.ok(groupService.createGroup(groupRequest, admin.getId()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<List<GroupResponse>> getGroups() {
        User admin = getCurrentUser();
        return ResponseEntity.ok(groupService.getGroupsByAdmin(admin.getId()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<GroupResponse> updateGroup(UUID groupId, GroupRequest groupRequest) {
        User admin = getCurrentUser();
        return ResponseEntity.ok(groupService.updateGroup(groupId, groupRequest, admin.getId()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public ResponseEntity<Void> deleteGroup(UUID groupId) {
        User admin = getCurrentUser();
        groupService.deleteGroup(groupId, admin.getId());
        return ResponseEntity.noContent().build();
    }

    private User getCurrentUser() {
        return (User) org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication().getPrincipal();
    }
}

