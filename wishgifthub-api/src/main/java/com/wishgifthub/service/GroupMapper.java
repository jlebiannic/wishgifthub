package com.wishgifthub.service;

import com.wishgifthub.entity.Group;
import com.wishgifthub.openapi.model.GroupResponse;

final class GroupMapper {
    private GroupMapper() {
    }

    static GroupResponse toResponse(Group group) {
        GroupResponse response = new GroupResponse();
        response.setId(group.getId());
        response.setName(group.getName());
        response.setType(group.getType());
        response.setAdminId(group.getAdmin().getId());
        response.setCreatedAt(group.getCreatedAt());
        response.setCollectiveGiftThreshold(group.getCollectiveGiftThreshold());
        return response;
    }
}
