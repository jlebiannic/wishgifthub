package com.wishgifthub.repository;

import com.wishgifthub.entity.Wish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface WishRepository extends JpaRepository<Wish, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wish w where w.id = :id")
    Optional<Wish> findByIdForUpdate(UUID id);

    List<Wish> findByGroupId(UUID groupId);
    List<Wish> findByUserId(UUID userId);
    List<Wish> findByGroupIdAndUserId(UUID groupId, UUID userId);
}
