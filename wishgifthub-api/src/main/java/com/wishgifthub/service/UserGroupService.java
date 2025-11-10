package com.wishgifthub.service;

import com.wishgifthub.dto.GroupResponse;
import com.wishgifthub.entity.Group;
import com.wishgifthub.entity.User;
import com.wishgifthub.entity.UserGroup;
import com.wishgifthub.exception.ResourceNotFoundException;
import com.wishgifthub.repository.GroupRepository;
import com.wishgifthub.repository.UserGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserGroupService {
    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private GroupRepository groupRepository;

    public List<User> getUsersByGroup(UUID groupId, UUID userId) {
        // Vérifier que le groupe existe
        groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Groupe", groupId));

        return userGroupRepository.findByGroupId(groupId).stream()
                .map(UserGroup::getUser)
                .collect(Collectors.toList());
    }

    public List<GroupResponse> getGroupsByUser(UUID userId) {
        return userGroupRepository.findByUserId(userId).stream()
                .map(ug -> {
                    Group group = ug.getGroup();
                    GroupResponse response = new GroupResponse();
                    response.id = group.getId();
                    response.name = group.getName();
                    response.type = group.getType();
                    return response;
                })
                .collect(Collectors.toList());
    }
}

