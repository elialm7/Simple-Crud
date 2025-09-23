package com.roelias.crud.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roelias.crud.entities.*;
import com.roelias.crud.repositories.PostgresqlRepository;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class TestPostgresql {


    private static ObjectMapper mapper = new ObjectMapper();
    private static PostgresqlRepository repository;
    @BeforeAll
    static void setupDatabase(){
        Jdbi jdbi = Jdbi.create("jdbc:postgresql://localhost:5432/test_crud", "postgres", "admin");
        repository = new PostgresqlRepository(jdbi);
    }


    public List<TestUserPostgresql> generateBatch(int count) {
        String[] names = {"Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace"};
        String[] themes = {"dark", "light", "blue", "green"};
        String[] tagsPool = {"important", "test", "demo", "urgent", "review"};
        Department[] departments = Department.values();
        Status[] statuses = Status.values();
        Priority[] priorities = Priority.values();

        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> {
                    TestUserPostgresql entity = new TestUserPostgresql();

                    // Random name
                    String name = names[ThreadLocalRandom.current().nextInt(names.length)] + " " + i;
                    entity.setName(name);

                    // Random preferences
                    String theme = themes[ThreadLocalRandom.current().nextInt(themes.length)];
                    boolean notifications = ThreadLocalRandom.current().nextBoolean();
                    entity.setPreferences(new UserPreferences(theme, notifications, Map.of("lang", "en")));

                    // Random manual UUID
                    entity.setManualUuid(java.util.UUID.randomUUID().toString());

                    // Random metadata
                    entity.setMetadata(Map.of(
                            "version", "1." + ThreadLocalRandom.current().nextInt(0, 10),
                            "author", name
                    ));

                    // Random raw JSON
                    entity.setRawJson(mapper.createObjectNode().put("name", name));

                    // Random tags (subset of tagsPool)
                    int tagCount = ThreadLocalRandom.current().nextInt(1, tagsPool.length + 1);
                    List<String> tags = java.util.stream.IntStream.range(0, tagCount)
                            .mapToObj(idx -> tagsPool[ThreadLocalRandom.current().nextInt(tagsPool.length)])
                            .distinct()
                            .toList();
                    entity.setTags(tags);

                    // Random enums
                    entity.setStatus(statuses[ThreadLocalRandom.current().nextInt(statuses.length)]);
                    entity.setPriority(priorities[ThreadLocalRandom.current().nextInt(priorities.length)]);
                    entity.setDepartment(departments[ThreadLocalRandom.current().nextInt(departments.length)]);

                    // Random document bytes
                    byte[] doc = new byte[ThreadLocalRandom.current().nextInt(2, 6)];
                    ThreadLocalRandom.current().nextBytes(doc);
                    entity.setDocument(doc);

                    // Random categories
                    int catCount = ThreadLocalRandom.current().nextInt(1, 4);
                    String[] categories = java.util.stream.IntStream.range(0, catCount)
                            .mapToObj(idx -> "cat" + ThreadLocalRandom.current().nextInt(1, 100))
                            .toArray(String[]::new);
                    entity.setCategories(categories);

                    return entity;
                })
                .toList();
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

    @Test
    void testCRUDUpdate() {
        List<TestUserPostgresql> users = repository.findAll();
        if (!users.isEmpty()) {
            TestUserPostgresql user = users.get(1);
            user.setName("Updated Name");
            user.setPreferences(new UserPreferences("white", true, Map.of("lang", "en")));
            user.setActive(false);
            user.setAttempts(5);
            repository.update(user);
        }
    }


    @Test
    void testCRUDDelete() {
        List<TestUserPostgresql> users = repository.findAll();
        if (!users.isEmpty()) {
            TestUserPostgresql user = users.getLast();
            repository.deleteById(user.getId());
        }
    }

    @Test
    void testFindById() {
        List<TestUserPostgresql> users = repository.findAll();
        if (!users.isEmpty()) {
            TestUserPostgresql user = users.getLast();
            TestUserPostgresql fetched = repository.findById(user.getId()).get();
            System.out.println("Fetched User Name: " + fetched.getName());
        }
    }

    @Test
    void testBatchInsert() {
        List<TestUserPostgresql> batch = generateBatch(50);
        repository.saveAll(batch);
    }

    @Test
    void testCount() {
        long count = repository.count();
        System.out.println("Total records: " + count);
    }
    @Test
    void testDeleteAll() {
        repository.deleteAll();
    }

    @Test
    void testExistsById() {
        List<TestUserPostgresql> users = repository.findAll();
        if (!users.isEmpty()) {
            TestUserPostgresql user = users.getLast();
            boolean exists = repository.existsById(user.getId());
            System.out.println("User with ID " + user.getId() + " exists: " + exists);
        }
    }

    @Test
    void testCreationTableTemplate(){
        System.out.println(repository.getTableCreationTemplate());
    }

}
