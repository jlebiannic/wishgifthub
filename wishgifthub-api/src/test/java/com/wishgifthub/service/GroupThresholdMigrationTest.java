package com.wishgifthub.service;

import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GroupThresholdMigrationTest {
    @Test
    void migrationDefaultsExistingAndNewGroupsAndPreservesRelatedData() throws Exception {
        try (var connection = DriverManager.getConnection(
                "jdbc:h2:mem:threshold-migration-" + UUID.randomUUID() + ";MODE=PostgreSQL", "sa", "");
             var sql = connection.createStatement()) {
            for (String name : new String[]{"V1__initial_schema.sql", "V2__add_wish_image_price.sql",
                    "V3__increase_varchar_sizes.sql", "V4__add_avatar_id.sql", "V5__add_pseudo.sql",
                    "V6__collective_gifts.sql", "V7__child_profiles.sql"}) {
                migrate(connection, name);
            }
            UUID owner = UUID.randomUUID();
            UUID child = UUID.randomUUID();
            UUID group = UUID.randomUUID();
            UUID individual = UUID.randomUUID();
            UUID collective = UUID.randomUUID();
            UUID invitation = UUID.randomUUID();
            sql.execute("INSERT INTO users(id,email,password_hash,is_admin) VALUES ('" + owner + "','owner@test','hash',TRUE)");
            sql.execute("INSERT INTO users(id,parent_id,pseudo) VALUES ('" + child + "','" + owner + "','Child')");
            sql.execute("INSERT INTO groups(id,name,type,admin_id) VALUES ('" + group + "','Existing','noël','" + owner + "')");
            sql.execute("INSERT INTO user_groups(user_id,group_id) VALUES ('" + child + "','" + group + "')");
            sql.execute("INSERT INTO invitations(id,group_id,email,token) VALUES ('" + invitation + "','" + group + "','invite@test','" + UUID.randomUUID() + "')");
            sql.execute("INSERT INTO wishes(id,user_id,group_id,gift_name,reserved_by) VALUES ('" + individual + "','" + child + "','" + group + "','Reserved','" + owner + "')");
            sql.execute("INSERT INTO wishes(id,user_id,group_id,gift_name,collective,max_participants) VALUES ('" + collective + "','" + child + "','" + group + "','Collective',TRUE,3)");
            sql.execute("INSERT INTO wish_participants(wish_id,user_id) VALUES ('" + collective + "','" + owner + "')");
            migrate(connection, "V8__group_collective_gift_threshold.sql");
            try (var rows = sql.executeQuery("SELECT * FROM groups WHERE id='" + group + "'")) {
                assertTrue(rows.next());
                assertEquals("Existing", rows.getString("name"));
                assertEquals(owner, rows.getObject("admin_id", UUID.class));
                assertEquals(new BigDecimal("50.00"), rows.getBigDecimal("collective_gift_threshold"));
            }
            try (var rows = sql.executeQuery("SELECT reserved_by FROM wishes WHERE id='" + individual + "'")) {
                assertTrue(rows.next());
                assertEquals(owner, rows.getObject(1, UUID.class));
            }
            try (var rows = sql.executeQuery("SELECT collective,max_participants FROM wishes WHERE id='" + collective + "'")) {
                assertTrue(rows.next());
                assertTrue(rows.getBoolean(1));
                assertEquals(3, rows.getInt(2));
            }
            for (String table : new String[]{"user_groups", "invitations", "wish_participants"}) {
                try (var rows = sql.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    rows.next();
                    assertEquals(1, rows.getInt(1));
                }
            }
            try (var rows = sql.executeQuery("SELECT parent_id FROM users WHERE id='" + child + "'")) {
                assertTrue(rows.next());
                assertEquals(owner, rows.getObject(1, UUID.class));
            }
            sql.execute("INSERT INTO groups(name,type,admin_id) VALUES ('New','noël','" + owner + "')");
            try (var rows = sql.executeQuery("SELECT collective_gift_threshold FROM groups WHERE name='New'")) {
                assertTrue(rows.next());
                assertEquals(new BigDecimal("50.00"), rows.getBigDecimal(1));
            }
            sql.execute("UPDATE groups SET collective_gift_threshold=75.25 WHERE id='" + group + "'");
            assertThrows(SQLException.class, () -> sql.execute("UPDATE groups SET collective_gift_threshold=-0.01"));
            assertThrows(SQLException.class, () -> sql.execute("UPDATE groups SET collective_gift_threshold=NULL"));
            assertThrows(SQLException.class, () -> sql.execute("UPDATE groups SET collective_gift_threshold=100000000"));
        }
    }

    private void migrate(java.sql.Connection connection, String name) throws Exception {
        String sql = new ClassPathResource("db/migration/" + name).getContentAsString(StandardCharsets.UTF_8)
                .replace("UUID PRIMARY KEY DEFAULT gen_random_uuid()", "UUID DEFAULT gen_random_uuid() PRIMARY KEY");
        RunScript.execute(connection, new StringReader(sql));
    }
}
