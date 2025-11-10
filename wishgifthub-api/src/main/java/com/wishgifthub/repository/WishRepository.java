package com.wishgifthub.repository;

import com.wishgifthub.entity.Wish;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface WishRepository extends JpaRepository<Wish, UUID> {
    List<Wish> findByGroupId(UUID groupId);
    List<Wish> findByGroupIdAndUserId(UUID groupId, UUID userId);
}

