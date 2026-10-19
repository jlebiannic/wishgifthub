package com.wishgifthub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wishgifthub.entity.User;
import com.wishgifthub.openapi.model.GroupRequest;
import com.wishgifthub.repository.GroupRepository;
import com.wishgifthub.repository.UserRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:group-threshold-api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "wishgifthub.jwt.secret=group-threshold-api-test-signing-key-not-for-production",
        "wishgifthub.jwt.expiration-ms=60000"
})
@AutoConfigureMockMvc
class GroupThresholdApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired GroupRepository groups;
    @Autowired GroupService service;
    @Autowired JwtService jwt;

    @Test
    void defaultsCustomUpdatesAndOmittedUpdatesRemainScopedToGroup() throws Exception {
        User owner = user(true);
        UUID defaultGroup = create(owner, "", 50);
        UUID customGroup = create(owner, ",\"collectiveGiftThreshold\":75.25", 75.25);
        String token = token(owner);
        mvc.perform(get("/api/groups").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + customGroup + "')].collectiveGiftThreshold")
                        .value(org.hamcrest.Matchers.contains(75.25)));
        mvc.perform(get("/api/groups/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + defaultGroup + "')].collectiveGiftThreshold")
                        .value(org.hamcrest.Matchers.contains(50.0)));
        update(owner, customGroup, ",\"collectiveGiftThreshold\":125.50", 125.5);
        update(owner, customGroup, "", 125.5);
        update(owner, customGroup, ",\"collectiveGiftThreshold\":null", 125.5);
        assertEquals(new BigDecimal("50.00"), groups.findById(defaultGroup).orElseThrow().getCollectiveGiftThreshold());
        assertEquals(new BigDecimal("125.50"), groups.findById(customGroup).orElseThrow().getCollectiveGiftThreshold());
        update(owner, customGroup, ",\"collectiveGiftThreshold\":0", 0);
        update(owner, customGroup, ",\"collectiveGiftThreshold\":99999999.99", 99999999.99);

        // The threshold only suggests a collective gift; it never forces one.
        mvc.perform(post("/api/groups/" + defaultGroup + "/wishes")
                        .header("Authorization", "Bearer " + jwt.generateToken(owner, List.of(defaultGroup)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"giftName\":\"Individual\",\"price\":\"100.00 €\",\"collective\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.collective").value(false));
    }

    @Test
    void onlyOwnerCanChangeThresholdAndOnlyAdminsCanCreateGroups() throws Exception {
        User owner = user(true);
        User otherAdmin = user(true);
        User member = user(false);
        UUID group = create(owner, "", 50);
        String body = body(",\"collectiveGiftThreshold\":10");
        mvc.perform(post("/api/groups").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/groups").header("Authorization", token(member))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        for (User unauthorized : List.of(member, otherAdmin)) {
            mvc.perform(put("/api/groups/" + group).header("Authorization", token(unauthorized))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(put("/api/groups/" + group).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        assertEquals(new BigDecimal("50.00"), groups.findById(group).orElseThrow().getCollectiveGiftThreshold());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "100000000", "50.001", "\"NaN\"", "\"Infinity\"", "NaN", "1e9999"})
    void invalidThresholdsAreBadRequestsAndNeverPersisted(String value) throws Exception {
        User owner = user(true);
        UUID group = create(owner, "", 50);
        String body = body(",\"collectiveGiftThreshold\":" + value);
        mvc.perform(post("/api/groups").header("Authorization", token(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/groups/" + group).header("Authorization", token(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertEquals(1, groups.findByAdminId(owner.getId()).size());
        assertEquals(new BigDecimal("50.00"), groups.findById(group).orElseThrow().getCollectiveGiftThreshold());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "100000000", "50.001"})
    void directServiceCallsValidateThresholds(String value) throws Exception {
        User owner = user(true);
        UUID group = create(owner, "", 50);
        GroupRequest request = json.readValue(body(",\"collectiveGiftThreshold\":" + value), GroupRequest.class);
        assertThrows(ConstraintViolationException.class, () -> service.createGroup(request, owner.getId()));
        assertThrows(ConstraintViolationException.class, () -> service.updateGroup(group, request, owner.getId()));
        assertEquals(new BigDecimal("50.00"), groups.findById(group).orElseThrow().getCollectiveGiftThreshold());
    }

    private UUID create(User owner, String threshold, Number expected) throws Exception {
        var result = mvc.perform(post("/api/groups").header("Authorization", token(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(body(threshold)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.collectiveGiftThreshold").value(expected))
                .andExpect(jsonPath("$.jwtToken").isNotEmpty()).andReturn();
        return UUID.fromString(json.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private void update(User owner, UUID group, String threshold, Number expected) throws Exception {
        mvc.perform(put("/api/groups/" + group).header("Authorization", token(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(body(threshold)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.collectiveGiftThreshold").value(expected));
    }

    private String body(String threshold) {
        return "{\"name\":\"Threshold test\",\"type\":\"noël\"" + threshold + "}";
    }

    private User user(boolean admin) {
        User user = new User();
        user.setEmail(UUID.randomUUID() + "@threshold-test.example");
        user.setAdmin(admin);
        return users.save(user);
    }

    private String token(User user) {
        return "Bearer " + jwt.generateToken(user, List.of());
    }
}
