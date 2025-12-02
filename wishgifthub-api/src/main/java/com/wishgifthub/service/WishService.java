package com.wishgifthub.service;

import com.wishgifthub.entity.Group;
import com.wishgifthub.entity.User;
import com.wishgifthub.entity.Wish;
import com.wishgifthub.exception.AccessDeniedException;
import com.wishgifthub.exception.BusinessRuleException;
import com.wishgifthub.exception.ResourceNotFoundException;
import com.wishgifthub.openapi.model.WishRequest;
import com.wishgifthub.openapi.model.WishResponse;
import com.wishgifthub.repository.GroupRepository;
import com.wishgifthub.repository.UserGroupRepository;
import com.wishgifthub.repository.UserRepository;
import com.wishgifthub.repository.WishRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
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

        // Validation des champs
        if (request.getGiftName() == null || request.getGiftName().trim().isEmpty()) {
            throw new BusinessRuleException("Le nom du cadeau est requis");
        }
        if (request.getGiftName().length() > 255) {
            throw new BusinessRuleException("Le nom du cadeau ne peut pas dépasser 255 caractères");
        }
        if (request.getDescription() != null && request.getDescription().length() > 10000) {
            throw new BusinessRuleException("La description ne peut pas dépasser 10 000 caractères");
        }
        if (request.getPrice() != null && request.getPrice().length() > 100) {
            throw new BusinessRuleException("Le prix ne peut pas dépasser 100 caractères");
        }

        // Validation de la longueur des URLs (la validation du format est faite par @ValidUrl)
        if (request.getUrl() != null && request.getUrl().toString().length() > 2048) {
            throw new BusinessRuleException("L'URL du produit ne peut pas dépasser 2048 caractères");
        }
        if (request.getImageUrl() != null && request.getImageUrl().toString().length() > 2048) {
            throw new BusinessRuleException("L'URL de l'image ne peut pas dépasser 2048 caractères");
        }

        Wish wish = new Wish();
        wish.setUser(user);
        wish.setGroup(group);
        wish.setGiftName(request.getGiftName());
        if (request.getDescription() != null) {
            wish.setDescription(request.getDescription());
        }
        if (request.getUrl() != null) {
            wish.setUrl(request.getUrl().toString());
        }
        if (request.getImageUrl() != null) {
            wish.setImageUrl(request.getImageUrl().toString());
        }
        if (request.getPrice() != null) {
            wish.setPrice(request.getPrice());
        }
        wish = wishRepository.save(wish);

        return toResponse(wish);
    }

    public List<WishResponse> getWishesByGroup(UUID groupId, UUID userId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Groupe", groupId));

        return wishRepository.findByGroupId(groupId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public WishResponse reserveWish(UUID groupId, UUID wishId, UUID userId) {
        Wish wish = wishRepository.findById(wishId)
                .orElseThrow(() -> new ResourceNotFoundException("Souhait", wishId));

        if (!wish.getGroup().getId().equals(groupId)) {
            throw new BusinessRuleException("Le souhait n'appartient pas à ce groupe");
        }

        if (wish.getUser().getId().equals(userId)) {
            throw new BusinessRuleException("Vous ne pouvez pas réserver votre propre souhait");
        }

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

        if (!wish.getGroup().getId().equals(groupId)) {
            throw new BusinessRuleException("Le souhait n'appartient pas à ce groupe");
        }

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

        if (!wish.getGroup().getId().equals(groupId)) {
            throw new BusinessRuleException("Le souhait n'appartient pas à ce groupe");
        }

        if (!wish.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Vous ne pouvez supprimer que vos propres souhaits");
        }

        wishRepository.delete(wish);
    }

    public List<WishResponse> getWishesByUserInGroup(UUID groupId, UUID targetUserId, UUID requestingUserId) {
        if (!userGroupRepository.existsByUserIdAndGroupId(targetUserId, groupId)) {
            throw new BusinessRuleException("L'utilisateur cible n'appartient pas à ce groupe");
        }

        return wishRepository.findByGroupIdAndUserId(groupId, targetUserId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private WishResponse toResponse(Wish wish) {
        WishResponse resp = new WishResponse();
        resp.setId(wish.getId());
        resp.setUserId(wish.getUser().getId());
        resp.setGroupId(wish.getGroup().getId());
        resp.setGiftName(wish.getGiftName());
        if (wish.getDescription() != null) {
            resp.setDescription(wish.getDescription());
        }
        if (wish.getUrl() != null) {
            try {
                resp.setUrl(new URI(wish.getUrl()));
            } catch (Exception e) {
                log.warn("URL invalide pour le souhait {}: {}", wish.getId(), wish.getUrl(), e);
            }
        }
        if (wish.getImageUrl() != null) {
            try {
                resp.setImageUrl(new URI(wish.getImageUrl()));
            } catch (Exception e) {
                log.warn("URL d'image invalide pour le souhait {}: {}", wish.getId(), wish.getImageUrl(), e);
            }
        }
        if (wish.getPrice() != null) {
            resp.setPrice(wish.getPrice());
        }
        if (wish.getReservedBy() != null) {
            resp.setReservedBy(wish.getReservedBy().getId());
        }
        resp.setCreatedAt(wish.getCreatedAt());
        return resp;
    }
}

