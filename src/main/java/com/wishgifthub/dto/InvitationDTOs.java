package com.wishgifthub.dto;

import java.util.UUID;
import java.time.OffsetDateTime;

public class InvitationRequest {
    public String email;
}

public class InvitationResponse {
    public UUID id;
    public String email;
    public UUID groupId;
    public UUID token;
    public boolean accepted;
    public OffsetDateTime createdAt;
    public String invitationLink;
}

