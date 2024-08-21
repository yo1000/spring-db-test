package com.yo1000.spring.dbtest.jdbc;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;

@DataJdbcTest
// `@DataJdbcTest` uses an embedded database such as H2 by default,
// so set `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)`
// to suppress the default behaviour when using TestContainers or similar.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class JdbcUserRepositoryTests {
    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(DockerImageName
            .parse("postgres"));

    @BeforeAll
    static void startContainers() {
        postgresContainer.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgresContainer.getJdbcUrl());
        registry.add("spring.datasource.username", () -> postgresContainer.getUsername());
        registry.add("spring.datasource.password", () -> postgresContainer.getPassword());

        // Required to apply `schema.sql`.
        registry.add("spring.sql.init.mode", () -> "always");
    }

    @Autowired
    JdbcClient jdbcClient;

    @Test
    // When using JDBC, the Sql annotation is used to setup test data.
    @Sql(statements = {
            // When SQL wraps in the middle of a statement,
            // a backslash `\` is appended at the end of the line.
            """
            INSERT INTO "user" (id, username, email) \
            VALUES (1000, 'alice', 'alice@localhost')
            """, """
            INSERT INTO "user" (id, username, email) \
            VALUES (2000, 'bob', 'bob@localhost')
            """
    })
    void testFindAll() {
        JdbcUserRepository userRepo = new JdbcUserRepository(jdbcClient);

        List<User> actualUsers = userRepo.findAll();

        Assertions.assertThat(actualUsers).isNotNull();
        Assertions.assertThat(actualUsers.size()).isEqualTo(2);

        Assertions.assertThat(actualUsers.get(0).id()).isEqualTo(1000);
        Assertions.assertThat(actualUsers.get(0).username()).isEqualTo("alice");
        Assertions.assertThat(actualUsers.get(0).email()).isEqualTo("alice@localhost");

        Assertions.assertThat(actualUsers.get(1).id()).isEqualTo(2000);
        Assertions.assertThat(actualUsers.get(1).username()).isEqualTo("bob");
        Assertions.assertThat(actualUsers.get(1).email()).isEqualTo("bob@localhost");
    }

    @Test
    @Sql(statements = {
            // When SQL wraps in the middle of a statement,
            // a backslash `\` is appended at the end of the line.
            """
            INSERT INTO "user" (id, username, email) \
            VALUES (1000, 'alice', 'alice@localhost')
            """
    })
    void testFindById() {
        JdbcUserRepository userRepo = new JdbcUserRepository(jdbcClient);

        Optional<User> actualUsers = userRepo.findById(1000);

        Assertions.assertThat(actualUsers).isNotNull();
        Assertions.assertThat(actualUsers.isPresent()).isTrue();

        actualUsers.ifPresent(user -> {
            Assertions.assertThat(user.id()).isEqualTo(1000);
            Assertions.assertThat(user.username()).isEqualTo("alice");
            Assertions.assertThat(user.email()).isEqualTo("alice@localhost");
        });
    }

    @Test
    void testSave() {
        JdbcUserRepository userRepo = new JdbcUserRepository(jdbcClient);

        User savedUser = userRepo.save(new User(1000, "alice", "alice@localhost"));

        Assertions.assertThat(savedUser).isNotNull();

        Assertions.assertThat(savedUser.id()).isEqualTo(1000);
        Assertions.assertThat(savedUser.username()).isEqualTo("alice");
        Assertions.assertThat(savedUser.email()).isEqualTo("alice@localhost");

        User actualUser = jdbcClient
                .sql("""
                    SELECT
                        id,
                        username,
                        email
                    FROM
                        "user"
                    WHERE
                        id = :id
                """)
                .param("id", 1000)
                .query(DataClassRowMapper.newInstance(User.class))
                .single();

        Assertions.assertThat(actualUser).isNotNull();

        Assertions.assertThat(actualUser.id()).isEqualTo(1000);
        Assertions.assertThat(actualUser.username()).isEqualTo("alice");
        Assertions.assertThat(actualUser.email()).isEqualTo("alice@localhost");
    }
}
