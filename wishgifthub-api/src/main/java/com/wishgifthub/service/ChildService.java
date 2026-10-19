package com.wishgifthub.service;

import com.wishgifthub.dto.UserResponseMapper;
import com.wishgifthub.entity.User;
import com.wishgifthub.entity.UserGroup;
import com.wishgifthub.exception.AccessDeniedException;
import com.wishgifthub.exception.BusinessRuleException;
import com.wishgifthub.exception.ResourceNotFoundException;
import com.wishgifthub.openapi.model.ChildRequest;
import com.wishgifthub.openapi.model.UserResponse;
import com.wishgifthub.repository.GroupRepository;
import com.wishgifthub.repository.UserGroupRepository;
import com.wishgifthub.repository.UserRepository;
import com.wishgifthub.repository.WishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ChildService {
    private final UserRepository users;
    private final UserGroupRepository memberships;
    private final GroupRepository groups;
    private final WishRepository wishes;

    @Transactional(readOnly = true)
    public List<UserResponse> getChildren(UUID groupId, UUID parentId) {
        requireParentInGroup(groupId, parentId);
        return memberships.findByGroupIdAndUserParentIdOrderByUserCreatedAtAsc(groupId, parentId).stream()
                .map(UserGroup::getUser)
                .map(UserResponseMapper::toResponse)
                .toList();
    }

    public UserResponse createChild(UUID groupId, UUID parentId, ChildRequest request) {
        requireParentInGroup(groupId, parentId);
        User child = new User();
        child.setParentId(parentId);
        updateProfile(child, request);
        child = users.save(child);
        UserGroup membership = new UserGroup();
        membership.setUser(child);
        membership.setGroup(groups.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Groupe", groupId)));
        memberships.save(membership);
        return UserResponseMapper.toResponse(child);
    }

    public UserResponse updateChild(UUID groupId, UUID childId, UUID parentId, ChildRequest request) {
        User child = requireChild(groupId, childId, parentId);
        updateProfile(child, request);
        return UserResponseMapper.toResponse(users.save(child));
    }

    public void deleteChild(UUID groupId, UUID childId, UUID parentId) {
        User child = requireChild(groupId, childId, parentId);
        wishes.deleteAll(wishes.findByUserId(childId));
        memberships.deleteAll(memberships.findByUserId(childId));
        users.delete(child);
    }

    private User requireChild(UUID groupId, UUID childId, UUID parentId) {
        requireParentInGroup(groupId, parentId);
        User child = users.findById(childId)
                .orElseThrow(() -> new ResourceNotFoundException("Enfant", childId));
        if (!parentId.equals(child.getParentId())
                || !memberships.existsByUserIdAndGroupId(childId, groupId)) {
            throw new AccessDeniedException("Vous ne pouvez gérer que vos enfants membres de ce groupe");
        }
        return child;
    }

    private void requireParentInGroup(UUID groupId, UUID parentId) {
        User parent = users.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", parentId));
        if (parent.getParentId() != null || !memberships.existsByUserIdAndGroupId(parentId, groupId)) {
            throw new AccessDeniedException("Vous devez être membre du groupe pour gérer vos enfants");
        }
    }

    private void updateProfile(User child, ChildRequest request) {
        if (request.getPseudo() == null || request.getPseudo().isBlank() || request.getPseudo().length() > 100) {
            throw new BusinessRuleException("Le nom de l'enfant doit contenir entre 1 et 100 caractères");
        }
        child.setPseudo(request.getPseudo().trim());
        child.setAvatarId(request.getAvatarId());
    }
}
