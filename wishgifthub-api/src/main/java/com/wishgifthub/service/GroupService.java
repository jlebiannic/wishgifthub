package com.wishgifthub.service;

import com.wishgifthub.entity.Group;
import com.wishgifthub.entity.User;
import com.wishgifthub.entity.UserGroup;
import com.wishgifthub.exception.AccessDeniedException;
import com.wishgifthub.exception.ResourceNotFoundException;
import com.wishgifthub.openapi.model.GroupRequest;
import com.wishgifthub.openapi.model.GroupResponse;
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
    @Autowired
    private JwtService jwtService;

    @Transactional
    public GroupResponse createGroup(GroupRequest request, UUID adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", adminId));

        // Vérification que l'utilisateur est bien un administrateur
        if (!admin.isAdmin()) {
            throw new AccessDeniedException("Seuls les administrateurs peuvent créer des groupes");
        }

        Group group = new Group();
        group.setName(request.getName());
        group.setType(request.getType().getValue());
        group.setAdmin(admin);
        groupRepository.save(group);

        // Ajout admin dans user_groups
        UserGroup ug = new UserGroup();
        ug.setUser(admin);
        ug.setGroup(group);
        userGroupRepository.save(ug);

        // Récupération de tous les groupes de l'admin (incluant le nouveau)
        List<UUID> groupIds = userGroupRepository.findByUserId(adminId)
                .stream()
                .map(userGroup -> userGroup.getGroup().getId())
                .collect(Collectors.toList());

        // Génération du nouveau JWT avec tous les groupes
        String jwt = jwtService.generateToken(admin, groupIds);

        GroupResponse resp = new GroupResponse();
        resp.setId(group.getId());
        resp.setName(group.getName());
        resp.setType(group.getType());
        resp.setAdminId(admin.getId());
        resp.setCreatedAt(group.getCreatedAt());
        resp.setJwtToken(jwt);
        return resp;
    }

    public List<GroupResponse> getGroupsByAdmin(UUID adminId) {
        return groupRepository.findByAdminId(adminId).stream().map(g -> {
            GroupResponse resp = new GroupResponse();
            resp.setId(g.getId());
            resp.setName(g.getName());
            resp.setType(g.getType());
            resp.setAdminId(g.getAdmin().getId());
            resp.setCreatedAt(g.getCreatedAt());
            return resp;
        }).collect(Collectors.toList());
    }

    @Transactional
    public GroupResponse updateGroup(UUID groupId, GroupRequest request, UUID adminId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Groupe", groupId));

        if (!group.getAdmin().getId().equals(adminId)) {
            throw new AccessDeniedException("Seul le propriétaire du groupe peut le modifier");
        }

        group.setName(request.getName());
        group.setType(request.getType().getValue());
        groupRepository.save(group);

        GroupResponse resp = new GroupResponse();
        resp.setId(group.getId());
        resp.setName(group.getName());
        resp.setType(group.getType());
        resp.setAdminId(group.getAdmin().getId());
        resp.setCreatedAt(group.getCreatedAt());
        return resp;
    }

    @Transactional
    public void deleteGroup(UUID groupId, UUID adminId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Groupe", groupId));

        if (!group.getAdmin().getId().equals(adminId)) {
            throw new AccessDeniedException("Seul le propriétaire du groupe peut le supprimer");
        }

        groupRepository.delete(group);
    }
}

