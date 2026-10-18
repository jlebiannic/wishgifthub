package com.wishgifthub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wishgifthub.entity.Group;
import com.wishgifthub.entity.User;
import com.wishgifthub.entity.UserGroup;
import com.wishgifthub.repository.GroupRepository;
import com.wishgifthub.repository.UserGroupRepository;
import com.wishgifthub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:collective-api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "wishgifthub.jwt.secret=collective-api-test-only-signing-key-not-for-production",
        "wishgifthub.jwt.expiration-ms=60000"
})
@AutoConfigureMockMvc
class CollectiveGiftApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired GroupRepository groups;
    @Autowired UserGroupRepository memberships;
    @Autowired JwtService jwt;

    @Test
    void authenticatedApiLifecycleAndRecipientRedaction() throws Exception {
        User owner = user();
        User first = user();
        User second = user();
        User outsider = user();
        Group group = new Group();
        group.setAdmin(owner);
        group.setName("API test");
        group.setType("BIRTHDAY");
        group = groups.save(group);
        for (User user : List.of(owner, first, second)) {
            UserGroup membership = new UserGroup();
            membership.setGroup(group);
            membership.setUser(user);
            memberships.save(membership);
        }
        String ownerToken = token(owner, group);
        String firstToken = token(first, group);
        String secondToken = token(second, group);
        String root = "/api/groups/" + group.getId() + "/wishes";
        String body = "{\"giftName\":\"Collective\",\"collective\":true,\"maxParticipants\":2}";

        mvc.perform(post(root).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(root).header("Authorization", "Bearer " + jwt.generateToken(outsider, List.of()))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post(root).header("Authorization", ownerToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"giftName\":\"Invalid\",\"collective\":true,\"maxParticipants\":1}"))
                .andExpect(status().isBadRequest());
        var created = mvc.perform(post(root).header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.collective").value(true))
                .andExpect(jsonPath("$.participantCount").doesNotExist()).andReturn();
        String wishId = json.readTree(created.getResponse().getContentAsString()).get("id").asText();
        String wish = root + "/" + wishId;
        mvc.perform(post(wish + "/reserve").header("Authorization", ownerToken))
                .andExpect(status().isConflict());
        mvc.perform(post(wish + "/reserve").header("Authorization", firstToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.participantCount").value(1))
                .andExpect(jsonPath("$.participating").value(true));
        mvc.perform(post(wish + "/reserve").header("Authorization", firstToken))
                .andExpect(status().isConflict());
        mvc.perform(post(wish + "/reserve").header("Authorization", secondToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.full").value(true));
        for (String path : List.of(root, root + "/me", root + "/users/" + owner.getId())) {
            mvc.perform(get(path).header("Authorization", ownerToken)).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].participantCount").doesNotExist())
                    .andExpect(jsonPath("$[0].participating").doesNotExist())
                    .andExpect(jsonPath("$[0].full").doesNotExist())
                    .andExpect(jsonPath("$[0].reservedBy").doesNotExist());
        }
        mvc.perform(put(wish).header("Authorization", firstToken).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(put(wish).header("Authorization", ownerToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"giftName\":\"Collective\",\"collective\":false}")).andExpect(status().isConflict());
        mvc.perform(delete(wish + "/reserve").header("Authorization", ownerToken))
                .andExpect(status().isConflict());
        mvc.perform(delete(wish + "/reserve").header("Authorization", firstToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.full").value(false))
                .andExpect(jsonPath("$.participantCount").value(1));
        mvc.perform(post(wish + "/reserve").header("Authorization", firstToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.full").value(true));
    }

    private User user() {
        User user = new User();
        user.setEmail(UUID.randomUUID() + "@api-test.example");
        return users.save(user);
    }

    private String token(User user, Group group) {
        return "Bearer " + jwt.generateToken(user, List.of(group.getId()));
    }
}
