package com.roelias.crud.repositories;

import com.roelias.crud.CRUD;
import com.roelias.crud.entities.TestUserPostgresql;
import org.jdbi.v3.core.Jdbi;

public class PostgresqlRepository extends CRUD<TestUserPostgresql, Long> {

    public PostgresqlRepository(Jdbi jdbi) {
        super(jdbi, TestUserPostgresql.class, Long.class);
    }

}
