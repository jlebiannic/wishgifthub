package com.wishgifthub.service;

import com.wishgifthub.entity.Group;
import com.wishgifthub.entity.User;
import com.wishgifthub.entity.Wish;
import com.wishgifthub.exception.AccessDeniedException;
import com.wishgifthub.exception.BusinessRuleException;
import com.wishgifthub.exception.ResourceNotFoundException;
import com.wishgifthub.openapi.model.WishRequest;
import com.wishgifthub.openapi.model.WishResponse;
import com.wishgifthub.openapi.model.UserResponse;
import com.wishgifthub.repository.GroupRepository;
import com.wishgifthub.repository.UserGroupRepository;
import com.wishgifthub.repository.UserRepository;
import com.wishgifthub.repository.WishRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
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

        // La validation des URLs (format et longueur) est faite automatiquement par @ValidUrl

        Wish wish = new Wish();
        wish.setUser(user);
        wish.setGroup(group);
        wish.setGiftName(request.getGiftName());
        configureReservation(wish, request);
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

        return toResponse(wish, userId);
    }

    public List<WishResponse> getWishesByGroup(UUID groupId, UUID userId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Groupe", groupId));

        return wishRepository.findByGroupId(groupId).stream()
                .map(wish -> toResponse(wish, userId))
                .collect(Collectors.toList());
    }

    public WishResponse reserveWish(UUID groupId, UUID wishId, UUID userId) {
        Wish wish = wishRepository.findByIdForUpdate(wishId)
                .orElseThrow(() -> new ResourceNotFoundException("Souhait", wishId));

        if (!wish.getGroup().getId().equals(groupId)) {
            throw new BusinessRuleException("Le souhait n'appartient pas à ce groupe");
        }

        if (wish.getUser().getId().equals(userId)) {
            throw new BusinessRuleException("Vous ne pouvez pas réserver votre propre souhait");
        }

        if (wish.isCollective() && wish.getParticipants().contains(userId)) {
            throw new BusinessRuleException("Vous participez déjà à ce cadeau commun");
        }
        if (wish.isCollective() && wish.getParticipants().size() >= wish.getMaxParticipants()) {
            throw new BusinessRuleException("Ce cadeau commun est complet");
        }
        if (!wish.isCollective() && wish.getReservedBy() != null) {
            throw new BusinessRuleException("Ce souhait est déjà réservé");
        }

        User reserver = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));
        if (wish.isCollective()) {
            wish.getParticipants().add(reserver.getId());
        } else {
            wish.setReservedBy(reserver);
        }
        wish = wishRepository.save(wish);

        return toResponse(wish, userId);
    }

    public WishResponse unreserveWish(UUID groupId, UUID wishId, UUID userId) {
        Wish wish = wishRepository.findByIdForUpdate(wishId)
                .orElseThrow(() -> new ResourceNotFoundException("Souhait", wishId));

        if (!wish.getGroup().getId().equals(groupId)) {
            throw new BusinessRuleException("Le souhait n'appartient pas à ce groupe");
        }

        if (wish.isCollective() ? !wish.getParticipants().contains(userId)
                : wish.getReservedBy() == null || !wish.getReservedBy().getId().equals(userId)) {
            throw new BusinessRuleException("Vous n'avez pas réservé ce souhait");
        }

        if (wish.isCollective()) {
            wish.getParticipants().remove(userId);
        } else {
            wish.setReservedBy(null);
        }
        wish = wishRepository.save(wish);

        return toResponse(wish, userId);
    }

    public void deleteWish(UUID groupId, UUID wishId, UUID userId) {
        Wish wish = wishRepository.findByIdForUpdate(wishId)
                .orElseThrow(() -> new ResourceNotFoundException("Souhait", wishId));

        if (!wish.getGroup().getId().equals(groupId)) {
            throw new BusinessRuleException("Le souhait n'appartient pas à ce groupe");
        }

        if (!wish.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Vous ne pouvez supprimer que vos propres souhaits");
        }

        wishRepository.delete(wish);
    }

    /**
     * Modifie un souhait existant
     */
    public WishResponse updateWish(UUID groupId, UUID wishId, WishRequest request, UUID userId) {
        Wish wish = wishRepository.findByIdForUpdate(wishId)
                .orElseThrow(() -> new ResourceNotFoundException("Souhait", wishId));

        if (!wish.getGroup().getId().equals(groupId)) {
            throw new BusinessRuleException("Le souhait n'appartient pas à ce groupe");
        }

        if (!wish.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Vous ne pouvez modifier que vos propres souhaits");
        }

        if (!wish.isCollective() && wish.getReservedBy() != null) {
            throw new BusinessRuleException("Impossible de modifier un souhait déjà réservé");
        }

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

        // Mise à jour des champs
        wish.setGiftName(request.getGiftName());
        wish.setDescription(request.getDescription());
        wish.setUrl(request.getUrl() != null ? request.getUrl().toString() : null);
        wish.setImageUrl(request.getImageUrl() != null ? request.getImageUrl().toString() : null);
        wish.setPrice(request.getPrice());
        configureReservation(wish, request);

        wish = wishRepository.save(wish);

        return toResponse(wish, userId);
    }

    public List<WishResponse> getWishesByUserInGroup(UUID groupId, UUID targetUserId, UUID requestingUserId) {
        if (!userGroupRepository.existsByUserIdAndGroupId(targetUserId, groupId)) {
            throw new BusinessRuleException("L'utilisateur cible n'appartient pas à ce groupe");
        }

        return wishRepository.findByGroupIdAndUserId(groupId, targetUserId).stream()
                .map(wish -> toResponse(wish, requestingUserId))
                .collect(Collectors.toList());
    }

    private void configureReservation(Wish wish, WishRequest request) {
        boolean collective = Boolean.TRUE.equals(request.getCollective());
        int capacity = collective ? (request.getMaxParticipants() == null ? 2 : request.getMaxParticipants()) : 1;
        if (collective && capacity < 2) {
            throw new BusinessRuleException("Un cadeau commun nécessite au moins 2 participants");
        }
        int active = participantCount(wish);
        if (capacity < active) {
            throw new BusinessRuleException("Cette modification du mode de réservation n'est pas possible");
        }
        if (wish.isCollective() && !collective && active == 1) {
            UUID participant = wish.getParticipants().iterator().next();
            wish.setReservedBy(userRepository.findById(participant)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", participant)));
            wish.getParticipants().clear();
        }
        wish.setCollective(collective);
        wish.setMaxParticipants(capacity);
    }

    private WishResponse toResponse(Wish wish, UUID viewerId) {
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
        resp.setCollective(wish.isCollective());
        resp.setMaxParticipants(wish.getMaxParticipants());
        int count = participantCount(wish);
        resp.setParticipantCount(count);
        resp.setFull(count >= wish.getMaxParticipants());
        resp.setParticipating(wish.isCollective() ? wish.getParticipants().contains(viewerId)
                : wish.getReservedBy() != null && wish.getReservedBy().getId().equals(viewerId));
        if (wish.isCollective()) {
            resp.setParticipants(userRepository.findAllById(wish.getParticipants()).stream()
                    .map(this::toParticipantResponse)
                    .collect(Collectors.toList()));
        }
        if (!wish.isCollective() && wish.getReservedBy() != null) {
            resp.setReservedBy(wish.getReservedBy().getId());
        }
        resp.setCreatedAt(wish.getCreatedAt());
        return resp;
    }

    private UserResponse toParticipantResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setIsAdmin(user.isAdmin());
        response.setAvatarId(user.getAvatarId());
        response.setPseudo(user.getPseudo());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    private int participantCount(Wish wish) {
        return wish.isCollective() ? wish.getParticipants().size() : wish.getReservedBy() == null ? 0 : 1;
    }
}
