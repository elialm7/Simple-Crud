package com.roelias.crud;

import java.util.Objects;

public class TenantConnection {
    private final String tenantId;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String driverClassName;

    public TenantConnection(String tenantId, String jdbcUrl, String username,
                            String password, String driverClassName) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl cannot be null");
        this.username = Objects.requireNonNull(username, "username cannot be null");
        this.password = Objects.requireNonNull(password, "password cannot be null");
        this.driverClassName = Objects.requireNonNull(driverClassName, "driverClassName cannot be null");
    }

    // Getters (inmutabilidad)
    public String getTenantId() { return tenantId; }
    public String getJdbcUrl() { return jdbcUrl; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getDriverClassName() { return driverClassName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TenantConnection that = (TenantConnection) o;
        return tenantId.equals(that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId);
    }

    @Override
    public String toString() {
        return "TenantConnection{" +
                "tenantId='" + tenantId + '\'' +
                ", jdbcUrl='" + jdbcUrl + '\'' +
                ", username='" + username + '\'' +
                ", driverClassName='" + driverClassName + '\'' +
                '}';
    }
}
