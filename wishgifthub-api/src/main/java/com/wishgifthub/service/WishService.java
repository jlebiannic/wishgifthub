package com.wishgifthub.service;

import com.wishgifthub.dto.WishRequest;
import com.wishgifthub.dto.WishResponse;
import com.wishgifthub.entity.Group;
import com.wishgifthub.entity.User;
import com.wishgifthub.entity.Wish;
import com.wishgifthub.exception.AccessDeniedException;
import com.wishgifthub.exception.BusinessRuleException;
import com.wishgifthub.exception.ResourceNotFoundException;
import com.wishgifthub.repository.GroupRepository;
import com.wishgifthub.repository.UserGroupRepository;
import com.wishgifthub.repository.UserRepository;
import com.wishgifthub.repository.WishRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WishService {
    @Autowired
    private WishRepository wishRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private UserRepository userRepository;

    public WishResponse createWish(UUID groupId, WishRequest request, UUID userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Groupe", groupId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));

        Wish wish = new Wish();
        wish.setUser(user);
        wish.setGroup(group);
        wish.setGiftName(request.giftName);
        wish.setDescription(request.description);
        wish.setUrl(request.url);
        wish = wishRepository.save(wish);

        return toResponse(wish);
    }

    public List<WishResponse> getWishesByGroup(UUID groupId, UUID userId) {
        // Vérifier que le groupe existe
        groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Groupe", groupId));

        return wishRepository.findByGroupId(groupId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public WishResponse reserveWish(UUID groupId, UUID wishId, UUID userId) {
        Wish wish = wishRepository.findById(wishId)
                .orElseThrow(() -> new ResourceNotFoundException("Souhait", wishId));

        // Vérifier que le souhait appartient au bon groupe
        if (!wish.getGroup().getId().equals(groupId)) {
            throw new BusinessRuleException("Le souhait n'appartient pas à ce groupe");
        }

        // Vérifier que l'utilisateur n'essaie pas de réserver son propre souhait
        if (wish.getUser().getId().equals(userId)) {
            throw new BusinessRuleException("Vous ne pouvez pas réserver votre propre souhait");
        }

        // Vérifier que le souhait n'est pas déjà réservé
        if (wish.getReservedBy() != null) {
            throw new BusinessRuleException("Ce souhait est déjà réservé");
        }

        User reserver = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));
        wish.setReservedBy(reserver);
        wish = wishRepository.save(wish);

        return toResponse(wish);
    }

    public WishResponse unreserveWish(UUID groupId, UUID wishId, UUID userId) {
        Wish wish = wishRepository.findById(wishId)
                .orElseThrow(() -> new ResourceNotFoundException("Souhait", wishId));

        // Vérifier que le souhait appartient au bon groupe
        if (!wish.getGroup().getId().equals(groupId)) {
            throw new BusinessRuleException("Le souhait n'appartient pas à ce groupe");
        }

        // Vérifier que c'est bien l'utilisateur qui a réservé
        if (wish.getReservedBy() == null || !wish.getReservedBy().getId().equals(userId)) {
            throw new BusinessRuleException("Vous n'avez pas réservé ce souhait");
        }

        wish.setReservedBy(null);
        wish = wishRepository.save(wish);

        return toResponse(wish);
    }

    public void deleteWish(UUID groupId, UUID wishId, UUID userId) {
        Wish wish = wishRepository.findById(wishId)
                .orElseThrow(() -> new ResourceNotFoundException("Souhait", wishId));

        // Vérifier que le souhait appartient au bon groupe
        if (!wish.getGroup().getId().equals(groupId)) {
            throw new BusinessRuleException("Le souhait n'appartient pas à ce groupe");
        }

        // Vérifier que c'est bien l'utilisateur propriétaire du souhait
        if (!wish.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Vous ne pouvez supprimer que vos propres souhaits");
        }

        wishRepository.delete(wish);
    }

    public List<WishResponse> getWishesByUserInGroup(UUID groupId, UUID targetUserId, UUID requestingUserId) {
        // Vérifier que l'utilisateur cible appartient au groupe
        if (!userGroupRepository.existsByUserIdAndGroupId(targetUserId, groupId)) {
            throw new BusinessRuleException("L'utilisateur cible n'appartient pas à ce groupe");
        }

        return wishRepository.findByGroupIdAndUserId(groupId, targetUserId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private WishResponse toResponse(Wish wish) {
        WishResponse resp = new WishResponse();
        resp.id = wish.getId();
        resp.userId = wish.getUser().getId();
        resp.groupId = wish.getGroup().getId();
        resp.giftName = wish.getGiftName();
        resp.description = wish.getDescription();
        resp.url = wish.getUrl();
        resp.reservedBy = wish.getReservedBy() != null ? wish.getReservedBy().getId() : null;
        resp.createdAt = wish.getCreatedAt();
        return resp;
    }
}

