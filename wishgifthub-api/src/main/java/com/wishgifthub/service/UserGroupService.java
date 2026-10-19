package com.wishgifthub.service;

import com.wishgifthub.entity.User;
import com.wishgifthub.entity.UserGroup;
import com.wishgifthub.exception.ResourceNotFoundException;
import com.wishgifthub.openapi.model.GroupResponse;
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
                .map(ug -> GroupMapper.toResponse(ug.getGroup()))
                .collect(Collectors.toList());
    }
}
