package com.wishgifthub.controller;

import com.wishgifthub.entity.User;
import com.wishgifthub.openapi.api.EnfantsApi;
import com.wishgifthub.openapi.model.ChildRequest;
import com.wishgifthub.openapi.model.UserResponse;
import com.wishgifthub.service.ChildService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ChildController implements EnfantsApi {
    private final ChildService childService;

    @Override
    @PreAuthorize("hasAuthority('GROUP_' + #groupId)")
    public ResponseEntity<List<UserResponse>> getChildren(UUID groupId) {
        return ResponseEntity.ok(childService.getChildren(groupId, currentUserId()));
    }

    @Override
    @PreAuthorize("hasAuthority('GROUP_' + #groupId)")
    public ResponseEntity<UserResponse> createChild(UUID groupId, ChildRequest childRequest) {
        return ResponseEntity.ok(childService.createChild(groupId, currentUserId(), childRequest));
    }

    @Override
    @PreAuthorize("hasAuthority('GROUP_' + #groupId)")
    public ResponseEntity<UserResponse> updateChild(UUID groupId, UUID childId, ChildRequest childRequest) {
        return ResponseEntity.ok(childService.updateChild(groupId, childId, currentUserId(), childRequest));
    }

    @Override
    @PreAuthorize("hasAuthority('GROUP_' + #groupId)")
    public ResponseEntity<Void> deleteChild(UUID groupId, UUID childId) {
        childService.deleteChild(groupId, childId, currentUserId());
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId() {
        return ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
    }
}
