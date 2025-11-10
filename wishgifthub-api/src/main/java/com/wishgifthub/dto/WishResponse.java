package com.wishgifthub.dto;

import java.util.UUID;
import java.time.OffsetDateTime;

public class WishResponse {
    public UUID id;
    public UUID userId;
    public UUID groupId;
    public String giftName;
    public String description;
    public String url;
    public UUID reservedBy;
    public OffsetDateTime createdAt;
}

