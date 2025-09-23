package com.roelias.crud.repositories;

import com.roelias.crud.CRUD;

import com.roelias.crud.entities.TestSimpleUserPostgresql;
import com.roelias.crud.entities.TestUserPostgresql;
import org.jdbi.v3.core.Jdbi;

import java.util.List;

public class PostgresqlRepository extends CRUD<TestUserPostgresql, Long> {

    public PostgresqlRepository(Jdbi jdbi) {
        super(jdbi, TestUserPostgresql.class, Long.class);
    }

    public List<TestSimpleUserPostgresql> listSimpleUsers() {
        return jdbi.withHandle(handle -> {

            return handle.createQuery("SELECT * FROM " + getTableName())
                    .map(getCustomRowMapper(TestSimpleUserPostgresql.class))
                    .list();
        });
    }
}
