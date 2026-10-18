package com.wishgifthub.service;

import org.junit.jupiter.api.Test;
import org.h2.tools.RunScript;
import org.springframework.core.io.ClassPathResource;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CollectiveGiftMigrationTest {
    @Test
    void upgradePreservesExistingIndividualWishesAndReservations() throws Exception {
        String url = "jdbc:h2:mem:migration-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(url, "sa", "")) {
            for (String migration : new String[]{"V1__initial_schema.sql", "V2__add_wish_image_price.sql",
                    "V3__increase_varchar_sizes.sql", "V4__add_avatar_id.sql", "V5__add_pseudo.sql"}) {
                migrate(connection, migration);
            }
        }
        UUID owner = UUID.randomUUID();
        UUID reserver = UUID.randomUUID();
        UUID group = UUID.randomUUID();
        UUID reserved = UUID.randomUUID();
        UUID available = UUID.randomUUID();
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("INSERT INTO users(id,email) VALUES ('" + owner + "','owner@test'),('" + reserver + "','reserver@test')");
            statement.execute("INSERT INTO groups(id,name,type,admin_id) VALUES ('" + group + "','Test','BIRTHDAY','" + owner + "')");
            statement.execute("INSERT INTO wishes(id,user_id,group_id,gift_name,reserved_by) VALUES ('"
                    + reserved + "','" + owner + "','" + group + "','Reserved','" + reserver + "')");
            statement.execute("INSERT INTO wishes(id,user_id,group_id,gift_name) VALUES ('"
                    + available + "','" + owner + "','" + group + "','Available')");
        }
        try (var connection = DriverManager.getConnection(url, "sa", "")) {
            migrate(connection, "V6__collective_gifts.sql");
        }
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("SELECT * FROM wishes ORDER BY gift_name")) {
                assertTrue(rows.next());
                assertEquals(available, rows.getObject("id", UUID.class));
                assertFalse(rows.getBoolean("collective"));
                assertEquals(1, rows.getInt("max_participants"));
                assertNull(rows.getObject("reserved_by"));
                assertTrue(rows.next());
                assertEquals(reserved, rows.getObject("id", UUID.class));
                assertEquals(reserver, rows.getObject("reserved_by", UUID.class));
                assertFalse(rows.getBoolean("collective"));
                assertEquals(1, rows.getInt("max_participants"));
                assertFalse(rows.next());
            }
            try (var rows = statement.executeQuery("SELECT count(*) FROM wish_participants")) {
                rows.next();
                assertEquals(0, rows.getInt(1));
            }
        }
    }

    private void migrate(Connection connection, String name) throws Exception {
        String sql = new ClassPathResource("db/migration/" + name).getContentAsString(StandardCharsets.UTF_8);
        // H2 requires DEFAULT before PRIMARY KEY; production migrations remain unchanged.
        sql = sql.replace("UUID PRIMARY KEY DEFAULT gen_random_uuid()", "UUID DEFAULT gen_random_uuid() PRIMARY KEY");
        RunScript.execute(connection, new StringReader(sql));
    }
}
