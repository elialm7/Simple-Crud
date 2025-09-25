package com.roelias.crud;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SQL Builder fluido compatible con JDBI
 * Permite construir consultas SQL complejas con sintaxis fluida
 */
public class SqlBuilder {

    public enum JoinType {
        INNER("INNER JOIN"),
        LEFT("LEFT JOIN"),
        RIGHT("RIGHT JOIN"),
        FULL("FULL OUTER JOIN");

        private final String sql;

        JoinType(String sql) {
            this.sql = sql;
        }

        public String getSql() {
            return sql;
        }
    }

    public enum OrderDirection {
        ASC, DESC
    }

    // Clases internas para representar componentes de la consulta
    public static class SelectField {
        private final String field;
        private final String alias;

        public SelectField(String field, String alias) {
            this.field = field;
            this.alias = alias;
        }

        public String toSql() {
            return alias != null ? field + " AS " + alias : field;
        }
    }

    public static class JoinClause {
        private final JoinType type;
        private final String table;
        private final String alias;
        private final String condition;

        public JoinClause(JoinType type, String table, String alias, String condition) {
            this.type = type;
            this.table = table;
            this.alias = alias;
            this.condition = condition;
        }

        public String toSql() {
            String tableWithAlias = alias != null ? table + " " + alias : table;
            return type.getSql() + " " + tableWithAlias + " ON " + condition;
        }
    }

    public static class WhereCondition {
        private final String condition;
        private final String operator;

        public WhereCondition(String condition, String operator) {
            this.condition = condition;
            this.operator = operator;
        }

        public String getCondition() {
            return condition;
        }

        public String getOperator() {
            return operator;
        }
    }

    public static class OrderByClause {
        private final String field;
        private final OrderDirection direction;

        public OrderByClause(String field, OrderDirection direction) {
            this.field = field;
            this.direction = direction;
        }

        public String toSql() {
            return field + " " + direction.name();
        }
    }

    // Campos del builder
    private final List<SelectField> selectFields = new ArrayList<>();
    private String fromTable;
    private String fromAlias;
    private final List<JoinClause> joins = new ArrayList<>();
    private final List<WhereCondition> whereConditions = new ArrayList<>();
    private final List<String> groupByFields = new ArrayList<>();
    private final List<WhereCondition> havingConditions = new ArrayList<>();
    private final List<OrderByClause> orderByFields = new ArrayList<>();
    private Integer limitValue;
    private Integer offsetValue;
    private final Map<String, Object> parameters = new HashMap<>();
    private boolean distinct = false;

    // Constructor privado para forzar uso de métodos estáticos
    private SqlBuilder() {}

    // Métodos estáticos para iniciar construcción
    public static SqlBuilder select(String... fields) {
        SqlBuilder builder = new SqlBuilder();
        for (String field : fields) {
            builder.selectFields.add(new SelectField(field, null));
        }
        return builder;
    }

    public static SqlBuilder select() {
        return new SqlBuilder();
    }

    public static SqlBuilder selectDistinct(String... fields) {
        SqlBuilder builder = select(fields);
        builder.distinct = true;
        return builder;
    }

    // Métodos fluidos para SELECT
    public SqlBuilder field(String field) {
        this.selectFields.add(new SelectField(field, null));
        return this;
    }

    public SqlBuilder field(String field, String alias) {
        this.selectFields.add(new SelectField(field, alias));
        return this;
    }

    public SqlBuilder fields(String... fields) {
        for (String field : fields) {
            this.selectFields.add(new SelectField(field, null));
        }
        return this;
    }

    public SqlBuilder distinct() {
        this.distinct = true;
        return this;
    }

    // Subconsulta en SELECT
    public SqlBuilder subSelect(String alias, SqlBuilder subQuery) {
        String subQuerySql = "(" + subQuery.build() + ")";
        this.selectFields.add(new SelectField(subQuerySql, alias));
        // Agregar parámetros de la subconsulta
        this.parameters.putAll(subQuery.parameters);
        return this;
    }

    // FROM
    public SqlBuilder from(String table) {
        this.fromTable = table;
        return this;
    }

    public SqlBuilder from(String table, String alias) {
        this.fromTable = table;
        this.fromAlias = alias;
        return this;
    }

    // Subconsulta en FROM
    public SqlBuilder fromSubQuery(SqlBuilder subQuery, String alias) {
        this.fromTable = "(" + subQuery.build() + ")";
        this.fromAlias = alias;
        this.parameters.putAll(subQuery.parameters);
        return this;
    }

    // JOINs
    public SqlBuilder join(String table, String condition) {
        return join(JoinType.INNER, table, null, condition);
    }

    public SqlBuilder join(String table, String alias, String condition) {
        return join(JoinType.INNER, table, alias, condition);
    }

    public SqlBuilder leftJoin(String table, String condition) {
        return join(JoinType.LEFT, table, null, condition);
    }

    public SqlBuilder leftJoin(String table, String alias, String condition) {
        return join(JoinType.LEFT, table, alias, condition);
    }

    public SqlBuilder rightJoin(String table, String condition) {
        return join(JoinType.RIGHT, table, null, condition);
    }

    public SqlBuilder rightJoin(String table, String alias, String condition) {
        return join(JoinType.RIGHT, table, alias, condition);
    }

    public SqlBuilder fullJoin(String table, String condition) {
        return join(JoinType.FULL, table, null, condition);
    }

    public SqlBuilder fullJoin(String table, String alias, String condition) {
        return join(JoinType.FULL, table, alias, condition);
    }

    private SqlBuilder join(JoinType type, String table, String alias, String condition) {
        this.joins.add(new JoinClause(type, table, alias, condition));
        return this;
    }

    // JOIN con subconsulta
    public SqlBuilder joinSubQuery(SqlBuilder subQuery, String alias, String condition) {
        return joinSubQuery(JoinType.INNER, subQuery, alias, condition);
    }

    public SqlBuilder leftJoinSubQuery(SqlBuilder subQuery, String alias, String condition) {
        return joinSubQuery(JoinType.LEFT, subQuery, alias, condition);
    }

    private SqlBuilder joinSubQuery(JoinType type, SqlBuilder subQuery, String alias, String condition) {
        String subQuerySql = "(" + subQuery.build() + ")";
        this.joins.add(new JoinClause(type, subQuerySql, alias, condition));
        this.parameters.putAll(subQuery.parameters);
        return this;
    }

    // WHERE
    public SqlBuilder where(String condition) {
        this.whereConditions.add(new WhereCondition(condition, "AND"));
        return this;
    }

    public SqlBuilder and(String condition) {
        this.whereConditions.add(new WhereCondition(condition, "AND"));
        return this;
    }

    public SqlBuilder or(String condition) {
        this.whereConditions.add(new WhereCondition(condition, "OR"));
        return this;
    }

    // WHERE con parámetros (compatible con JDBI)
    public SqlBuilder where(String condition, Object value) {
        return where(condition).param(extractParamName(condition), value);
    }

    public SqlBuilder and(String condition, Object value) {
        return and(condition).param(extractParamName(condition), value);
    }

    public SqlBuilder or(String condition, Object value) {
        return or(condition).param(extractParamName(condition), value);
    }

    // WHERE con subconsulta
    public SqlBuilder whereExists(SqlBuilder subQuery) {
        String subQuerySql = "EXISTS (" + subQuery.build() + ")";
        this.whereConditions.add(new WhereCondition(subQuerySql, "AND"));
        this.parameters.putAll(subQuery.parameters);
        return this;
    }

    public SqlBuilder whereNotExists(SqlBuilder subQuery) {
        String subQuerySql = "NOT EXISTS (" + subQuery.build() + ")";
        this.whereConditions.add(new WhereCondition(subQuerySql, "AND"));
        this.parameters.putAll(subQuery.parameters);
        return this;
    }

    public SqlBuilder whereIn(String field, SqlBuilder subQuery) {
        String subQuerySql = field + " IN (" + subQuery.build() + ")";
        this.whereConditions.add(new WhereCondition(subQuerySql, "AND"));
        this.parameters.putAll(subQuery.parameters);
        return this;
    }

    public SqlBuilder whereNotIn(String field, SqlBuilder subQuery) {
        String subQuerySql = field + " NOT IN (" + subQuery.build() + ")";
        this.whereConditions.add(new WhereCondition(subQuerySql, "AND"));
        this.parameters.putAll(subQuery.parameters);
        return this;
    }

    // GROUP BY
    public SqlBuilder groupBy(String... fields) {
        this.groupByFields.addAll(Arrays.asList(fields));
        return this;
    }

    // HAVING
    public SqlBuilder having(String condition) {
        this.havingConditions.add(new WhereCondition(condition, "AND"));
        return this;
    }

    public SqlBuilder havingAnd(String condition) {
        this.havingConditions.add(new WhereCondition(condition, "AND"));
        return this;
    }

    public SqlBuilder havingOr(String condition) {
        this.havingConditions.add(new WhereCondition(condition, "OR"));
        return this;
    }

    // ORDER BY
    public SqlBuilder orderBy(String field) {
        this.orderByFields.add(new OrderByClause(field, OrderDirection.ASC));
        return this;
    }

    public SqlBuilder orderBy(String field, OrderDirection direction) {
        this.orderByFields.add(new OrderByClause(field, direction));
        return this;
    }

    public SqlBuilder orderByAsc(String field) {
        return orderBy(field, OrderDirection.ASC);
    }

    public SqlBuilder orderByDesc(String field) {
        return orderBy(field, OrderDirection.DESC);
    }

    // LIMIT y OFFSET
    public SqlBuilder limit(int limit) {
        this.limitValue = limit;
        return this;
    }

    public SqlBuilder offset(int offset) {
        this.offsetValue = offset;
        return this;
    }

    public SqlBuilder limit(int limit, int offset) {
        this.limitValue = limit;
        this.offsetValue = offset;
        return this;
    }

    // Parámetros para JDBI
    public SqlBuilder param(String name, Object value) {
        this.parameters.put(name, value);
        return this;
    }

    public SqlBuilder params(Map<String, Object> params) {
        this.parameters.putAll(params);
        return this;
    }

    // Método para obtener parámetros (útil para JDBI)
    public Map<String, Object> getParameters() {
        return new HashMap<>(parameters);
    }

    // Construcción del SQL
    public String build() {
        StringBuilder sql = new StringBuilder();

        // SELECT
        sql.append("SELECT ");
        if (distinct) {
            sql.append("DISTINCT ");
        }

        if (selectFields.isEmpty()) {
            sql.append("*");
        } else {
            sql.append(selectFields.stream()
                    .map(SelectField::toSql)
                    .collect(Collectors.joining(", ")));
        }

        // FROM
        if (fromTable != null) {
            sql.append(" FROM ").append(fromTable);
            if (fromAlias != null) {
                sql.append(" ").append(fromAlias);
            }
        }

        // JOINs
        for (JoinClause join : joins) {
            sql.append(" ").append(join.toSql());
        }

        // WHERE
        if (!whereConditions.isEmpty()) {
            sql.append(" WHERE ");
            for (int i = 0; i < whereConditions.size(); i++) {
                WhereCondition condition = whereConditions.get(i);
                if (i > 0) {
                    sql.append(" ").append(condition.getOperator()).append(" ");
                }
                sql.append(condition.getCondition());
            }
        }

        // GROUP BY
        if (!groupByFields.isEmpty()) {
            sql.append(" GROUP BY ").append(String.join(", ", groupByFields));
        }

        // HAVING
        if (!havingConditions.isEmpty()) {
            sql.append(" HAVING ");
            for (int i = 0; i < havingConditions.size(); i++) {
                WhereCondition condition = havingConditions.get(i);
                if (i > 0) {
                    sql.append(" ").append(condition.getOperator()).append(" ");
                }
                sql.append(condition.getCondition());
            }
        }

        // ORDER BY
        if (!orderByFields.isEmpty()) {
            sql.append(" ORDER BY ");
            sql.append(orderByFields.stream()
                    .map(OrderByClause::toSql)
                    .collect(Collectors.joining(", ")));
        }

        // LIMIT
        if (limitValue != null) {
            sql.append(" LIMIT ").append(limitValue);
        }

        // OFFSET
        if (offsetValue != null) {
            sql.append(" OFFSET ").append(offsetValue);
        }

        return sql.toString();
    }

    // Método de utilidad para extraer nombres de parámetros
    private String extractParamName(String condition) {
        // Busca patrones como :paramName o ?paramName
        if (condition.contains(":")) {
            String[] parts = condition.split(":");
            if (parts.length > 1) {
                String paramPart = parts[1].split("\\s+")[0];
                return paramPart.replaceAll("[^a-zA-Z0-9_]", "");
            }
        }
        return "param" + System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return build();
    }

    // Clase para construcción de consultas UPDATE
    public static class UpdateBuilder {
        private String table;
        private final Map<String, Object> setValues = new HashMap<>();
        private final List<WhereCondition> whereConditions = new ArrayList<>();
        private final Map<String, Object> parameters = new HashMap<>();

        private UpdateBuilder(String table) {
            this.table = table;
        }

        public static UpdateBuilder update(String table) {
            return new UpdateBuilder(table);
        }

        public UpdateBuilder set(String field, Object value) {
            setValues.put(field, value);
            return this;
        }

        public UpdateBuilder set(Map<String, Object> values) {
            setValues.putAll(values);
            return this;
        }

        public UpdateBuilder where(String condition) {
            whereConditions.add(new WhereCondition(condition, "AND"));
            return this;
        }

        public UpdateBuilder where(String condition, Object value) {
            return where(condition).param(extractParamName(condition), value);
        }

        public UpdateBuilder and(String condition) {
            whereConditions.add(new WhereCondition(condition, "AND"));
            return this;
        }

        public UpdateBuilder param(String name, Object value) {
            parameters.put(name, value);
            return this;
        }

        public Map<String, Object> getParameters() {
            Map<String, Object> allParams = new HashMap<>(parameters);
            setValues.forEach((k, v) -> allParams.put(k, v));
            return allParams;
        }

        public String build() {
            StringBuilder sql = new StringBuilder("UPDATE ").append(table).append(" SET ");

            sql.append(setValues.keySet().stream()
                    .map(field -> field + " = :" + field)
                    .collect(Collectors.joining(", ")));

            if (!whereConditions.isEmpty()) {
                sql.append(" WHERE ");
                for (int i = 0; i < whereConditions.size(); i++) {
                    WhereCondition condition = whereConditions.get(i);
                    if (i > 0) {
                        sql.append(" ").append(condition.getOperator()).append(" ");
                    }
                    sql.append(condition.getCondition());
                }
            }

            return sql.toString();
        }

        private String extractParamName(String condition) {
            if (condition.contains(":")) {
                String[] parts = condition.split(":");
                if (parts.length > 1) {
                    String paramPart = parts[1].split("\\s+")[0];
                    return paramPart.replaceAll("[^a-zA-Z0-9_]", "");
                }
            }
            return "param" + System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return build();
        }
    }

    // Clase para construcción de consultas DELETE
    public static class DeleteBuilder {
        private String table;
        private final List<WhereCondition> whereConditions = new ArrayList<>();
        private final Map<String, Object> parameters = new HashMap<>();

        private DeleteBuilder(String table) {
            this.table = table;
        }

        public static DeleteBuilder deleteFrom(String table) {
            return new DeleteBuilder(table);
        }

        public DeleteBuilder where(String condition) {
            whereConditions.add(new WhereCondition(condition, "AND"));
            return this;
        }

        public DeleteBuilder where(String condition, Object value) {
            return where(condition).param(extractParamName(condition), value);
        }

        public DeleteBuilder and(String condition) {
            whereConditions.add(new WhereCondition(condition, "AND"));
            return this;
        }

        public DeleteBuilder param(String name, Object value) {
            parameters.put(name, value);
            return this;
        }

        public Map<String, Object> getParameters() {
            return new HashMap<>(parameters);
        }

        public String build() {
            StringBuilder sql = new StringBuilder("DELETE FROM ").append(table);

            if (!whereConditions.isEmpty()) {
                sql.append(" WHERE ");
                for (int i = 0; i < whereConditions.size(); i++) {
                    WhereCondition condition = whereConditions.get(i);
                    if (i > 0) {
                        sql.append(" ").append(condition.getOperator()).append(" ");
                    }
                    sql.append(condition.getCondition());
                }
            }

            return sql.toString();
        }

        private String extractParamName(String condition) {
            if (condition.contains(":")) {
                String[] parts = condition.split(":");
                if (parts.length > 1) {
                    String paramPart = parts[1].split("\\s+")[0];
                    return paramPart.replaceAll("[^a-zA-Z0-9_]", "");
                }
            }
            return "param" + System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return build();
        }
    }

    // Clase para construcción de consultas INSERT
    public static class InsertBuilder {
        private String table;
        private final List<String> columns = new ArrayList<>();
        private final List<Map<String, Object>> valuesList = new ArrayList<>();
        private final Map<String, Object> parameters = new HashMap<>();

        private InsertBuilder(String table) {
            this.table = table;
        }

        public static InsertBuilder insertInto(String table) {
            return new InsertBuilder(table);
        }

        public InsertBuilder columns(String... cols) {
            columns.addAll(Arrays.asList(cols));
            return this;
        }

        public InsertBuilder values(Object... vals) {
            if (vals.length != columns.size()) {
                throw new IllegalArgumentException("Number of values must match number of columns");
            }

            Map<String, Object> valueMap = new HashMap<>();
            for (int i = 0; i < columns.size(); i++) {
                valueMap.put(columns.get(i), vals[i]);
            }
            valuesList.add(valueMap);
            return this;
        }

        public InsertBuilder values(Map<String, Object> valueMap) {
            if (columns.isEmpty()) {
                columns.addAll(valueMap.keySet());
            }
            valuesList.add(new HashMap<>(valueMap));
            return this;
        }

        public Map<String, Object> getParameters() {
            Map<String, Object> allParams = new HashMap<>(parameters);
            for (int i = 0; i < valuesList.size(); i++) {
                Map<String, Object> values = valuesList.get(i);
                for (String column : columns) {
                    String paramName = column + (i > 0 ? "_" + i : "");
                    allParams.put(paramName, values.get(column));
                }
            }
            return allParams;
        }

        public String build() {
            if (columns.isEmpty() || valuesList.isEmpty()) {
                throw new IllegalStateException("Must specify columns and values");
            }

            StringBuilder sql = new StringBuilder("INSERT INTO ").append(table);
            sql.append(" (").append(String.join(", ", columns)).append(")");
            sql.append(" VALUES ");

            List<String> valueClauses = new ArrayList<>();
            for (int i = 0; i < valuesList.size(); i++) {
                int finalI = i;
                String valueClause = columns.stream()
                        .map(col -> ":" + col + (finalI > 0 ? "_" + finalI : ""))
                        .collect(Collectors.joining(", ", "(", ")"));
                valueClauses.add(valueClause);
            }

            sql.append(String.join(", ", valueClauses));
            return sql.toString();
        }

        @Override
        public String toString() {
            return build();
        }
    }
}