package com.yo1000.spring.dbtest.jpa;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;

@DataJpaTest
// `@DataJpaTest` uses an embedded database such as H2 by default,
// so set `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)`
// to suppress the default behaviour when using TestContainers or similar.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class JpaUserRepositoryTests {
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

        // Required to setup DB.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("spring.jpa.show-sql", () -> true);
    }

    // When using JPA, the TestEntityManager is used to setup test data.
    @Autowired
    TestEntityManager testEntityManager;

    @Autowired
    JpaUserRepository userRepo;

    @Test
    void testFindAll() {
        User testUser1 = new User();
        testUser1.setId(1000);
        testUser1.setUsername("alice");
        testUser1.setEmail("alice@localhost");

        User testUser2 = new User();
        testUser2.setId(2000);
        testUser2.setUsername("bob");
        testUser2.setEmail("bob@localhost");

        testEntityManager.persist(testUser1);
        testEntityManager.persist(testUser2);

        List<User> actualUsers = userRepo.findAll();

        Assertions.assertThat(actualUsers).isNotNull();
        Assertions.assertThat(actualUsers.size()).isEqualTo(2);

        Assertions.assertThat(actualUsers.get(0).getId()).isEqualTo(1000);
        Assertions.assertThat(actualUsers.get(0).getUsername()).isEqualTo("alice");
        Assertions.assertThat(actualUsers.get(0).getEmail()).isEqualTo("alice@localhost");

        Assertions.assertThat(actualUsers.get(1).getId()).isEqualTo(2000);
        Assertions.assertThat(actualUsers.get(1).getUsername()).isEqualTo("bob");
        Assertions.assertThat(actualUsers.get(1).getEmail()).isEqualTo("bob@localhost");
    }

    @Test
    void testFindById() {
        User testUser = new User();
        testUser.setId(1000);
        testUser.setUsername("alice");
        testUser.setEmail("alice@localhost");

        testEntityManager.persist(testUser);

        Optional<User> actualUsers = userRepo.findById(1000);

        Assertions.assertThat(actualUsers).isNotNull();
        Assertions.assertThat(actualUsers.isPresent()).isTrue();

        actualUsers.ifPresent(user -> {
            Assertions.assertThat(user.getId()).isEqualTo(1000);
            Assertions.assertThat(user.getUsername()).isEqualTo("alice");
            Assertions.assertThat(user.getEmail()).isEqualTo("alice@localhost");
        });
    }

    @Test
    void testSave() {
        User testUser = new User();
        testUser.setId(1000);
        testUser.setUsername("alice");
        testUser.setEmail("alice@localhost");

        User savedUser = userRepo.save(testUser);

        Assertions.assertThat(savedUser).isNotNull();

        Assertions.assertThat(savedUser.getId()).isEqualTo(1000);
        Assertions.assertThat(savedUser.getUsername()).isEqualTo("alice");
        Assertions.assertThat(savedUser.getEmail()).isEqualTo("alice@localhost");

        User actualUser = testEntityManager.find(User.class, 1000);

        Assertions.assertThat(actualUser).isNotNull();

        Assertions.assertThat(actualUser.getId()).isEqualTo(1000);
        Assertions.assertThat(actualUser.getUsername()).isEqualTo("alice");
        Assertions.assertThat(actualUser.getEmail()).isEqualTo("alice@localhost");

    }
}
