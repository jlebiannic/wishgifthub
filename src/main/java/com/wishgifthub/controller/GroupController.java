package com.wishgifthub.controller;

import com.wishgifthub.dto.GroupRequest;
import com.wishgifthub.dto.GroupResponse;
import com.wishgifthub.entity.User;
import com.wishgifthub.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
public class GroupController {
    @Autowired
    private GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@RequestBody GroupRequest request, @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(groupService.createGroup(request, admin.getId()));
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getGroups(@AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(groupService.getGroupsByAdmin(admin.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupResponse> updateGroup(@PathVariable UUID id, @RequestBody GroupRequest request, @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(groupService.updateGroup(id, request, admin.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable UUID id, @AuthenticationPrincipal User admin) {
        groupService.deleteGroup(id, admin.getId());
        return ResponseEntity.noContent().build();
    }
}
package com.wishgifthub.service;

import com.wishgifthub.dto.GroupRequest;
import com.wishgifthub.dto.GroupResponse;
import com.wishgifthub.entity.Group;
import com.wishgifthub.entity.User;
import com.wishgifthub.entity.UserGroup;
import com.wishgifthub.repository.GroupRepository;
import com.wishgifthub.repository.UserGroupRepository;
import com.wishgifthub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GroupService {
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserGroupRepository userGroupRepository;

    @Transactional
    public GroupResponse createGroup(GroupRequest request, UUID adminId) {
        User admin = userRepository.findById(adminId).orElseThrow();
        Group group = new Group();
        group.setName(request.name);
        group.setType(request.type);
        group.setAdmin(admin);
        groupRepository.save(group);
        // Ajout admin dans user_groups
        UserGroup ug = new UserGroup();
        ug.setUser(admin);
        ug.setGroup(group);
        userGroupRepository.save(ug);
        GroupResponse resp = new GroupResponse();
        resp.id = group.getId();
        resp.name = group.getName();
        resp.type = group.getType();
        resp.adminId = admin.getId();
        resp.createdAt = group.getCreatedAt();
        return resp;
    }

    public List<GroupResponse> getGroupsByAdmin(UUID adminId) {
        return groupRepository.findByAdminId(adminId).stream().map(g -> {
            GroupResponse resp = new GroupResponse();
            resp.id = g.getId();
            resp.name = g.getName();
            resp.type = g.getType();
            resp.adminId = g.getAdmin().getId();
            resp.createdAt = g.getCreatedAt();
            return resp;
        }).collect(Collectors.toList());
    }

    @Transactional
    public GroupResponse updateGroup(UUID groupId, GroupRequest request, UUID adminId) {
        Group group = groupRepository.findById(groupId).orElseThrow();
        if (!group.getAdmin().getId().equals(adminId)) throw new SecurityException();
        group.setName(request.name);
        group.setType(request.type);
        groupRepository.save(group);
        GroupResponse resp = new GroupResponse();
        resp.id = group.getId();
        resp.name = group.getName();
        resp.type = group.getType();
        resp.adminId = group.getAdmin().getId();
        resp.createdAt = group.getCreatedAt();
        return resp;
    }

    @Transactional
    public void deleteGroup(UUID groupId, UUID adminId) {
        Group group = groupRepository.findById(groupId).orElseThrow();
        if (!group.getAdmin().getId().equals(adminId)) throw new SecurityException();
        groupRepository.delete(group);
    }
}

