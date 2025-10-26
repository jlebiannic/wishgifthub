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
