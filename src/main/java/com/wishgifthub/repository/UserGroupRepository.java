package com.wishgifthub.repository;

import com.wishgifthub.entity.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface UserGroupRepository extends JpaRepository<UserGroup, UUID> {
    List<UserGroup> findByUserId(UUID userId);
    List<UserGroup> findByGroupId(UUID groupId);
    boolean existsByUserIdAndGroupId(UUID userId, UUID groupId);
}

