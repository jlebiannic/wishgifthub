package com.wishgifthub.controller;

import com.wishgifthub.entity.User;
import com.wishgifthub.service.UserGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
public class UserGroupController {
    @Autowired
    private UserGroupService userGroupService;

    @GetMapping("/{groupId}/users")
    public ResponseEntity<List<User>> getUsersByGroup(@PathVariable UUID groupId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userGroupService.getUsersByGroup(groupId, user.getId()));
    }

    @GetMapping("/user/groups")
    public ResponseEntity<List<UUID>> getUserGroups(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userGroupService.getGroupsByUser(user.getId()));
    }
}

