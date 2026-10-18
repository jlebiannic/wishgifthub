package com.wishgifthub.service;

import com.wishgifthub.entity.Group;
import com.wishgifthub.entity.User;
import com.wishgifthub.entity.UserGroup;
import com.wishgifthub.exception.AccessDeniedException;
import com.wishgifthub.exception.BusinessRuleException;
import com.wishgifthub.openapi.model.WishRequest;
import com.wishgifthub.openapi.model.WishResponse;
import com.wishgifthub.repository.GroupRepository;
import com.wishgifthub.repository.UserGroupRepository;
import com.wishgifthub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:collective;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(WishService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CollectiveGiftTest {
    @Autowired WishService service;
    @Autowired UserRepository users;
    @Autowired GroupRepository groups;
    @Autowired UserGroupRepository memberships;
    private User owner;
    private User first;
    private User second;
    private User third;
    private Group group;

    @BeforeEach
    void setup() {
        owner = user();
        first = user();
        second = user();
        third = user();
        group = new Group();
        group.setName("Collective test");
        group.setType("BIRTHDAY");
        group.setAdmin(owner);
        group = groups.save(group);
        for (User user : List.of(owner, first, second, third)) {
            UserGroup membership = new UserGroup();
            membership.setUser(user);
            membership.setGroup(group);
            memberships.save(membership);
        }
    }

    private User user() {
        User user = new User();
        user.setEmail(UUID.randomUUID() + "@test.example");
        return users.save(user);
    }

    private WishRequest request(boolean collective, int capacity) {
        return new WishRequest().giftName("Gift").collective(collective).maxParticipants(capacity);
    }

    private UUID create(int capacity) {
        return service.createWish(group.getId(), request(true, capacity), owner.getId()).getId();
    }

    private WishResponse reserve(UUID wish, User user) {
        return service.reserveWish(group.getId(), wish, user.getId());
    }

    @Test
    void createsDefaultsAndValidatesCapacity() {
        WishResponse individual = service.createWish(group.getId(), new WishRequest().giftName("Old client"), owner.getId());
        assertFalse(individual.getCollective());
        assertEquals(1, individual.getMaxParticipants());
        WishResponse collective = service.createWish(group.getId(),
                new WishRequest().giftName("Shared").collective(true), owner.getId());
        assertTrue(collective.getCollective());
        assertEquals(2, collective.getMaxParticipants());
        assertNull(collective.getParticipantCount());
        assertThrows(BusinessRuleException.class, () -> create(1));
        assertThrows(BusinessRuleException.class, () -> create(0));
    }

    @Test
    void distinctParticipantsFillCancelAndReopen() {
        UUID id = create(2);
        assertThrows(BusinessRuleException.class, () -> reserve(id, owner));
        assertEquals(1, reserve(id, first).getParticipantCount());
        assertThrows(BusinessRuleException.class, () -> reserve(id, first));
        WishResponse full = reserve(id, second);
        assertEquals(2, full.getParticipantCount());
        assertTrue(full.getFull());
        assertTrue(full.getParticipating());
        assertNull(full.getReservedBy());
        assertThrows(BusinessRuleException.class, () -> reserve(id, third));
        assertThrows(BusinessRuleException.class, () -> service.unreserveWish(group.getId(), id, third.getId()));
        WishResponse reopened = service.unreserveWish(group.getId(), id, first.getId());
        assertEquals(1, reopened.getParticipantCount());
        assertFalse(reopened.getFull());
        assertFalse(reopened.getParticipating());
        assertTrue(reserve(id, third).getFull());
    }

    @Test
    void editsRespectActiveCountAndOwnership() {
        UUID id = create(3);
        reserve(id, first);
        reserve(id, second);
        reserve(id, third);
        assertThrows(AccessDeniedException.class, () ->
                service.updateWish(group.getId(), id, request(true, 5), first.getId()));
        assertThrows(BusinessRuleException.class, () ->
                service.updateWish(group.getId(), id, request(true, 2), owner.getId()));
        assertThrows(BusinessRuleException.class, () ->
                service.updateWish(group.getId(), id, request(false, 2), owner.getId()));
        service.updateWish(group.getId(), id, request(true, 4), owner.getId());
        service.unreserveWish(group.getId(), id, third.getId());
        service.updateWish(group.getId(), id, request(true, 2), owner.getId());
        service.unreserveWish(group.getId(), id, second.getId());
        service.updateWish(group.getId(), id, request(false, 2), owner.getId());
        WishResponse individual = service.getWishesByGroup(group.getId(), first.getId()).getFirst();
        assertFalse(individual.getCollective());
        assertEquals(first.getId(), individual.getReservedBy());
        service.unreserveWish(group.getId(), id, first.getId());
        service.updateWish(group.getId(), id, request(true, 2), owner.getId());
        assertEquals(1, reserve(id, second).getParticipantCount());
    }

    @Test
    void recipientCannotSeeCollectiveActivityFromAnyReadOrWriteResponse() {
        UUID id = create(2);
        reserve(id, first);
        reserve(id, second);
        for (WishResponse response : List.of(
                service.getWishesByGroup(group.getId(), owner.getId()).getFirst(),
                service.getWishesByUserInGroup(group.getId(), owner.getId(), owner.getId()).getFirst(),
                service.updateWish(group.getId(), id, request(true, 3), owner.getId()))) {
            assertNull(response.getParticipantCount());
            assertNull(response.getParticipating());
            assertNull(response.getFull());
            assertNull(response.getReservedBy());
        }
        WishResponse viewer = service.getWishesByUserInGroup(group.getId(), owner.getId(), third.getId()).getFirst();
        assertEquals(2, viewer.getParticipantCount());
        assertFalse(viewer.getParticipating());
    }

    @Test
    void individualReservationSemanticsRemainCompatible() {
        UUID id = service.createWish(group.getId(), new WishRequest().giftName("Individual"), owner.getId()).getId();
        assertEquals(first.getId(), reserve(id, first).getReservedBy());
        assertThrows(BusinessRuleException.class, () -> reserve(id, second));
        assertThrows(BusinessRuleException.class, () ->
                service.updateWish(group.getId(), id, request(false, 2), owner.getId()));
        assertThrows(BusinessRuleException.class, () -> service.unreserveWish(group.getId(), id, second.getId()));
        assertNull(service.unreserveWish(group.getId(), id, first.getId()).getReservedBy());
        assertEquals(second.getId(), reserve(id, second).getReservedBy());
    }

    @Test
    void concurrentRequestsForLastSlotCannotOverbook() throws Exception {
        UUID id = create(2);
        reserve(id, first);
        assertEquals(1, concurrentReservations(id, second, third));
        assertEquals(2, service.getWishesByGroup(group.getId(), first.getId()).getFirst().getParticipantCount());
    }

    @Test
    void concurrentDuplicateParticipationSucceedsOnlyOnce() throws Exception {
        UUID id = create(3);
        assertEquals(1, concurrentReservations(id, first, first));
        assertEquals(1, service.getWishesByGroup(group.getId(), first.getId()).getFirst().getParticipantCount());
    }

    private int concurrentReservations(UUID id, User... participants) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (User user : participants) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    try {
                        reserve(id, user);
                        return true;
                    } catch (BusinessRuleException full) {
                        return false;
                    }
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            int successes = 0;
            for (var result : results) if (result.get(15, TimeUnit.SECONDS)) successes++;
            return successes;
        }
    }

    @Test
    void wrongGroupCannotMutateReservation() {
        UUID id = create(2);
        assertThrows(BusinessRuleException.class, () -> service.reserveWish(UUID.randomUUID(), id, first.getId()));
        assertThrows(BusinessRuleException.class, () -> service.unreserveWish(UUID.randomUUID(), id, first.getId()));
        assertThrows(BusinessRuleException.class, () -> service.updateWish(UUID.randomUUID(), id, request(true, 3), owner.getId()));
    }
}
