package com.roelias.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roelias.crud.SqlBuilder;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSqlBuilderMysql {

    private static Jdbi jdbi;

    @BeforeAll
    static void setupDatabase() {
        jdbi = Jdbi.create("jdbc:mysql://localhost:3306/test_crud", "root", "admin");

    }

    @Test
    public void testSimpleSelect() {
        SqlBuilder query = SqlBuilder
                .select("id", "name", "email")
                .from("users")
                .where("active = :active")
                .param("active", true)
                .orderBy("name");

        String expectedSql = "SELECT id, name, email FROM users WHERE active = :active ORDER BY name ASC";
        assertEquals(expectedSql, query.build());

        Map<String, Object> params = query.getParameters();
        assertEquals(true, params.get("active"));
    }

    @Test
    public void testSelectWithAlias() {
        SqlBuilder query = SqlBuilder
                .select()
                .field("u.id", "userId")
                .field("u.name", "userName")
                .field("p.title", "profileTitle")
                .from("users", "u")
                .leftJoin("profiles p", "u.id = p.user_id");

        String expectedSql = "SELECT u.id AS userId, u.name AS userName, p.title AS profileTitle " +
                "FROM users u LEFT JOIN profiles p ON u.id = p.user_id";
        assertEquals(expectedSql, query.build());
    }

    @Test
    public void testMultipleJoins() {
        SqlBuilder query = SqlBuilder
                .select("o.id", "c.name", "p.name")
                .from("orders", "o")
                .leftJoin("customers c", "o.customer_id = c.id")
                .leftJoin("order_items oi", "o.id = oi.order_id")
                .leftJoin("products p", "oi.product_id = p.id")
                .where("o.status = :status")
                .param("status", "COMPLETED");

        String expectedSql = "SELECT o.id, c.name, p.name FROM orders o " +
                "LEFT JOIN customers c ON o.customer_id = c.id " +
                "LEFT JOIN order_items oi ON o.id = oi.order_id " +
                "LEFT JOIN products p ON oi.product_id = p.id " +
                "WHERE o.status = :status";
        assertEquals(expectedSql, query.build());
        assertEquals("COMPLETED", query.getParameters().get("status"));
    }

    @Test
    public void testSubqueryInSelect() {
        SqlBuilder subQuery = SqlBuilder
                .select("COUNT(*)")
                .from("orders")
                .where("customer_id = c.id");

        SqlBuilder mainQuery = SqlBuilder
                .select("c.id", "c.name")
                .subSelect("order_count", subQuery)
                .from("customers", "c");

        String expectedSql = "SELECT c.id, c.name, (SELECT COUNT(*) FROM orders WHERE customer_id = c.id) AS order_count " +
                "FROM customers c";
        assertEquals(expectedSql, mainQuery.build());
    }

    @Test
    public void testSubqueryInFrom() {
        SqlBuilder subQuery = SqlBuilder
                .select("customer_id")
                .field("SUM(total_amount)", "total_spent")
                .from("orders")
                .where("status = :status")
                .param("status", "COMPLETED")
                .groupBy("customer_id");

        SqlBuilder mainQuery = SqlBuilder
                .select("c.name", "os.total_spent")
                .fromSubQuery(subQuery, "os")
                .leftJoin("customers c", "os.customer_id = c.id")
                .orderByDesc("os.total_spent");

        String expectedSql = "SELECT c.name, os.total_spent " +
                "FROM (SELECT customer_id, SUM(total_amount) AS total_spent " +
                "FROM orders WHERE status = :status GROUP BY customer_id) os " +
                "LEFT JOIN customers c ON os.customer_id = c.id " +
                "ORDER BY os.total_spent DESC";
        assertEquals(expectedSql, mainQuery.build());
        assertEquals("COMPLETED", mainQuery.getParameters().get("status"));
    }

    @Test
    public void testExistsSubquery() {
        SqlBuilder existsSubQuery = SqlBuilder
                .select("1")
                .from("orders")
                .where("customer_id = c.id")
                .and("status = :orderStatus")
                .param("orderStatus", "ACTIVE");

        SqlBuilder mainQuery = SqlBuilder
                .select("*")
                .from("customers", "c")
                .whereExists(existsSubQuery);

        String expectedSql = "SELECT * FROM customers c " +
                "WHERE EXISTS (SELECT 1 FROM orders WHERE customer_id = c.id AND status = :orderStatus)";
        assertEquals(expectedSql, mainQuery.build());
        assertEquals("ACTIVE", mainQuery.getParameters().get("orderStatus"));
    }

    @Test
    public void testInSubquery() {
        SqlBuilder subQuery = SqlBuilder
                .select("customer_id")
                .from("orders")
                .where("order_date >= :recentDate")
                .param("recentDate", "2023-01-01");

        SqlBuilder mainQuery = SqlBuilder
                .select("*")
                .from("customers")
                .whereIn("id", subQuery);

        String expectedSql = "SELECT * FROM customers " +
                "WHERE id IN (SELECT customer_id FROM orders WHERE order_date >= :recentDate)";
        assertEquals(expectedSql, mainQuery.build());
        assertEquals("2023-01-01", mainQuery.getParameters().get("recentDate"));
    }

    @Test
    public void testGroupByAndHaving() {
        SqlBuilder query = SqlBuilder
                .select("category")
                .field("COUNT(*)", "product_count")
                .field("AVG(price)", "avg_price")
                .from("products")
                .where("active = :active")
                .param("active", true)
                .groupBy("category")
                .having("COUNT(*) > :minCount")
                .havingAnd("AVG(price) > :minAvgPrice")
                .param("minCount", 5)
                .param("minAvgPrice", 100.0)
                .orderByDesc("product_count");

        String expectedSql = "SELECT category, COUNT(*) AS product_count, AVG(price) AS avg_price " +
                "FROM products WHERE active = :active GROUP BY category " +
                "HAVING COUNT(*) > :minCount AND AVG(price) > :minAvgPrice " +
                "ORDER BY product_count DESC";
        assertEquals(expectedSql, query.build());

        Map<String, Object> params = query.getParameters();
        assertEquals(true, params.get("active"));
        assertEquals(5, params.get("minCount"));
        assertEquals(100.0, params.get("minAvgPrice"));
    }

    @Test
    public void testLimitAndOffset() {
        SqlBuilder query = SqlBuilder
                .select("*")
                .from("products")
                .where("active = :active")
                .param("active", true)
                .orderBy("name")
                .limit(10)
                .offset(20);

        String expectedSql = "SELECT * FROM products WHERE active = :active ORDER BY name ASC LIMIT 10 OFFSET 20";
        assertEquals(expectedSql, query.build());
    }

    @Test
    public void testDistinct() {
        SqlBuilder query = SqlBuilder
                .selectDistinct("category", "brand")
                .from("products")
                .where("active = :active")
                .param("active", true)
                .orderBy("category");

        String expectedSql = "SELECT DISTINCT category, brand FROM products WHERE active = :active ORDER BY category ASC";
        assertEquals(expectedSql, query.build());
    }

    @Test
    public void testComplexWhereConditions() {
        SqlBuilder query = SqlBuilder
                .select("*")
                .from("products")
                .where("category = :category")
                .and("price BETWEEN :minPrice AND :maxPrice")
                .or("featured = :featured")
                .param("category", "Electronics")
                .param("minPrice", 100)
                .param("maxPrice", 500)
                .param("featured", true);

        String expectedSql = "SELECT * FROM products " +
                "WHERE category = :category " +
                "AND price BETWEEN :minPrice AND :maxPrice " +
                "OR featured = :featured";
        assertEquals(expectedSql, query.build());

        Map<String, Object> params = query.getParameters();
        assertEquals("Electronics", params.get("category"));
        assertEquals(100, params.get("minPrice"));
        assertEquals(500, params.get("maxPrice"));
        assertEquals(true, params.get("featured"));
    }

    @Test
    public void testUpdateBuilder() {
        SqlBuilder.UpdateBuilder update = SqlBuilder.UpdateBuilder
                .update("products")
                .set("price", 150.0)
                .set("updated_at", "CURRENT_TIMESTAMP")
                .where("id = :productId")
                .and("active = :active")
                .param("productId", 123L)
                .param("active", true);

        String expectedSql = "UPDATE products SET price = :price, updated_at = :updated_at " +
                "WHERE id = :productId AND active = :active";
        assertEquals(expectedSql, update.build());

        Map<String, Object> params = update.getParameters();
        assertEquals(150.0, params.get("price"));
        assertEquals("CURRENT_TIMESTAMP", params.get("updated_at"));
        assertEquals(123L, params.get("productId"));
        assertEquals(true, params.get("active"));
    }

    @Test
    public void testDeleteBuilder() {
        SqlBuilder.DeleteBuilder delete = SqlBuilder.DeleteBuilder
                .deleteFrom("products")
                .where("active = :active")
                .and("last_updated < :cutoffDate")
                .param("active", false)
                .param("cutoffDate", "2023-01-01");

        String expectedSql = "DELETE FROM products WHERE active = :active AND last_updated < :cutoffDate";
        assertEquals(expectedSql, delete.build());

        Map<String, Object> params = delete.getParameters();
        assertEquals(false, params.get("active"));
        assertEquals("2023-01-01", params.get("cutoffDate"));
    }

    @Test
    public void testInsertBuilder() {
        SqlBuilder.InsertBuilder insert = SqlBuilder.InsertBuilder
                .insertInto("products")
                .columns("name", "price", "category", "active")
                .values("Test Product", 99.99, "Electronics", true);

        String expectedSql = "INSERT INTO products (name, price, category, active) " +
                "VALUES (:name, :price, :category, :active)";
        assertEquals(expectedSql, insert.build());

        Map<String, Object> params = insert.getParameters();
        assertEquals("Test Product", params.get("name"));
        assertEquals(99.99, params.get("price"));
        assertEquals("Electronics", params.get("category"));
        assertEquals(true, params.get("active"));
    }

    @Test
    public void testInsertBuilderMultipleRows() {
        SqlBuilder.InsertBuilder insert = SqlBuilder.InsertBuilder
                .insertInto("products")
                .columns("name", "price", "category")
                .values("Product 1", 100.0, "Category A")
                .values("Product 2", 200.0, "Category B");

        String expectedSql = "INSERT INTO products (name, price, category) " +
                "VALUES (:name, :price, :category), (:name_1, :price_1, :category_1)";
        assertEquals(expectedSql, insert.build());

        Map<String, Object> params = insert.getParameters();
        assertEquals("Product 1", params.get("name"));
        assertEquals(100.0, params.get("price"));
        assertEquals("Category A", params.get("category"));
        assertEquals("Product 2", params.get("name_1"));
        assertEquals(200.0, params.get("price_1"));
        assertEquals("Category B", params.get("category_1"));
    }

    @Test
    public void testJoinWithSubquery() {
        SqlBuilder subQuery = SqlBuilder
                .select("customer_id")
                .field("COUNT(*)", "order_count")
                .from("orders")
                .groupBy("customer_id")
                .having("COUNT(*) > :minOrders")
                .param("minOrders", 5);

        SqlBuilder mainQuery = SqlBuilder
                .select("c.name", "oc.order_count")
                .from("customers", "c")
                .leftJoinSubQuery(subQuery, "oc", "c.id = oc.customer_id")
                .orderByDesc("oc.order_count");

        String expectedSql = "SELECT c.name, oc.order_count FROM customers c " +
                "LEFT JOIN (SELECT customer_id, COUNT(*) AS order_count FROM orders " +
                "GROUP BY customer_id HAVING COUNT(*) > :minOrders) oc " +
                "ON c.id = oc.customer_id ORDER BY oc.order_count DESC";
        assertEquals(expectedSql, mainQuery.build());
        assertEquals(5, mainQuery.getParameters().get("minOrders"));
    }

    @Test
    public void testEmptySelectDefaultsToStar() {
        SqlBuilder query = SqlBuilder
                .select()
                .from("users");

        String expectedSql = "SELECT * FROM users";
        assertEquals(expectedSql, query.build());
    }

    @Test
    public void testParameterOverride() {
        SqlBuilder query = SqlBuilder
                .select("*")
                .from("users")
                .where("status = :status")
                .param("status", "ACTIVE")
                .param("status", "INACTIVE"); // Override previous value

        assertEquals("INACTIVE", query.getParameters().get("status"));
    }

}
