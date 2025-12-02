package com.wishgifthub.controller;

import com.wishgifthub.entity.User;
import com.wishgifthub.openapi.api.SouhaitsApi;
import com.wishgifthub.openapi.model.WishRequest;
import com.wishgifthub.openapi.model.WishResponse;
import com.wishgifthub.service.WishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Validated
public class WishController implements SouhaitsApi {
    @Autowired
    private WishService wishService;

    @PreAuthorize("hasAuthority('GROUP_' + #groupId)")
    @Override
    public ResponseEntity<WishResponse> addWish(UUID groupId, WishRequest wishRequest) {
        User user = getCurrentUser();
        return ResponseEntity.ok(wishService.createWish(groupId, wishRequest, user.getId()));
    }

    @PreAuthorize("hasAuthority('GROUP_' + #groupId)")
    @Override
    public ResponseEntity<List<WishResponse>> getGroupWishes(UUID groupId) {
        User user = getCurrentUser();
        return ResponseEntity.ok(wishService.getWishesByGroup(groupId, user.getId()));
    }

    @PreAuthorize("hasAuthority('GROUP_' + #groupId)")
    @Override
    public ResponseEntity<Void> deleteWish(UUID groupId, UUID wishId) {
        User user = getCurrentUser();
        wishService.deleteWish(groupId, wishId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('GROUP_' + #groupId)")
    @Override
    public ResponseEntity<WishResponse> reserveWish(UUID groupId, UUID wishId) {
        User user = getCurrentUser();
        return ResponseEntity.ok(wishService.reserveWish(groupId, wishId, user.getId()));
    }

    @PreAuthorize("hasAuthority('GROUP_' + #groupId)")
    @Override
    public ResponseEntity<WishResponse> unreserveWish(UUID groupId, UUID wishId) {
        User user = getCurrentUser();
        return ResponseEntity.ok(wishService.unreserveWish(groupId, wishId, user.getId()));
    }

    @PreAuthorize("hasAuthority('GROUP_' + #groupId)")
    @Override
    public ResponseEntity<List<WishResponse>> getMyWishes(UUID groupId) {
        User user = getCurrentUser();
        return ResponseEntity.ok(wishService.getWishesByUserInGroup(groupId, user.getId(), user.getId()));
    }

    @PreAuthorize("hasAuthority('GROUP_' + #groupId)")
    @Override
    public ResponseEntity<List<WishResponse>> getUserWishes(UUID groupId, UUID userId) {
        User user = getCurrentUser();
        return ResponseEntity.ok(wishService.getWishesByUserInGroup(groupId, userId, user.getId()));
    }

    private User getCurrentUser() {
        return (User) org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication().getPrincipal();
    }
}

