package com.wishgifthub.service;

import com.wishgifthub.entity.UserGroup;
import com.wishgifthub.entity.User;
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

    public List<User> getUsersByGroup(UUID groupId, UUID userId) {
        // Vérifie que l'utilisateur appartient au groupe
        if (!userGroupRepository.existsByUserIdAndGroupId(userId, groupId)) {
            throw new SecurityException("L'utilisateur n'appartient pas à ce groupe");
        }
        return userGroupRepository.findByGroupId(groupId).stream().map(UserGroup::getUser).collect(Collectors.toList());
    }

    public List<UUID> getGroupsByUser(UUID userId) {
        return userGroupRepository.findByUserId(userId).stream().map(ug -> ug.getGroup().getId()).collect(Collectors.toList());
    }
}

