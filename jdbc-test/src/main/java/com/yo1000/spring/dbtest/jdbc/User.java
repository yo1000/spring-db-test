package com.yo1000.spring.dbtest.jdbc;

import org.springframework.data.relational.core.mapping.Table;

public record User(
        Integer id,
        String username,
        String email
) {}
