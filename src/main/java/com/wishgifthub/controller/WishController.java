package com.wishgifthub.controller;

import com.wishgifthub.dto.WishRequest;
import com.wishgifthub.dto.WishResponse;
import com.wishgifthub.entity.User;
import com.wishgifthub.service.WishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups/{groupId}/wishes")
public class WishController {
    @Autowired
    private WishService wishService;

    @PreAuthorize("hasAuthority('GROUP_' + #groupId)")
    @PostMapping
    public ResponseEntity<WishResponse> addWish(@PathVariable UUID groupId, @RequestBody WishRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(wishService.createWish(groupId, request, user.getId()));
    }

    @PreAuthorize("hasAuthority('GROUP_' + #groupId)")
    @GetMapping
    public ResponseEntity<List<WishResponse>> getGroupWishes(@PathVariable UUID groupId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(wishService.getWishesByGroup(groupId, user.getId()));
    }

    @PreAuthorize("hasAuthority('GROUP_' + #groupId)")
    @DeleteMapping("/{wishId}")
    public ResponseEntity<Void> deleteWish(@PathVariable UUID groupId, @PathVariable UUID wishId, @AuthenticationPrincipal User user) {
        wishService.deleteWish(groupId, wishId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('GROUP_' + #groupId)")
    @PostMapping("/{wishId}/reserve")
    public ResponseEntity<WishResponse> reserveWish(@PathVariable UUID groupId, @PathVariable UUID wishId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(wishService.reserveWish(groupId, wishId, user.getId()));
    }

    @PreAuthorize("hasAuthority('GROUP_' + #groupId)")
    @DeleteMapping("/{wishId}/reserve")
    public ResponseEntity<WishResponse> unreserveWish(@PathVariable UUID groupId, @PathVariable UUID wishId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(wishService.unreserveWish(groupId, wishId, user.getId()));
    }

    @PreAuthorize("hasAuthority('GROUP_' + #groupId)")
    @GetMapping("/me")
    public ResponseEntity<List<WishResponse>> getMyWishes(@PathVariable UUID groupId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(wishService.getWishesByUserInGroup(groupId, user.getId(), user.getId()));
    }

    @PreAuthorize("hasAuthority('GROUP_' + #groupId)")
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<WishResponse>> getUserWishes(@PathVariable UUID groupId, @PathVariable UUID userId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(wishService.getWishesByUserInGroup(groupId, userId, user.getId()));
    }
}
