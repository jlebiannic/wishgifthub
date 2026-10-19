package com.wishgifthub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wishgifthub.entity.Group;
import com.wishgifthub.entity.User;
import com.wishgifthub.entity.UserGroup;
import com.wishgifthub.repository.GroupRepository;
import com.wishgifthub.repository.UserGroupRepository;
import com.wishgifthub.repository.UserRepository;
import com.wishgifthub.repository.WishRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:children-api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "wishgifthub.jwt.secret=children-api-test-only-signing-key-not-for-production",
        "wishgifthub.jwt.expiration-ms=600000"
})
@AutoConfigureMockMvc
class ChildApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired GroupRepository groups;
    @Autowired UserGroupRepository memberships;
    @Autowired WishRepository wishes;
    @Autowired JwtService jwt;

    @Test
    void invitedParentManagesMultipleChildrenAndTheirWishesWithoutChangingIdentity() throws Exception {
        User admin = user(true);
        User parent = user(false);
        User other = user(false);
        User outsider = user(false);
        Group group = group(admin, parent, other);
        Group secondGroup = group(admin, parent);
        String root = "/api/groups/" + group.getId();
        String children = root + "/children";
        String parentToken = token(parent, group);
        String otherToken = token(other, group);

        mvc.perform(get(children)).andExpect(status().isUnauthorized());
        mvc.perform(post(children).header("Authorization", token(outsider))
                .contentType(MediaType.APPLICATION_JSON).content("{\"pseudo\":\"Enfant\"}"))
                .andExpect(status().isForbidden());
        for (String invalid : List.of("{}", "{\"pseudo\":\"   \"}", "{\"pseudo\":\"" + "a".repeat(101) + "\"}")) {
            mvc.perform(post(children).header("Authorization", parentToken)
                    .contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
        }
        UUID first = createChild(children, parentToken, "Premier", parent);
        UUID second = createChild(children, parentToken, "Deuxieme", parent);
        createChild(children, parentToken, "Troisieme", parent);
        UUID othersChild = createChild(children, otherToken, "Autre", other);
        UUID anotherGroupChild = createChild("/api/groups/" + secondGroup.getId() + "/children",
                token(parent, secondGroup), "Autre groupe", parent);
        mvc.perform(get(children).header("Authorization", parentToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get(children).header("Authorization", otherToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(root + "/users").header("Authorization", parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + first + "')].parentId").value(parent.getId().toString()));

        mvc.perform(put(children + "/" + first).header("Authorization", parentToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"pseudo\":\"Nouveau nom\",\"avatarId\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pseudo").value("Nouveau nom"));
        assertNull(users.findById(first).orElseThrow().getAvatarId());
        for (String forbiddenToken : List.of(otherToken, token(admin, group))) {
            mvc.perform(put(children + "/" + first).header("Authorization", forbiddenToken)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"pseudo\":\"Interdit\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(delete(children + "/" + first).header("Authorization", forbiddenToken))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(delete(children + "/" + anotherGroupChild).header("Authorization", parentToken))
                .andExpect(status().isForbidden());
        mvc.perform(get(children).header("Authorization",
                "Bearer " + jwt.generateToken(users.findById(first).orElseThrow(), List.of(group.getId()))))
                .andExpect(status().isUnauthorized());

        String wishRoot = root + "/wishes";
        String firstWish = createWish(wishRoot, parentToken, first, false);
        String secondWish = createWish(wishRoot, parentToken, second, true);
        mvc.perform(post(wishRoot).header("Authorization", parentToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"giftName\":\"Pour moi\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(parent.getId().toString()));
        for (UUID forbiddenOwner : List.of(other.getId(), othersChild, anotherGroupChild)) {
            mvc.perform(post(wishRoot).header("Authorization", parentToken).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"giftName\":\"Interdit\",\"userId\":\"" + forbiddenOwner + "\"}"))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get(wishRoot + "/users/" + first).header("Authorization", parentToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(firstWish));
        mvc.perform(put(wishRoot + "/" + firstWish).header("Authorization", parentToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"giftName\":\"Modifie\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(first.toString()));
        mvc.perform(put(wishRoot + "/" + firstWish).header("Authorization", parentToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"giftName\":\"Transfert\",\"userId\":\"" + second + "\"}"))
                .andExpect(status().isConflict());
        mvc.perform(put(wishRoot + "/" + firstWish).header("Authorization", otherToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"giftName\":\"Interdit\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete(wishRoot + "/" + firstWish).header("Authorization", otherToken))
                .andExpect(status().isForbidden());

        mvc.perform(post(wishRoot + "/" + firstWish + "/reserve").header("Authorization", otherToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reservedBy").value(other.getId().toString()));
        mvc.perform(put(wishRoot + "/" + firstWish).header("Authorization", parentToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"giftName\":\"Reserve\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post(wishRoot + "/" + secondWish + "/reserve").header("Authorization", parentToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.participants[0].id").value(parent.getId().toString()));
        mvc.perform(put(wishRoot + "/" + secondWish).header("Authorization", parentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"giftName\":\"Commun modifie\",\"collective\":true,\"maxParticipants\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.participantCount").value(1));
        mvc.perform(delete(wishRoot + "/" + secondWish).header("Authorization", parentToken))
                .andExpect(status().isNoContent());
        assertFalse(wishes.existsById(UUID.fromString(secondWish)));

        // Leave a collective wish with a participant to verify collection cleanup on profile deletion.
        String collective = createWish(wishRoot, parentToken, first, true);
        mvc.perform(post(wishRoot + "/" + collective + "/reserve").header("Authorization", otherToken))
                .andExpect(status().isOk());
        mvc.perform(delete(children + "/" + first).header("Authorization", parentToken))
                .andExpect(status().isNoContent());
        assertFalse(users.existsById(first));
        assertTrue(wishes.findByUserId(first).isEmpty());
        assertTrue(memberships.findByUserId(first).isEmpty());
        assertTrue(users.existsById(second));
        assertTrue(users.existsById(parent.getId()));
        assertFalse(wishes.existsById(UUID.fromString(firstWish)));
        mvc.perform(get(children).header("Authorization", parentToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(delete(children + "/" + first).header("Authorization", parentToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void staleGroupAuthorityDoesNotAllowManagingChildren() throws Exception {
        User parent = user(true);
        Group group = group(parent);
        String authorization = token(parent, group);
        memberships.deleteAll(memberships.findByUserId(parent.getId()));
        mvc.perform(post("/api/groups/" + group.getId() + "/children")
                .header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON)
                .content("{\"pseudo\":\"Interdit\"}")).andExpect(status().isForbidden());
    }

    private UUID createChild(String path, String token, String name, User parent) throws Exception {
        var result = mvc.perform(post(path).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"pseudo\":\"" + name + "\",\"avatarId\":\"avatar-1\","
                        + "\"parentId\":\"" + UUID.randomUUID() + "\",\"isAdmin\":true,\"email\":\"ignored@test\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pseudo").value(name))
                .andExpect(jsonPath("$.parentId").value(parent.getId().toString()))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.isAdmin").value(false))
                .andExpect(jsonPath("$.passwordHash").doesNotExist()).andReturn();
        UUID id = UUID.fromString(json.readTree(result.getResponse().getContentAsString()).get("id").asText());
        assertNull(users.findById(id).orElseThrow().getPasswordHash());
        return id;
    }

    private String createWish(String path, String token, UUID childId, boolean collective) throws Exception {
        var result = mvc.perform(post(path).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"giftName\":\"Cadeau\",\"collective\":" + collective + ",\"userId\":\"" + childId + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(childId.toString())).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private User user(boolean admin) {
        User user = new User();
        user.setEmail(UUID.randomUUID() + "@children-test.example");
        user.setAdmin(admin);
        return users.save(user);
    }

    private Group group(User admin, User... members) {
        Group group = new Group();
        group.setName("Noel");
        group.setType("noël");
        group.setAdmin(admin);
        group = groups.save(group);
        membership(admin, group);
        for (User member : members) membership(member, group);
        return group;
    }

    private void membership(User user, Group group) {
        UserGroup membership = new UserGroup();
        membership.setUser(user);
        membership.setGroup(group);
        memberships.save(membership);
    }

    private String token(User user, Group... groups) {
        return "Bearer " + jwt.generateToken(user, java.util.Arrays.stream(groups).map(Group::getId).toList());
    }
}
