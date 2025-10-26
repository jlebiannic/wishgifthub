package com.wishgifthub.dto;

import java.util.UUID;
import java.time.OffsetDateTime;

public class GroupResponse {
    public UUID id;
    public String name;
    public String type;
    public UUID adminId;
    public OffsetDateTime createdAt;
}

