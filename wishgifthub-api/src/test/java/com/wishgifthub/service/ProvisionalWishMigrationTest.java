package com.wishgifthub.service;

import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProvisionalWishMigrationTest {
    @Test
    void migrationKeepsAllExistingWishesPublishedAndPreservesRelatedData() throws Exception {
        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:provisional-migration-" + UUID.randomUUID() + ";MODE=PostgreSQL", "sa", "");
             var sql = connection.createStatement()) {
            for (String name : new String[]{"V1__initial_schema.sql", "V2__add_wish_image_price.sql",
                    "V3__increase_varchar_sizes.sql", "V4__add_avatar_id.sql", "V5__add_pseudo.sql",
                    "V6__collective_gifts.sql", "V7__child_profiles.sql", "V8__group_collective_gift_threshold.sql"}) {
                migrate(connection, name);
            }
            UUID owner = UUID.randomUUID();
            UUID child = UUID.randomUUID();
            UUID group = UUID.randomUUID();
            UUID individual = UUID.randomUUID();
            UUID collective = UUID.randomUUID();
            sql.execute("INSERT INTO users(id,email,password_hash,is_admin) VALUES ('" + owner + "','owner@test','hash',TRUE)");
            sql.execute("INSERT INTO users(id,parent_id,pseudo) VALUES ('" + child + "','" + owner + "','Child')");
            sql.execute("INSERT INTO groups(id,name,type,admin_id,collective_gift_threshold) VALUES ('" + group + "','Existing','noël','" + owner + "',75.25)");
            sql.execute("INSERT INTO user_groups(user_id,group_id) VALUES ('" + child + "','" + group + "')");
            sql.execute("INSERT INTO invitations(group_id,email,token) VALUES ('" + group + "','invite@test','" + UUID.randomUUID() + "')");
            sql.execute("INSERT INTO wishes(id,user_id,group_id,gift_name,reserved_by,description,url,image_url,price) VALUES ('"
                    + individual + "','" + child + "','" + group + "','Reserved','" + owner
                    + "','Description','https://example.com','https://example.com/image','12 EUR')");
            sql.execute("INSERT INTO wishes(id,user_id,group_id,gift_name,collective,max_participants) VALUES ('" + collective + "','" + child + "','" + group + "','Collective',TRUE,3)");
            sql.execute("INSERT INTO wish_participants(wish_id,user_id) VALUES ('" + collective + "','" + owner + "')");
            migrate(connection, "V9__provisional_wishes.sql");
            try (var rows = sql.executeQuery("SELECT * FROM wishes WHERE id='" + individual + "'")) {
                assertTrue(rows.next());
                assertFalse(rows.getBoolean("provisional"));
                assertEquals(owner, rows.getObject("reserved_by", UUID.class));
                assertEquals(child, rows.getObject("user_id", UUID.class));
                assertEquals(group, rows.getObject("group_id", UUID.class));
                assertEquals("Reserved", rows.getString("gift_name"));
                assertEquals("Description", rows.getString("description"));
                assertEquals("https://example.com", rows.getString("url"));
                assertEquals("https://example.com/image", rows.getString("image_url"));
                assertEquals("12 EUR", rows.getString("price"));
                assertNotNull(rows.getObject("created_at"));
            }
            try (var rows = sql.executeQuery("SELECT * FROM wishes WHERE id='" + collective + "'")) {
                assertTrue(rows.next());
                assertFalse(rows.getBoolean("provisional"));
                assertTrue(rows.getBoolean("collective"));
                assertEquals(3, rows.getInt("max_participants"));
            }
            try (var rows = sql.executeQuery("SELECT user_id FROM wish_participants WHERE wish_id='" + collective + "'")) {
                assertTrue(rows.next());
                assertEquals(owner, rows.getObject(1, UUID.class));
            }
            for (String table : new String[]{"user_groups", "invitations", "wish_participants", "groups"}) {
                try (var rows = sql.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    rows.next();
                    assertEquals(1, rows.getInt(1));
                }
            }
            try (var rows = sql.executeQuery("SELECT parent_id FROM users WHERE id='" + child + "'")) {
                assertTrue(rows.next());
                assertEquals(owner, rows.getObject(1, UUID.class));
            }
            try (var rows = sql.executeQuery("SELECT collective_gift_threshold FROM groups WHERE id='" + group + "'")) {
                assertTrue(rows.next());
                assertEquals("75.25", rows.getBigDecimal(1).toPlainString());
            }
            sql.execute("INSERT INTO wishes(user_id,group_id,gift_name) VALUES ('" + owner + "','" + group + "','New')");
            try (var rows = sql.executeQuery("SELECT provisional FROM wishes WHERE gift_name='New'")) {
                assertTrue(rows.next());
                assertFalse(rows.getBoolean(1));
            }
            assertThrows(SQLException.class, () -> sql.execute("UPDATE wishes SET provisional=NULL"));
            sql.execute("UPDATE wishes SET provisional=TRUE WHERE gift_name='New'");
        }
    }

    private void migrate(java.sql.Connection connection, String name) throws Exception {
        String sql = new ClassPathResource("db/migration/" + name).getContentAsString(StandardCharsets.UTF_8)
                .replace("UUID PRIMARY KEY DEFAULT gen_random_uuid()", "UUID DEFAULT gen_random_uuid() PRIMARY KEY");
        RunScript.execute(connection, new StringReader(sql));
    }
}
