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
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:provisional-api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "wishgifthub.jwt.secret=provisional-api-test-only-signing-key-not-for-production",
        "wishgifthub.jwt.expiration-ms=600000"
})
@AutoConfigureMockMvc
class ProvisionalWishApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired GroupRepository groups;
    @Autowired UserGroupRepository memberships;
    @Autowired WishRepository wishes;
    @Autowired WishService service;
    @Autowired JwtService jwt;

    @Test
    void listsFilterDraftsAndGuessedIdsCannotReserveOrUnreserveEvenForAdmins() throws Exception {
        User admin = user(true);
        User owner = user(false);
        User other = user(false);
        Group group = group(admin, owner, other);
        String root = root(group);
        String draft = create(root, owner, group, "\"provisional\":true");
        String published = create(root, owner, group, "");
        for (User viewer : List.of(owner, other, admin)) {
            int count = viewer == owner ? 2 : 1;
            for (String path : List.of(root, root + "/users/" + owner.getId())) {
                mvc.perform(get(path).header("Authorization", token(viewer, group)))
                        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(count));
            }
            mvc.perform(get(root + "/me").header("Authorization", token(viewer, group)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(viewer == owner ? 2 : 0));
            for (String action : List.of("reserve", "unreserve")) {
                String path = root + "/" + draft + "/reserve";
                var result = mvc.perform((action.equals("reserve") ? post(path) : delete(path))
                        .header("Authorization", token(viewer, group)));
                result.andExpect(viewer == owner ? status().isConflict() : status().isNotFound());
            }
        }
        for (User viewer : List.of(other, admin)) {
            update(root, draft, viewer, group, "\"provisional\":false").andExpect(status().isForbidden());
            mvc.perform(delete(root + "/" + draft).header("Authorization", token(viewer, group)))
                    .andExpect(status().isForbidden());
        }
        update(root, draft, owner, group, "").andExpect(status().isOk())
                .andExpect(jsonPath("$.provisional").value(true));
        assertTrue(wishes.findById(UUID.fromString(draft)).orElseThrow().isProvisional());
        update(root, draft, owner, group, "\"provisional\":false").andExpect(status().isOk())
                .andExpect(jsonPath("$.provisional").value(false));
        update(root, draft, owner, group, "\"provisional\":true").andExpect(status().isOk());
        update(root, draft, owner, group, "\"provisional\":false").andExpect(status().isOk());
        mvc.perform(post(root + "/" + draft + "/reserve").header("Authorization", token(other, group)))
                .andExpect(status().isOk());
        assertFalse(wishes.findById(UUID.fromString(published)).orElseThrow().isProvisional());
        mvc.perform(delete(root + "/" + published).header("Authorization", token(owner, group)))
                .andExpect(status().isNoContent());
        mvc.perform(get(root)).andExpect(status().isUnauthorized());
        mvc.perform(get(root).header("Authorization",
                "Bearer " + jwt.generateToken(user(false), List.of())))
                .andExpect(status().isForbidden());
    }

    @Test
    void managingParentSeesAndManagesChildDraftOnlyWhileMemberOfTheSameGroup() throws Exception {
        User admin = user(true);
        User parent = user(false);
        User other = user(false);
        User child = new User();
        child.setParentId(parent.getId());
        child.setPseudo("Enfant");
        child = users.save(child);
        Group group = group(admin, parent, other, child);
        String root = root(group);
        String draft = create(root, parent, group, "\"userId\":\"" + child.getId() + "\",\"provisional\":true");
        create(root, parent, group, "\"provisional\":true");
        for (User viewer : List.of(parent, admin, other)) {
            mvc.perform(get(root).header("Authorization", token(viewer, group)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(viewer == parent ? 2 : 0));
            mvc.perform(get(root + "/users/" + child.getId()).header("Authorization", token(viewer, group)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(viewer == parent ? 1 : 0));
            mvc.perform(get(root + "/me").header("Authorization", token(viewer, group)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(viewer == parent ? 1 : 0));
        }
        assertEquals(1, service.getWishesByUserInGroup(group.getId(), child.getId(), child.getId()).size());
        assertEquals(1, service.getWishesByGroup(group.getId(), child.getId()).size());
        for (String action : List.of("reserve", "unreserve")) {
            String path = root + "/" + draft + "/reserve";
            mvc.perform((action.equals("reserve") ? post(path) : delete(path)).header("Authorization", token(parent, group)))
                    .andExpect(status().isConflict());
            mvc.perform((action.equals("reserve") ? post(path) : delete(path)).header("Authorization", token(other, group)))
                    .andExpect(status().isNotFound());
        }
        update(root, draft, parent, group, "").andExpect(status().isOk())
                .andExpect(jsonPath("$.provisional").value(true));
        memberships.deleteAll(memberships.findByUserId(parent.getId()));
        assertTrue(service.getWishesByUserInGroup(group.getId(), child.getId(), parent.getId()).isEmpty());
        assertEquals(1, service.getWishesByGroup(group.getId(), parent.getId()).size());
        for (String action : List.of("reserve", "unreserve")) {
            String path = root + "/" + draft + "/reserve";
            mvc.perform((action.equals("reserve") ? post(path) : delete(path)).header("Authorization", token(parent, group)))
                    .andExpect(status().isNotFound());
        }
        update(root, draft, parent, group, "\"provisional\":false").andExpect(status().isForbidden());
        membership(parent, group);
        update(root, draft, parent, group, "\"provisional\":false").andExpect(status().isOk());
        mvc.perform(post(root + "/" + draft + "/reserve").header("Authorization", token(other, group)))
                .andExpect(status().isOk());
        mvc.perform(delete(root + "/" + draft + "/reserve").header("Authorization", token(other, group)))
                .andExpect(status().isOk());
        mvc.perform(delete(root + "/" + draft).header("Authorization", token(parent, group)))
                .andExpect(status().isNoContent());
    }

    @Test
    void cannotHideReservedIndividualOrCollectiveAndFailedUpdatePreservesEverything() throws Exception {
        User owner = user(false);
        User other = user(false);
        Group group = group(user(true), owner, other);
        String root = root(group);
        for (boolean collective : List.of(false, true)) {
            String id = create(root, owner, group, "\"collective\":" + collective);
            mvc.perform(post(root + "/" + id + "/reserve").header("Authorization", token(other, group)))
                    .andExpect(status().isOk());
            // Also attempt converting a collective wish to individual during the rejected transition.
            for (boolean nextCollective : List.of(false, true)) {
                update(root, id, owner, group, "\"provisional\":true,\"collective\":" + nextCollective)
                        .andExpect(status().isConflict());
                var saved = service.getWishesByUserInGroup(group.getId(), owner.getId(), owner.getId()).stream()
                        .filter(w -> w.getId().equals(UUID.fromString(id))).findFirst().orElseThrow();
                assertFalse(saved.getProvisional());
                assertEquals("Cadeau", saved.getGiftName());
                assertEquals(collective, saved.getCollective());
                assertEquals(1, saved.getParticipantCount());
                if (collective) {
                    assertEquals(other.getId(), saved.getParticipants().getFirst().getId());
                } else {
                    assertEquals(other.getId(), saved.getReservedBy());
                }
            }
            mvc.perform(delete(root + "/" + id + "/reserve").header("Authorization", token(other, group)))
                    .andExpect(status().isOk());
            update(root, id, owner, group, "\"provisional\":true,\"collective\":" + collective)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.provisional").value(true));
            for (String action : List.of("reserve", "unreserve")) {
                String path = root + "/" + id + "/reserve";
                mvc.perform((action.equals("reserve") ? post(path) : delete(path))
                        .header("Authorization", token(other, group))).andExpect(status().isNotFound());
            }
        }
    }

    private String create(String root, User actor, Group group, String extra) throws Exception {
        var response = mvc.perform(post(root).header("Authorization", token(actor, group))
                .contentType(MediaType.APPLICATION_JSON).content(body("Cadeau", extra)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provisional").value(extra.contains("\"provisional\":true")))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asText();
    }

    private ResultActions update(String root, String id, User actor, Group group, String extra) throws Exception {
        return mvc.perform(put(root + "/" + id).header("Authorization", token(actor, group))
                .contentType(MediaType.APPLICATION_JSON).content(body("Modifie", extra)));
    }

    private String body(String name, String extra) {
        return "{\"giftName\":\"" + name + "\"" + (extra.isEmpty() ? "" : "," + extra) + "}";
    }

    private User user(boolean admin) {
        User user = new User();
        user.setEmail(UUID.randomUUID() + "@provisional-test.example");
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

    private String root(Group group) {
        return "/api/groups/" + group.getId() + "/wishes";
    }

    private String token(User user, Group group) {
        return "Bearer " + jwt.generateToken(user, List.of(group.getId()));
    }
}
