package com.roelias.sql;

import com.roelias.crud.SqlBuilder;
import org.jdbi.v3.core.Jdbi;

import java.util.List;

public class SqlBuilderExamples {

    private final Jdbi jdbi;

    public SqlBuilderExamples(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    // Ejemplo 1: Consulta simple con WHERE
    public List<User> findUsersByStatus(String status) {
        SqlBuilder query = SqlBuilder
                .select("id", "name", "email", "status")
                .from("users")
                .where("status = :status")
                .param("status", status)
                .orderBy("name");

        return jdbi.withHandle(handle ->
                handle.createQuery(query.build())
                        .bindMap(query.getParameters())
                        .mapToBean(User.class)
                        .list()
        );
    }

    // Ejemplo 2: Consulta con múltiples JOINs
    public List<OrderDetails> getOrdersWithCustomerAndProducts() {
        SqlBuilder query = SqlBuilder
                .select()
                .field("o.id", "orderId")
                .field("o.order_date", "orderDate")
                .field("c.name", "customerName")
                .field("c.email", "customerEmail")
                .field("p.name", "productName")
                .field("oi.quantity")
                .field("oi.price")
                .from("orders", "o")
                .leftJoin("customers c", "o.customer_id = c.id")
                .leftJoin("order_items oi", "o.id = oi.order_id")
                .leftJoin("products p", "oi.product_id = p.id")
                .where("o.status = :status")
                .param("status", "COMPLETED")
                .orderBy("o.order_date", SqlBuilder.OrderDirection.DESC);

        return jdbi.withHandle(handle ->
                handle.createQuery(query.build())
                        .bindMap(query.getParameters())
                        .mapToBean(OrderDetails.class)
                        .list()
        );
    }

    // Ejemplo 3: Subconsulta en SELECT
    public List<CustomerSummary> getCustomersWithOrderCount() {
        // Subconsulta para contar órdenes
        SqlBuilder orderCountSubQuery = SqlBuilder
                .select("COUNT(*)")
                .from("orders")
                .where("customer_id = c.id");

        SqlBuilder query = SqlBuilder
                .select("c.id", "c.name", "c.email")
                .subSelect("order_count", orderCountSubQuery)
                .from("customers", "c")
                .where("c.active = :active")
                .param("active", true)
                .orderBy("c.name");

        return jdbi.withHandle(handle ->
                handle.createQuery(query.build())
                        .bindMap(query.getParameters())
                        .mapToBean(CustomerSummary.class)
                        .list()
        );
    }

    // Ejemplo 4: Subconsulta en FROM
    public List<TopCustomer> getTopCustomersByOrders() {
        // Subconsulta para obtener el conteo de órdenes por cliente
        SqlBuilder customerOrdersSubQuery = SqlBuilder
                .select("customer_id")
                .field("COUNT(*)", "order_count")
                .from("orders")
                .where("status = :status")
                .param("status", "COMPLETED")
                .groupBy("customer_id")
                .having("COUNT(*) >= :minOrders")
                .param("minOrders", 5);

        SqlBuilder query = SqlBuilder
                .select("c.name", "c.email", "co.order_count")
                .fromSubQuery(customerOrdersSubQuery, "co")
                .leftJoin("customers c", "co.customer_id = c.id")
                .orderByDesc("co.order_count")
                .limit(10);

        return jdbi.withHandle(handle ->
                handle.createQuery(query.build())
                        .bindMap(query.getParameters())
                        .mapToBean(TopCustomer.class)
                        .list()
        );
    }

    // Ejemplo 5: Consulta con EXISTS
    public List<Product> getProductsWithActiveOrders() {
        SqlBuilder existsSubQuery = SqlBuilder
                .select("1")
                .from("order_items oi")
                .leftJoin("orders o", "oi.order_id = o.id")
                .where("oi.product_id = p.id")
                .and("o.status = :orderStatus")
                .param("orderStatus", "ACTIVE");

        SqlBuilder query = SqlBuilder
                .select("p.id", "p.name", "p.price", "p.category")
                .from("products", "p")
                .whereExists(existsSubQuery)
                .orderBy("p.category")
                .orderBy("p.name");

        return jdbi.withHandle(handle ->
                handle.createQuery(query.build())
                        .bindMap(query.getParameters())
                        .mapToBean(Product.class)
                        .list()
        );
    }

    // Ejemplo 6: Consulta con IN y subconsulta
    public List<Customer> getCustomersWithRecentOrders(int days) {
        SqlBuilder recentOrdersSubQuery = SqlBuilder
                .select("DISTINCT customer_id")
                .from("orders")
                .where("order_date >= CURRENT_DATE - INTERVAL :days DAY")
                .param("days", days);

        SqlBuilder query = SqlBuilder
                .select("id", "name", "email", "phone")
                .from("customers")
                .whereIn("id", recentOrdersSubQuery)
                .orderBy("name");

        return jdbi.withHandle(handle ->
                handle.createQuery(query.build())
                        .bindMap(query.getParameters())
                        .mapToBean(Customer.class)
                        .list()
        );
    }

    // Ejemplo 7: Consulta compleja con múltiples condiciones y agrupación
    public List<ProductSales> getProductSalesReport(String dateFrom, String dateTo) {
        SqlBuilder query = SqlBuilder
                .select()
                .field("p.id", "productId")
                .field("p.name", "productName")
                .field("p.category", "category")
                .field("SUM(oi.quantity)", "totalQuantity")
                .field("SUM(oi.quantity * oi.price)", "totalRevenue")
                .field("COUNT(DISTINCT o.id)", "orderCount")
                .from("products", "p")
                .leftJoin("order_items oi", "p.id = oi.product_id")
                .leftJoin("orders o", "oi.order_id = o.id")
                .where("o.order_date >= :dateFrom")
                .and("o.order_date <= :dateTo")
                .and("o.status = :status")
                .param("dateFrom", dateFrom)
                .param("dateTo", dateTo)
                .param("status", "COMPLETED")
                .groupBy("p.id", "p.name", "p.category")
                .having("SUM(oi.quantity) > :minQuantity")
                .param("minQuantity", 10)
                .orderByDesc("totalRevenue")
                .limit(50);

        return jdbi.withHandle(handle ->
                handle.createQuery(query.build())
                        .bindMap(query.getParameters())
                        .mapToBean(ProductSales.class)
                        .list()
        );
    }

    // Ejemplo 8: UPDATE con SqlBuilder
    public int updateCustomerStatus(Long customerId, String newStatus) {
        SqlBuilder.UpdateBuilder update = SqlBuilder.UpdateBuilder
                .update("customers")
                .set("status", newStatus)
                .set("updated_at", "CURRENT_TIMESTAMP")
                .where("id = :customerId")
                .param("customerId", customerId);

        return jdbi.withHandle(handle ->
                handle.createUpdate(update.build())
                        .bindMap(update.getParameters())
                        .execute()
        );
    }

    // Ejemplo 9: UPDATE con múltiples condiciones
    public int updateProductPrices(String category, double percentageIncrease) {
        SqlBuilder.UpdateBuilder update = SqlBuilder.UpdateBuilder
                .update("products")
                .set("price", "price * (1 + :percentage / 100)")
                .set("updated_at", "CURRENT_TIMESTAMP")
                .where("category = :category")
                .and("active = :active")
                .param("percentage", percentageIncrease)
                .param("category", category)
                .param("active", true);

        return jdbi.withHandle(handle ->
                handle.createUpdate(update.build())
                        .bindMap(update.getParameters())
                        .execute()
        );
    }

    // Ejemplo 10: DELETE con SqlBuilder
    public int deleteInactiveCustomers(int daysInactive) {
        SqlBuilder.DeleteBuilder delete = SqlBuilder.DeleteBuilder
                .deleteFrom("customers")
                .where("last_activity_date < CURRENT_DATE - INTERVAL :days DAY")
                .and("status = :status")
                .param("days", daysInactive)
                .param("status", "INACTIVE");

        return jdbi.withHandle(handle ->
                handle.createUpdate(delete.build())
                        .bindMap(delete.getParameters())
                        .execute()
        );
    }

    // Ejemplo 11: INSERT con SqlBuilder
    public int insertNewCustomer(String name, String email, String phone) {
        SqlBuilder.InsertBuilder insert = SqlBuilder.InsertBuilder
                .insertInto("customers")
                .columns("name", "email", "phone", "status", "created_at")
                .values(name, email, phone, "ACTIVE", "CURRENT_TIMESTAMP");

        return jdbi.withHandle(handle ->
                handle.createUpdate(insert.build())
                        .bindMap(insert.getParameters())
                        .execute()
        );
    }

    // Ejemplo 12: INSERT múltiple
    public int insertMultipleProducts(List<ProductData> products) {
        SqlBuilder.InsertBuilder insert = SqlBuilder.InsertBuilder
                .insertInto("products")
                .columns("name", "price", "category", "active");

        for (ProductData product : products) {
            insert.values(product.getName(), product.getPrice(), product.getCategory(), true);
        }

        return jdbi.withHandle(handle ->
                handle.createUpdate(insert.build())
                        .bindMap(insert.getParameters())
                        .execute()
        );
    }

    // Ejemplo 13: Consulta paginada
    public PaginatedResult<Customer> getCustomersPaginated(int page, int pageSize, String searchTerm) {
        // Consulta para contar el total
        SqlBuilder countQuery = SqlBuilder
                .select("COUNT(*)")
                .from("customers")
                .where("name ILIKE :searchTerm OR email ILIKE :searchTerm")
                .param("searchTerm", "%" + searchTerm + "%");

        // Consulta para obtener los datos paginados
        SqlBuilder dataQuery = SqlBuilder
                .select("id", "name", "email", "phone", "status")
                .from("customers")
                .where("name ILIKE :searchTerm OR email ILIKE :searchTerm")
                .param("searchTerm", "%" + searchTerm + "%")
                .orderBy("name")
                .limit(pageSize)
                .offset(page * pageSize);

        return jdbi.withHandle(handle -> {
            // Obtener el total de registros
            int total = handle.createQuery(countQuery.build())
                    .bindMap(countQuery.getParameters())
                    .mapTo(Integer.class)
                    .one();

            // Obtener los datos paginados
            List<Customer> customers = handle.createQuery(dataQuery.build())
                    .bindMap(dataQuery.getParameters())
                    .mapToBean(Customer.class)
                    .list();

            return new PaginatedResult<>(customers, total, page, pageSize);
        });
    }

    // Ejemplo 14: Consulta dinámica con condiciones opcionales
    public List<Order> searchOrders(OrderSearchCriteria criteria) {
        SqlBuilder query = SqlBuilder
                .select("o.id", "o.order_date", "o.status", "c.name as customer_name")
                .from("orders", "o")
                .leftJoin("customers c", "o.customer_id = c.id");

        // Agregar condiciones dinámicamente
        if (criteria.getCustomerId() != null) {
            query.and("o.customer_id = :customerId")
                    .param("customerId", criteria.getCustomerId());
        }

        if (criteria.getStatus() != null) {
            query.and("o.status = :status")
                    .param("status", criteria.getStatus());
        }

        if (criteria.getDateFrom() != null) {
            query.and("o.order_date >= :dateFrom")
                    .param("dateFrom", criteria.getDateFrom());
        }

        if (criteria.getDateTo() != null) {
            query.and("o.order_date <= :dateTo")
                    .param("dateTo", criteria.getDateTo());
        }

        if (criteria.getMinAmount() != null) {
            query.and("o.total_amount >= :minAmount")
                    .param("minAmount", criteria.getMinAmount());
        }

        query.orderBy("o.order_date", SqlBuilder.OrderDirection.DESC);

        if (criteria.getLimit() != null) {
            query.limit(criteria.getLimit());
        }

        return jdbi.withHandle(handle ->
                handle.createQuery(query.build())
                        .bindMap(query.getParameters())
                        .mapToBean(Order.class)
                        .list()
        );
    }

    // Ejemplo 15: Transacción con múltiples operaciones
    public void transferOrderToNewCustomer(Long orderId, Long newCustomerId) {
        jdbi.useTransaction(handle -> {
            // Verificar que la orden existe
            SqlBuilder orderCheck = SqlBuilder
                    .select("COUNT(*)")
                    .from("orders")
                    .where("id = :orderId")
                    .param("orderId", orderId);

            int orderExists = handle.createQuery(orderCheck.build())
                    .bindMap(orderCheck.getParameters())
                    .mapTo(Integer.class)
                    .one();

            if (orderExists == 0) {
                throw new RuntimeException("Order not found: " + orderId);
            }

            // Actualizar la orden
            SqlBuilder.UpdateBuilder updateOrder = SqlBuilder.UpdateBuilder
                    .update("orders")
                    .set("customer_id", newCustomerId)
                    .set("updated_at", "CURRENT_TIMESTAMP")
                    .where("id = :orderId")
                    .param("customer_id", newCustomerId)
                    .param("orderId", orderId);

            handle.createUpdate(updateOrder.build())
                    .bindMap(updateOrder.getParameters())
                    .execute();

            // Registrar el cambio en el log
            SqlBuilder.InsertBuilder insertLog = SqlBuilder.InsertBuilder
                    .insertInto("order_transfer_log")
                    .columns("order_id", "new_customer_id", "transfer_date")
                    .values(orderId, newCustomerId, "CURRENT_TIMESTAMP");

            handle.createUpdate(insertLog.build())
                    .bindMap(insertLog.getParameters())
                    .execute();
        });
    }

    // Clases de modelo para los ejemplos
    public static class User {
        private Long id;
        private String name;
        private String email;
        private String status;

        // Getters y setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class OrderDetails {
        private Long orderId;
        private String orderDate;
        private String customerName;
        private String customerEmail;
        private String productName;
        private Integer quantity;
        private Double price;

        // Getters y setters
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public String getOrderDate() { return orderDate; }
        public void setOrderDate(String orderDate) { this.orderDate = orderDate; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getCustomerEmail() { return customerEmail; }
        public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
    }

    public static class CustomerSummary {
        private Long id;
        private String name;
        private String email;
        private Integer orderCount;

        // Getters y setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Integer getOrderCount() { return orderCount; }
        public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }
    }

    public static class TopCustomer {
        private String name;
        private String email;
        private Integer orderCount;

        // Getters y setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Integer getOrderCount() { return orderCount; }
        public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }
    }

    public static class Product {
        private Long id;
        private String name;
        private Double price;
        private String category;

        // Getters y setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }

    public static class Customer {
        private Long id;
        private String name;
        private String email;
        private String phone;
        private String status;

        // Getters y setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class ProductSales {
        private Long productId;
        private String productName;
        private String category;
        private Integer totalQuantity;
        private Double totalRevenue;
        private Integer orderCount;

        // Getters y setters
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public Integer getTotalQuantity() { return totalQuantity; }
        public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
        public Double getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }
        public Integer getOrderCount() { return orderCount; }
        public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }
    }

    public static class ProductData {
        private String name;
        private Double price;
        private String category;

        public ProductData(String name, Double price, String category) {
            this.name = name;
            this.price = price;
            this.category = category;
        }

        // Getters
        public String getName() { return name; }
        public Double getPrice() { return price; }
        public String getCategory() { return category; }
    }

    public static class Order {
        private Long id;
        private String orderDate;
        private String status;
        private String customerName;

        // Getters y setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getOrderDate() { return orderDate; }
        public void setOrderDate(String orderDate) { this.orderDate = orderDate; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
    }

    public static class PaginatedResult<T> {
        private List<T> data;
        private int total;
        private int page;
        private int pageSize;
        private int totalPages;

        public PaginatedResult(List<T> data, int total, int page, int pageSize) {
            this.data = data;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
            this.totalPages = (int) Math.ceil((double) total / pageSize);
        }

        // Getters
        public List<T> getData() { return data; }
        public int getTotal() { return total; }
        public int getPage() { return page; }
        public int getPageSize() { return pageSize; }
        public int getTotalPages() { return totalPages; }
    }

    public static class OrderSearchCriteria {
        private Long customerId;
        private String status;
        private String dateFrom;
        private String dateTo;
        private Double minAmount;
        private Integer limit;

        // Getters y setters
        public Long getCustomerId() { return customerId; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getDateFrom() { return dateFrom; }
        public void setDateFrom(String dateFrom) { this.dateFrom = dateFrom; }
        public String getDateTo() { return dateTo; }
        public void setDateTo(String dateTo) { this.dateTo = dateTo; }
        public Double getMinAmount() { return minAmount; }
        public void setMinAmount(Double minAmount) { this.minAmount = minAmount; }
        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }
    }
}
