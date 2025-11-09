package com.wishgifthub.repository;

import com.wishgifthub.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<Group, UUID> {
    List<Group> findByAdminId(UUID adminId);
    Optional<Group> findByIdAndAdminId(UUID id, UUID adminId);
}

