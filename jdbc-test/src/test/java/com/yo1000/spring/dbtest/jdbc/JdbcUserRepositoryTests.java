package com.yo1000.spring.dbtest.jdbc;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;

@DataJdbcTest
// No required configure since Spring Boot 3.4.0
// @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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
            // When SQL code breaks a line in the middle of a statement,
            // it follows one of the rules.
            // - Option 1: Add a semicolon `;` at the end of the SQL statement.
            // - Option 2: Add a backslash `\` at the end of the line where the SQL statement is broken.
            """
            INSERT INTO "user" (id, username, email)
            VALUES (1000, 'alice', 'alice@localhost');
            """, """
            INSERT INTO "user" (id, username, email) \
            VALUES (2000, 'bob', 'bob@localhost')
            """},
            // Option: When using multiple data sources, Can switch target of SQL by configure Bean id.
            config = @SqlConfig(dataSource = "dataSource")
    )
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

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, delimiterString = "|", textBlock = """
            [id] | [username] | [email]
            1000 | 'alice'    | 'alice@localhost'
            2000 | 'bob'      | 'bob@localhost'
            """)
    @Sql(statements = {
            // When SQL code breaks a line in the middle of a statement,
            // it follows one of the rules.
            // - Option 1: Add a semicolon `;` at the end of the SQL statement.
            // - Option 2: Add a backslash `\` at the end of the line where the SQL statement is broken.
            """
            INSERT INTO "user" (id, username, email)
            VALUES (1000, 'alice', 'alice@localhost');
            """, """
            INSERT INTO "user" (id, username, email) \
            VALUES (2000, 'bob', 'bob@localhost')
            """}
    )
    void testFindById(Integer id, String username, String email) {
        JdbcUserRepository userRepo = new JdbcUserRepository(jdbcClient);

        Optional<User> actualUsers = userRepo.findById(id);

        Assertions.assertThat(actualUsers).isNotNull();
        Assertions.assertThat(actualUsers.isPresent()).isTrue();

        actualUsers.ifPresent(user -> {
            Assertions.assertThat(user.id()).isEqualTo(id);
            Assertions.assertThat(user.username()).isEqualTo(username);
            Assertions.assertThat(user.email()).isEqualTo(email);
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
