package com.wishgifthub.dto;

import java.util.UUID;
import java.time.OffsetDateTime;

public class WishRequest {
    public String giftName;
    public String description;
    public String url;
}

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
package com.wishgifthub.dto;

import java.util.UUID;

public class AuthRequest {
    public String email;
    public String password;
}

public class AuthResponse {
    public UUID userId;
    public boolean isAdmin;
    public String token;
}

