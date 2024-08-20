package com.yo1000.spring.dbtest.jdbc;


import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcUserRepository {
    private final JdbcClient jdbcClient;

    public JdbcUserRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<User> findAll() {
        return jdbcClient
                .sql("""
                    SELECT
                        id,
                        username,
                        email
                    FROM
                        "user"
                """)
                .query(DataClassRowMapper.newInstance(User.class))
                .list();
    }

    public Optional<User> findById(Integer id) {
        return jdbcClient
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
                .param("id", id)
                .query(DataClassRowMapper.newInstance(User.class))
                .optional();
    }

    public User save(User user) {
        jdbcClient
                .sql("""
                    INSERT INTO "user" (
                            id,
                            username,
                            email
                    ) VALUES (
                            :id,
                            :username,
                            :email
                    )
                """)
                .param("id", user.id())
                .param("username", user.username())
                .param("email", user.email())
                .update();

        return findById(user.id()).orElseThrow();
    }
}
