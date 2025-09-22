package com.roelias.crud.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roelias.crud.entities.*;
import com.roelias.crud.repositories.PostgresqlRepository;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class TestPostgresql {


    private static ObjectMapper mapper = new ObjectMapper();
    private static PostgresqlRepository repository;
    @BeforeAll
    static void setupDatabase(){
        Jdbi jdbi = Jdbi.create("jdbc:postgresql://localhost:5432/test_crud", "postgres", "admin");
        repository = new PostgresqlRepository(jdbi);
    }



    @Test
    void testCRUDInsert(){

        TestUserPostgresql entity = new TestUserPostgresql();
        entity.setName("Test Entity");
        entity.setPreferences(new UserPreferences("dark", true, Map.of("lang", "en")));
        entity.setManualUuid("550e8400-e29b-41d4-a716-446655440000");
        entity.setMetadata(Map.of("version", "1.0", "author", "john"));

        entity.setRawJson(mapper.createObjectNode().put("name", "Test User"));
        entity.setTags(List.of("important", "test", "demo"));
        entity.setStatus(Status.ACTIVE);
        entity.setPriority(Priority.HIGH);
        entity.setDepartment(Department.ENGINEERING);

        entity.setDocument(new byte[]{0x01, 0x02, 0x03, 0x04});
        entity.setCategories(new String[]{"cat1", "cat2", "cat3"});
        repository.save(entity);


    }


    @Test
    void testCRUDFindAll(){

        List<TestUserPostgresql> users = repository.findAll();
        users.forEach(u -> {
            System.out.println("ID: " + u.getId());
            System.out.println("Name: " + u.getName());
            System.out.println("UUID: " + u.getUuid());
            System.out.println("Manual UUID: " + u.getManualUuid());
            System.out.println("Preferences: " + (u.getPreferences() != null ? u.getPreferences().getTheme() : "null"));
            System.out.println("Metadata: " + u.getMetadata());
            System.out.println("Raw JSON: " + u.getRawJson());
            System.out.println("Tags: " + u.getTags());
            System.out.println("Categories: " + (u.getCategories() != null ? String.join(", ", u.getCategories()) : "null"));
            System.out.println("Status: " + u.getStatus());
            System.out.println("Priority: " + u.getPriority());
            System.out.println("Department: " + u.getDepartment());
            System.out.println("Document: " + (u.getDocument() != null ? u.getDocument().length + " bytes" : "null"));
            System.out.println("Created At: " + u.getCreatedAt());
            System.out.println("Updated At: " + u.getUpdatedAt());
            System.out.println("---------------------------");
        });




    }

}
