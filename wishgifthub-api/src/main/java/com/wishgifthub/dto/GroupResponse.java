package com.wishgifthub.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class GroupResponse {
    public UUID id;
    public String name;
    public String type;
    public UUID adminId;
    public OffsetDateTime createdAt;
    public String jwtToken;
}

