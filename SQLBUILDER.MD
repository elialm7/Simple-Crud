# SQL Builder - Fluent SQL Query Builder for JDBI

A fluent, type-safe SQL query builder specifically designed for JDBI compatibility. Build complex SQL queries with method chaining while maintaining readability and preventing SQL injection vulnerabilities.

## Table of Contents

- [Features](#features)
- [Database Compatibility](#database-compatibility)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [SELECT Operations](#select-operations)
- [INSERT Operations](#insert-operations)
- [UPDATE Operations](#update-operations)
- [DELETE Operations](#delete-operations)
- [Subqueries](#subqueries)
- [JOINs](#joins)
- [Advanced Features](#advanced-features)
- [Database-Specific Functions](#database-specific-functions)
- [JDBI Integration](#jdbi-integration)
- [Known Limitations](#known-limitations)
- [Best Practices](#best-practices)

## Features

- **Fluent API**: Method chaining for readable query construction
- **JDBI Compatible**: Direct integration with JDBI's named parameter binding
- **Type Safe**: Compile-time safety with builder pattern
- **Comprehensive**: Support for SELECT, INSERT, UPDATE, DELETE operations
- **Advanced Queries**: Subqueries, JOINs, window functions, CTEs simulation
- **Parameter Management**: Automatic parameter binding and management
- **Multiple Database Support**: PostgreSQL, MySQL, SQLite, H2 compatible
- **SQL Injection Prevention**: Parameterized queries by design

## Database Compatibility

### Fully Compatible Databases

| Database | Version | Status | Notes |
|----------|---------|---------|-------|
| **PostgreSQL** | 9.6+ | ✅ Full | Recommended database |
| **MySQL** | 5.7+ | ✅ Full | Except FULL OUTER JOIN |
| **MariaDB** | 10.2+ | ✅ Full | Same as MySQL |
| **SQLite** | 3.8+ | ✅ Full | Limited window functions |
| **H2** | 1.4+ | ✅ Full | Great for testing |

### Partially Compatible Databases

| Database | Version | Status | Limitations |
|----------|---------|---------|-------------|
| **SQL Server** | 2012+ | ⚠️ Partial | LIMIT/OFFSET syntax differences |
| **Oracle** | 12c+ | ⚠️ Partial | LIMIT/OFFSET in older versions |

### Feature Support Matrix

| Feature | PostgreSQL | MySQL | SQL Server | Oracle | SQLite |
|---------|------------|-------|------------|--------|--------|
| Basic SELECT/INSERT/UPDATE/DELETE | ✅ | ✅ | ✅ | ✅ | ✅ |
| INNER/LEFT/RIGHT JOIN | ✅ | ✅ | ✅ | ✅ | ✅ |
| FULL OUTER JOIN | ✅ | ❌ | ✅ | ✅ | ❌ |
| Subqueries | ✅ | ✅ | ✅ | ✅ | ✅ |
| Window Functions | ✅ | ✅ (8.0+) | ✅ | ✅ | ✅ (3.25+) |
| LIMIT/OFFSET | ✅ | ✅ | ⚠️ Different syntax | ⚠️ Different syntax | ✅ |
| JSON Functions | ✅ | ✅ (5.7+) | ✅ (2016+) | ✅ (12c+) | ✅ (3.38+) |
| Regex Operations | ✅ | ✅ | ✅ | ✅ | ❌ |

## Installation

Add the SQL Builder to your project (assuming it's in your classpath):

```java
import com.roelias.crud.SqlBuilder;
```

Ensure you have JDBI dependencies:

```xml
<dependency>
    <groupId>org.jdbi</groupId>
    <artifactId>jdbi3-core</artifactId>
    <version>3.39.0</version>
</dependency>
```

## Quick Start

### Basic SELECT Query

```java
// Simple query
SqlBuilder query = SqlBuilder
    .select("id", "name", "email")
    .from("users")
    .where("active = :active")
    .param("active", true)
    .orderBy("name");

// Execute with JDBI
List<User> users = jdbi.withHandle(handle ->
    handle.createQuery(query.build())
          .bindMap(query.getParameters())
          .mapToBean(User.class)
          .list()
);
```

### Basic INSERT Operation

```java
SqlBuilder.InsertBuilder insert = SqlBuilder.InsertBuilder
    .insertInto("users")
    .columns("name", "email", "status")
    .values("John Doe", "john@example.com", "ACTIVE");

Long userId = jdbi.withHandle(handle ->
    handle.createUpdate(insert.build())
          .bindMap(insert.getParameters())
          .executeAndReturnGeneratedKeys()
          .mapTo(Long.class)
          .one()
);
```

## SELECT Operations

### Basic SELECT

```java
// Select all columns
SqlBuilder query = SqlBuilder
    .select()  // or .select("*")
    .from("users");

// Select specific columns
SqlBuilder query = SqlBuilder
    .select("id", "name", "email")
    .from("users");

// Select with aliases
SqlBuilder query = SqlBuilder
    .select()
    .field("u.id", "userId")
    .field("u.name", "userName")
    .field("UPPER(u.email)", "emailUpper")
    .from("users", "u");
```

### WHERE Conditions

```java
SqlBuilder query = SqlBuilder
    .select("*")
    .from("users")
    .where("age >= :minAge")
    .and("status = :status")
    .or("priority_user = :priority")
    .param("minAge", 18)
    .param("status", "ACTIVE")
    .param("priority", true);
```

### DISTINCT and LIMIT

```java
SqlBuilder query = SqlBuilder
    .selectDistinct("category", "brand")
    .from("products")
    .orderBy("category")
    .limit(50)
    .offset(100);
```

### GROUP BY and HAVING

```java
SqlBuilder query = SqlBuilder
    .select("category")
    .field("COUNT(*)", "product_count")
    .field("AVG(price)", "avg_price")
    .from("products")
    .where("active = :active")
    .param("active", true)
    .groupBy("category")
    .having("COUNT(*) > :minCount")
    .havingAnd("AVG(price) < :maxAvgPrice")
    .param("minCount", 5)
    .param("maxAvgPrice", 1000.0)
    .orderByDesc("product_count");
```

## INSERT Operations

### Single Row INSERT

```java
SqlBuilder.InsertBuilder insert = SqlBuilder.InsertBuilder
    .insertInto("customers")
    .columns("name", "email", "phone", "created_at")
    .values("Jane Smith", "jane@example.com", "+1234567890", "CURRENT_TIMESTAMP");

int rowsAffected = jdbi.withHandle(handle ->
    handle.createUpdate(insert.build())
          .bindMap(insert.getParameters())
          .execute()
);
```

### Multiple Row INSERT

```java
SqlBuilder.InsertBuilder insert = SqlBuilder.InsertBuilder
    .insertInto("products")
    .columns("name", "price", "category")
    .values("Product A", 99.99, "Electronics")
    .values("Product B", 149.99, "Electronics")
    .values("Product C", 79.99, "Books");
```

### INSERT with Calculated Values

```java
SqlBuilder.InsertBuilder insert = SqlBuilder.InsertBuilder
    .insertInto("order_items")
    .columns("order_id", "product_id", "quantity", "unit_price", "total_price")
    .values(orderId, productId, quantity, unitPrice, "(:quantity * :unit_price)")
    .param("quantity", 3)
    .param("unit_price", 29.99);
```

## UPDATE Operations

### Basic UPDATE

```java
SqlBuilder.UpdateBuilder update = SqlBuilder.UpdateBuilder
    .update("users")
    .set("last_login", "CURRENT_TIMESTAMP")
    .set("login_count", "login_count + 1")
    .where("id = :userId")
    .param("userId", 123L);
```

### Conditional UPDATE

```java
SqlBuilder.UpdateBuilder update = SqlBuilder.UpdateBuilder
    .update("products")
    .set("price", "CASE " +
                  "WHEN category = 'Electronics' THEN price * 1.1 " +
                  "WHEN category = 'Books' THEN price * 1.05 " +
                  "ELSE price * 1.03 " +
                  "END")
    .where("active = :active")
    .param("active", true);
```

### UPDATE with Multiple Conditions

```java
SqlBuilder.UpdateBuilder update = SqlBuilder.UpdateBuilder
    .update("orders")
    .set("status", "CANCELLED")
    .set("cancelled_at", "CURRENT_TIMESTAMP")
    .where("status = :currentStatus")
    .and("created_at < :cutoffDate")
    .and("payment_status != :paidStatus")
    .param("currentStatus", "PENDING")
    .param("cutoffDate", "2023-01-01")
    .param("paidStatus", "PAID");
```

## DELETE Operations

### Basic DELETE

```java
SqlBuilder.DeleteBuilder delete = SqlBuilder.DeleteBuilder
    .deleteFrom("users")
    .where("status = :status")
    .and("last_login < :cutoffDate")
    .param("status", "INACTIVE")
    .param("cutoffDate", "2022-01-01");
```

### DELETE with Complex Conditions

```java
SqlBuilder.DeleteBuilder delete = SqlBuilder.DeleteBuilder
    .deleteFrom("logs")
    .where("log_level = :level")
    .and("created_at < CURRENT_DATE - INTERVAL :days DAY")
    .and("message NOT LIKE :importantPattern")
    .param("level", "DEBUG")
    .param("days", 30)
    .param("importantPattern", "%ERROR%");
```

## Subqueries

### Subquery in SELECT

```java
// Count related records
SqlBuilder orderCountSubquery = SqlBuilder
    .select("COUNT(*)")
    .from("orders")
    .where("customer_id = c.id");

SqlBuilder mainQuery = SqlBuilder
    .select("c.id", "c.name")
    .subSelect("order_count", orderCountSubquery)
    .from("customers", "c");
```

### Subquery in FROM

```java
// Use aggregated data as a table
SqlBuilder salesSubquery = SqlBuilder
    .select("product_id")
    .field("SUM(quantity)", "total_sold")
    .from("order_items")
    .groupBy("product_id");

SqlBuilder mainQuery = SqlBuilder
    .select("p.name", "s.total_sold")
    .fromSubQuery(salesSubquery, "s")
    .leftJoin("products p", "s.product_id = p.id")
    .orderByDesc("s.total_sold");
```

### Subquery in WHERE

```java
// EXISTS subquery
SqlBuilder existsSubquery = SqlBuilder
    .select("1")
    .from("orders")
    .where("customer_id = c.id")
    .and("status = :activeStatus")
    .param("activeStatus", "ACTIVE");

SqlBuilder mainQuery = SqlBuilder
    .select("*")
    .from("customers", "c")
    .whereExists(existsSubquery);

// IN subquery
SqlBuilder inSubquery = SqlBuilder
    .select("customer_id")
    .from("orders")
    .where("total_amount > :minAmount")
    .param("minAmount", 1000.0);

SqlBuilder mainQuery = SqlBuilder
    .select("*")
    .from("customers")
    .whereIn("id", inSubquery);
```

## JOINs

### Basic JOINs

```java
SqlBuilder query = SqlBuilder
    .select("u.name", "p.title", "c.name as company")
    .from("users", "u")
    .leftJoin("profiles p", "u.id = p.user_id")
    .leftJoin("companies c", "p.company_id = c.id")
    .where("u.active = :active")
    .param("active", true);
```

### JOIN with Subquery

```java
SqlBuilder recentOrdersSubquery = SqlBuilder
    .select("customer_id")
    .field("COUNT(*)", "recent_count")
    .from("orders")
    .where("order_date >= :recentDate")
    .param("recentDate", "2023-06-01")
    .groupBy("customer_id");

SqlBuilder query = SqlBuilder
    .select("c.name", "ro.recent_count")
    .from("customers", "c")
    .leftJoinSubQuery(recentOrdersSubquery, "ro", "c.id = ro.customer_id")
    .orderByDesc("ro.recent_count");
```

### Multiple JOINs

```java
SqlBuilder query = SqlBuilder
    .select()
    .field("o.id", "order_id")
    .field("c.name", "customer_name")
    .field("p.name", "product_name")
    .field("oi.quantity")
    .from("orders", "o")
    .innerJoin("customers c", "o.customer_id = c.id")
    .innerJoin("order_items oi", "o.id = oi.order_id")
    .innerJoin("products p", "oi.product_id = p.id")
    .where("o.status = :status")
    .param("status", "COMPLETED");
```

## Advanced Features

### Dynamic Query Building

```java
public List<Product> searchProducts(ProductFilter filter) {
    SqlBuilder query = SqlBuilder
        .select("*")
        .from("products")
        .where("active = :active")
        .param("active", true);
    
    // Add conditions dynamically
    if (filter.getCategory() != null) {
        query.and("category = :category")
             .param("category", filter.getCategory());
    }
    
    if (filter.getMinPrice() != null) {
        query.and("price >= :minPrice")
             .param("minPrice", filter.getMinPrice());
    }
    
    if (filter.getSearchText() != null) {
        query.and("(name ILIKE :search OR description ILIKE :search)")
             .param("search", "%" + filter.getSearchText() + "%");
    }
    
    return executeQuery(query);
}
```

### Pagination

```java
public PaginatedResult<Customer> getCustomersPaginated(int page, int pageSize) {
    // Count query
    SqlBuilder countQuery = SqlBuilder
        .select("COUNT(*)")
        .from("customers")
        .where("active = :active")
        .param("active", true);
    
    // Data query
    SqlBuilder dataQuery = SqlBuilder
        .select("*")
        .from("customers")
        .where("active = :active")
        .param("active", true)
        .orderBy("name")
        .limit(pageSize)
        .offset(page * pageSize);
    
    return executeWithPagination(countQuery, dataQuery);
}
```

### Window Functions

```java
// Ranking and analytics
SqlBuilder query = SqlBuilder
    .select("id", "name", "salary", "department")
    .field("ROW_NUMBER() OVER (PARTITION BY department ORDER BY salary DESC)", "dept_rank")
    .field("RANK() OVER (ORDER BY salary DESC)", "company_rank")
    .field("AVG(salary) OVER (PARTITION BY department)", "dept_avg_salary")
    .from("employees")
    .where("active = :active")
    .param("active", true);
```

## Database-Specific Functions

### PostgreSQL Examples

```java
// JSON operations
SqlBuilder query = SqlBuilder
    .select("id", "name")
    .field("metadata->>'category'", "category")
    .field("jsonb_array_length(tags)", "tag_count")
    .from("products")
    .where("tags @> :tag")
    .param("tag", "[\"electronics\"]");

// Full-text search
SqlBuilder query = SqlBuilder
    .select("*")
    .from("articles")
    .where("to_tsvector('english', title || ' ' || content) @@ to_tsquery(:query)")
    .param("query", "postgresql & performance");

// Array operations
SqlBuilder query = SqlBuilder
    .select("*")
    .from("users")
    .where("interests && :userInterests")
    .param("userInterests", "{technology,programming}");
```

### MySQL Examples

```java
// Date functions
SqlBuilder query = SqlBuilder
    .select("id", "name")
    .field("DATE_FORMAT(created_at, '%Y-%m')", "creation_month")
    .from("users")
    .where("STR_TO_DATE(birth_date, '%Y-%m-%d') > :minDate")
    .param("minDate", "1990-01-01");

// String functions
SqlBuilder query = SqlBuilder
    .select("id")
    .field("CONCAT(first_name, ' ', last_name)", "full_name")
    .field("SUBSTRING(email, 1, LOCATE('@', email) - 1)", "username")
    .from("users")
    .where("MATCH(first_name, last_name) AGAINST(:searchTerm IN BOOLEAN MODE)")
    .param("searchTerm", "+john +doe");

// JSON functions (MySQL 5.7+)
SqlBuilder query = SqlBuilder
    .select("*")
    .from("products")
    .where("JSON_CONTAINS(tags, :searchTag)")
    .param("searchTag", "\"electronics\"");
```

### SQL Server Adaptations

```java
// Note: LIMIT/OFFSET requires different syntax in SQL Server
// The builder generates standard LIMIT/OFFSET, which works in SQL Server 2012+
// For older versions, you'll need to modify the query

// Date functions
SqlBuilder query = SqlBuilder
    .select("*")
    .from("orders")
    .where("CONVERT(date, order_datetime) = :orderDate")
    .param("orderDate", "2023-06-15");

// String functions
SqlBuilder query = SqlBuilder
    .select()
    .field("CONCAT(first_name, ' ', last_name)", "full_name")
    .field("LEFT(email, CHARINDEX('@', email) - 1)", "username")
    .from("users")
    .where("LEN(first_name) >= :minLength")
    .param("minLength", 2);
```

## JDBI Integration

### Basic Integration

```java
public class UserDao {
    private final Jdbi jdbi;
    
    public UserDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }
    
    public List<User> findActiveUsers() {
        SqlBuilder query = SqlBuilder
            .select("*")
            .from("users")
            .where("active = :active")
            .param("active", true)
            .orderBy("name");
        
        return jdbi.withHandle(handle ->
            handle.createQuery(query.build())
                  .bindMap(query.getParameters())
                  .mapToBean(User.class)
                  .list()
        );
    }
    
    public Long createUser(User user) {
        SqlBuilder.InsertBuilder insert = SqlBuilder.InsertBuilder
            .insertInto("users")
            .columns("name", "email", "active", "created_at")
            .values(user.getName(), user.getEmail(), true, "CURRENT_TIMESTAMP");
        
        return jdbi.withHandle(handle ->
            handle.createUpdate(insert.build())
                  .bindMap(insert.getParameters())
                  .executeAndReturnGeneratedKeys()
                  .mapTo(Long.class)
                  .one()
        );
    }
}
```

### Transaction Support

```java
public void transferOrder(Long orderId, Long newCustomerId) {
    jdbi.useTransaction(handle -> {
        // Verify order exists
        SqlBuilder checkQuery = SqlBuilder
            .select("COUNT(*)")
            .from("orders")
            .where("id = :orderId")
            .param("orderId", orderId);
        
        int count = handle.createQuery(checkQuery.build())
                         .bindMap(checkQuery.getParameters())
                         .mapTo(Integer.class)
                         .one();
        
        if (count == 0) {
            throw new RuntimeException("Order not found");
        }
        
        // Update order
        SqlBuilder.UpdateBuilder update = SqlBuilder.UpdateBuilder
            .update("orders")
            .set("customer_id", newCustomerId)
            .set("updated_at", "CURRENT_TIMESTAMP")
            .where("id = :orderId")
            .param("orderId", orderId);
        
        handle.createUpdate(update.build())
              .bindMap(update.getParameters())
              .execute();
    });
}
```

### Batch Operations

```java
public int[] batchInsertProducts(List<Product> products) {
    return jdbi.withHandle(handle -> {
        PreparedBatch batch = handle.prepareBatch(
            SqlBuilder.InsertBuilder
                .insertInto("products")
                .columns("name", "price", "category")
                .values(":name", ":price", ":category")
                .build()
        );
        
        for (Product product : products) {
            batch.bind("name", product.getName())
                 .bind("price", product.getPrice())
                 .bind("category", product.getCategory())
                 .add();
        }
        
        return batch.execute();
    });
}
```

## Known Limitations

### General Limitations

1. **Raw SQL Mixing**: When using database-specific functions, you lose some type safety
2. **Complex CTEs**: Common Table Expressions must be built manually
3. **Advanced Window Functions**: Some complex window operations need manual SQL
4. **Stored Procedures**: No direct support for procedure calls

### Database-Specific Issues

#### MySQL
```java
// FULL OUTER JOIN not supported
// This will fail in MySQL:
SqlBuilder query = SqlBuilder
    .select("*")
    .from("table1")
    .fullJoin("table2", "table1.id = table2.id");  // ❌ Not supported in MySQL

// Use UNION instead:
SqlBuilder leftJoin = SqlBuilder
    .select("*")
    .from("table1")
    .leftJoin("table2", "table1.id = table2.id");

SqlBuilder rightJoin = SqlBuilder
    .select("*")
    .from("table1")
    .rightJoin("table2", "table1.id = table2.id")
    .where("table1.id IS NULL");

// Combine with UNION (manual SQL required)
```

#### SQL Server (Pre-2012)
```java
// LIMIT/OFFSET not supported in older versions
// The builder generates LIMIT/OFFSET syntax
// For SQL Server 2008/2005, you need to use ROW_NUMBER():

SqlBuilder query = SqlBuilder
    .select()
    .field("ROW_NUMBER() OVER (ORDER BY id)", "rn")
    .field("*")
    .from("users")
    .where("active = :active")
    .param("active", true);

// Then wrap in another query to filter by row number
```

#### Oracle (Pre-12c)
```java
// LIMIT/OFFSET syntax different
// Modern Oracle (12c+) supports LIMIT/OFFSET
// For older versions, use ROWNUM:

SqlBuilder query = SqlBuilder
    .select("*")
    .from("(SELECT u.*, ROWNUM rn FROM users u WHERE ROWNUM <= :maxRows)")
    .where("rn > :offset")
    .param("maxRows", 50)
    .param("offset", 20);
```

#### SQLite Limitations
```java
// Limited window functions in older versions
// Some JSON functions not available
// No regex support

// This might not work in older SQLite:
SqlBuilder query = SqlBuilder
    .select("*")
    .from("users")
    .where("email REGEXP :pattern")  // ❌ No regex in SQLite
    .param("pattern", ".*@gmail\\.com");

// Use LIKE instead:
SqlBuilder query = SqlBuilder
    .select("*")
    .from("users")
    .where("email LIKE :pattern")
    .param("pattern", "%@gmail.com");
```

## Best Practices

### Parameter Naming
```java
// Good: Descriptive parameter names
SqlBuilder query = SqlBuilder
    .select("*")
    .from("orders")
    .where("customer_id = :customerId")
    .and("status = :orderStatus")
    .param("customerId", 123L)
    .param("orderStatus", "COMPLETED");

// Avoid: Generic parameter names
SqlBuilder query = SqlBuilder
    .select("*")
    .from("orders")
    .where("customer_id = :param1")
    .and("status = :param2")
    .param("param1", 123L)
    .param("param2", "COMPLETED");
```

### Query Reusability
```java
public class QueryTemplates {
    public static SqlBuilder activeUsersBase() {
        return SqlBuilder
            .select("*")
            .from("users")
            .where("active = :active")
            .param("active", true);
    }
    
    public static SqlBuilder recentActiveUsers(int days) {
        return activeUsersBase()
            .and("last_login >= CURRENT_DATE - INTERVAL :days DAY")
            .param("days", days);
    }
}
```

### Error Handling
```java
public List<User> findUsers(UserFilter filter) {
    try {
        SqlBuilder query = buildUserQuery(filter);
        
        return jdbi.withHandle(handle ->
            handle.createQuery(query.build())
                  .bindMap(query.getParameters())
                  .mapToBean(User.class)
                  .list()
        );
    } catch (Exception e) {
        log.error("Failed to execute user query: {}", query.build(), e);
        throw new DatabaseException("User query failed", e);
    }
}
```

### Testing
```java
@Test
public void testQueryGeneration() {
    SqlBuilder query = SqlBuilder
        .select("id", "name")
        .from("users")
        .where("active = :active")
        .param("active", true);
    
    String expectedSql = "SELECT id, name FROM users WHERE active = :active";
    assertEquals(expectedSql, query.build());
    
    Map<String, Object> params = query.getParameters();
    assertEquals(true, params.get("active"));
}
```

### Performance Considerations
```java
// Good: Use appropriate indexes
SqlBuilder query = SqlBuilder
    .select("*")
    .from("orders")
    .where("customer_id = :customerId")  // Ensure index on customer_id
    .and("created_at >= :startDate")     // Ensure index on created_at
    .param("customerId", 123L)
    .param("startDate", "2023-01-01")
    .orderBy("created_at")
    .limit(100);  // Always limit large result sets

// Avoid: Functions in WHERE clauses when possible
// This prevents index usage:
SqlBuilder query = SqlBuilder
    .select("*")
    .from("users")
    .where("UPPER(email) = :email")
    .param("email", "JOHN@EXAMPLE.COM");

// Better: Use case-insensitive comparison
SqlBuilder query = SqlBuilder
    .select("*")
    .from("users")
    .where("email ILIKE :email")  // PostgreSQL
    .param("email", "john@example.com");
```

## Conclusion

This SQL Builder provides a powerful, type-safe way to construct SQL queries while maintaining compatibility with JDBI. It works excellently with PostgreSQL and MySQL, with good support for other major databases. When using database-specific features, always test thoroughly with your target database version.

For production use, ensure you:
- Test queries with your specific database version
- Monitor query performance
- Use appropriate indexes
- Handle database-specific exceptions
- Validate user inputs before query construction

The builder prioritizes safety and readability while providing the flexibility to use advanced SQL features when needed.