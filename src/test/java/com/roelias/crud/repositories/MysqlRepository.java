package com.roelias.crud.repositories;

import com.roelias.crud.CRUD;
import com.roelias.crud.entities.TestUserMysql;
import org.jdbi.v3.core.Jdbi;

public class MysqlRepository extends CRUD<TestUserMysql, Long> {

    public MysqlRepository(Jdbi jdbi) {
        super (jdbi, TestUserMysql.class, Long.class);
    }

}
