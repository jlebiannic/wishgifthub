package com.wishgifthub.controller;

import com.wishgifthub.dto.WishRequest;
import com.wishgifthub.dto.WishResponse;
import com.wishgifthub.entity.User;
import com.wishgifthub.service.WishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups/{groupId}/wishes")
public class WishController {
    @Autowired
    private WishService wishService;

    @PostMapping
    public ResponseEntity<WishResponse> addWish(@PathVariable UUID groupId, @RequestBody WishRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(wishService.addWish(groupId, request, user));
    }

    @GetMapping("/me")
    public ResponseEntity<List<WishResponse>> getMyWishes(@PathVariable UUID groupId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(wishService.getMyWishes(groupId, user));
    }

    @GetMapping
    public ResponseEntity<List<WishResponse>> getGroupWishes(@PathVariable UUID groupId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(wishService.getGroupWishes(groupId, user));
    }

    @PutMapping("/{wishId}")
    public ResponseEntity<WishResponse> updateWish(@PathVariable UUID groupId, @PathVariable UUID wishId, @RequestBody WishRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(wishService.updateWish(groupId, wishId, request, user));
    }

    @DeleteMapping("/{wishId}")
    public ResponseEntity<Void> deleteWish(@PathVariable UUID groupId, @PathVariable UUID wishId, @AuthenticationPrincipal User user) {
        wishService.deleteWish(groupId, wishId, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{wishId}/reserve")
    public ResponseEntity<WishResponse> reserveWish(@PathVariable UUID groupId, @PathVariable UUID wishId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(wishService.reserveWish(groupId, wishId, user));
    }

    @DeleteMapping("/{wishId}/reserve")
    public ResponseEntity<WishResponse> unreserveWish(@PathVariable UUID groupId, @PathVariable UUID wishId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(wishService.unreserveWish(groupId, wishId, user));
    }
}
package com.wishgifthub.service;

import com.wishgifthub.dto.WishRequest;
import com.wishgifthub.dto.WishResponse;
import com.wishgifthub.entity.Group;
import com.wishgifthub.entity.User;
import com.wishgifthub.entity.Wish;
import com.wishgifthub.repository.GroupRepository;
import com.wishgifthub.repository.UserGroupRepository;
import com.wishgifthub.repository.WishRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private void checkMembership(UUID userId, UUID groupId) {
        if (!userGroupRepository.existsByUserIdAndGroupId(userId, groupId)) {
            throw new SecurityException("L'utilisateur n'appartient pas à ce groupe");
        }
    }

    @Transactional
    public WishResponse addWish(UUID groupId, WishRequest request, User user) {
        checkMembership(user.getId(), groupId);
        Group group = groupRepository.findById(groupId).orElseThrow();
        Wish wish = new Wish();
        wish.setUser(user);
        wish.setGroup(group);
        wish.setGiftName(request.giftName);
        wish.setDescription(request.description);
        wish.setUrl(request.url);
        wishRepository.save(wish);
        return toResponse(wish);
    }

    public List<WishResponse> getMyWishes(UUID groupId, User user) {
        checkMembership(user.getId(), groupId);
        return wishRepository.findByGroupIdAndUserId(groupId, user.getId()).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<WishResponse> getGroupWishes(UUID groupId, User user) {
        checkMembership(user.getId(), groupId);
        return wishRepository.findByGroupId(groupId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public WishResponse updateWish(UUID groupId, UUID wishId, WishRequest request, User user) {
        checkMembership(user.getId(), groupId);
        Wish wish = wishRepository.findById(wishId).orElseThrow();
        if (!wish.getUser().getId().equals(user.getId())) throw new SecurityException();
        wish.setGiftName(request.giftName);
        wish.setDescription(request.description);
        wish.setUrl(request.url);
        wishRepository.save(wish);
        return toResponse(wish);
    }

    @Transactional
    public void deleteWish(UUID groupId, UUID wishId, User user) {
        checkMembership(user.getId(), groupId);
        Wish wish = wishRepository.findById(wishId).orElseThrow();
        if (!wish.getUser().getId().equals(user.getId())) throw new SecurityException();
        wishRepository.delete(wish);
    }

    @Transactional
    public WishResponse reserveWish(UUID groupId, UUID wishId, User user) {
        checkMembership(user.getId(), groupId);
        Wish wish = wishRepository.findById(wishId).orElseThrow();
        if (wish.getUser().getId().equals(user.getId())) throw new SecurityException("Impossible de réserver son propre souhait");
        wish.setReservedBy(user);
        wishRepository.save(wish);
        return toResponse(wish);
    }

    @Transactional
    public WishResponse unreserveWish(UUID groupId, UUID wishId, User user) {
        checkMembership(user.getId(), groupId);
        Wish wish = wishRepository.findById(wishId).orElseThrow();
        if (wish.getReservedBy() == null || !wish.getReservedBy().getId().equals(user.getId())) throw new SecurityException();
        wish.setReservedBy(null);
        wishRepository.save(wish);
        return toResponse(wish);
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

