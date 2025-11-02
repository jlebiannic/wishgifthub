package com.wishgifthub.controller;

import com.wishgifthub.dto.GroupRequest;
import com.wishgifthub.dto.GroupResponse;
import com.wishgifthub.entity.User;
import com.wishgifthub.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
public class GroupController {
    @Autowired
    private GroupService groupService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@RequestBody GroupRequest request, @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(groupService.createGroup(request, admin.getId()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<GroupResponse>> getGroups(@AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(groupService.getGroupsByAdmin(admin.getId()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<GroupResponse> updateGroup(@PathVariable UUID id, @RequestBody GroupRequest request, @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(groupService.updateGroup(id, request, admin.getId()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable UUID id, @AuthenticationPrincipal User admin) {
        groupService.deleteGroup(id, admin.getId());
        return ResponseEntity.noContent().build();
    }
}
