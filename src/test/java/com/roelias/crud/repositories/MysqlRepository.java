package com.roelias.crud.repositories;

import com.roelias.crud.CRUD;
import com.roelias.crud.entities.TestSimpleUserMysql;
import com.roelias.crud.entities.TestUserMysql;
import org.jdbi.v3.core.Jdbi;

import java.util.List;

public class MysqlRepository extends CRUD<TestUserMysql, Long> {

    public MysqlRepository(Jdbi jdbi) {
        super (jdbi, TestUserMysql.class, Long.class);
    }




    public List<TestSimpleUserMysql> listSimpleUsers(){
        return jdbi.withHandle(handle ->{

            return handle.createQuery("SELECT * FROM complex_entities")
                    .map(getCustomRowMapper(TestSimpleUserMysql.class))
                    .list();


        });
    }

}
