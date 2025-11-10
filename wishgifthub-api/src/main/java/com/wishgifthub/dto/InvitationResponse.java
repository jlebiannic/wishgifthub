package com.wishgifthub.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class InvitationResponse {
    public UUID id;
    public String email;
    public UUID groupId;
    public UUID token;
    public boolean accepted;
    public OffsetDateTime createdAt;
    public String invitationLink;
    public String jwtToken;
}

