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

class ChildMigrationTest {
    @Test
    void migrationPreservesAccountsWishesAndReservationsAndCascadesChildDeletion() throws Exception {
        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:child-migration-" + UUID.randomUUID() + ";MODE=PostgreSQL", "sa", "");
             var sql = connection.createStatement()) {
            for (String name : new String[]{"V1__initial_schema.sql", "V2__add_wish_image_price.sql",
                    "V3__increase_varchar_sizes.sql", "V4__add_avatar_id.sql", "V5__add_pseudo.sql",
                    "V6__collective_gifts.sql"}) {
                migrate(connection, name);
            }
            UUID parent = UUID.randomUUID();
            UUID group = UUID.randomUUID();
            UUID oldWish = UUID.randomUUID();
            UUID child = UUID.randomUUID();
            UUID childWish = UUID.randomUUID();
            sql.execute("INSERT INTO users(id,email,password_hash,is_admin,pseudo) VALUES ('" + parent + "','parent@test','hash',TRUE,'Parent')");
            sql.execute("INSERT INTO groups(id,name,type,admin_id) VALUES ('" + group + "','Noel','noel','" + parent + "')");
            sql.execute("INSERT INTO wishes(id,user_id,group_id,gift_name,reserved_by) VALUES ('" + oldWish + "','" + parent + "','" + group + "','Existing','" + parent + "')");
            migrate(connection, "V7__child_profiles.sql");
            try (var rows = sql.executeQuery("SELECT * FROM users WHERE id='" + parent + "'")) {
                assertTrue(rows.next());
                assertEquals("parent@test", rows.getString("email"));
                assertEquals("hash", rows.getString("password_hash"));
                assertTrue(rows.getBoolean("is_admin"));
                assertNull(rows.getObject("parent_id"));
                assertEquals("Parent", rows.getString("pseudo"));
            }
            sql.execute("INSERT INTO users(id,parent_id,pseudo) VALUES ('" + child + "','" + parent + "','Enfant')");
            sql.execute("INSERT INTO user_groups(user_id,group_id) VALUES ('" + child + "','" + group + "')");
            sql.execute("INSERT INTO wishes(id,user_id,group_id,gift_name,collective,max_participants) VALUES ('"
                    + childWish + "','" + child + "','" + group + "','Child',TRUE,2)");
            sql.execute("INSERT INTO wish_participants(wish_id,user_id) VALUES ('" + childWish + "','" + parent + "')");
            assertThrows(SQLException.class, () -> sql.execute("UPDATE users SET is_admin=TRUE WHERE id='" + child + "'"));
            assertThrows(SQLException.class, () -> sql.execute("UPDATE users SET password_hash='hash' WHERE id='" + child + "'"));
            assertThrows(SQLException.class, () -> sql.execute("UPDATE users SET email='child@test' WHERE id='" + child + "'"));
            assertThrows(SQLException.class, () -> sql.execute("UPDATE users SET parent_id=id WHERE id='" + child + "'"));
            assertThrows(SQLException.class, () -> sql.execute("INSERT INTO users(id) VALUES ('" + UUID.randomUUID() + "')"));
            sql.execute("DELETE FROM users WHERE id='" + child + "'");
            for (String table : new String[]{"user_groups", "wish_participants"}) {
                try (var rows = sql.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    rows.next();
                    assertEquals(0, rows.getInt(1));
                }
            }
            try (var rows = sql.executeQuery("SELECT * FROM wishes")) {
                assertTrue(rows.next());
                assertEquals(oldWish, rows.getObject("id", UUID.class));
                assertEquals(parent, rows.getObject("reserved_by", UUID.class));
                assertFalse(rows.getBoolean("collective"));
                assertFalse(rows.next());
            }
        }
    }

    private void migrate(java.sql.Connection connection, String name) throws Exception {
        String sql = new ClassPathResource("db/migration/" + name).getContentAsString(StandardCharsets.UTF_8)
                .replace("UUID PRIMARY KEY DEFAULT gen_random_uuid()", "UUID DEFAULT gen_random_uuid() PRIMARY KEY");
        RunScript.execute(connection, new StringReader(sql));
    }
}
