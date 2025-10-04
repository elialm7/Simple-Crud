package com.roelias.crud;

import com.roelias.crud.Exceptions.TenantContextException;
import org.apache.poi.ss.formula.functions.T;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractConnectionPool<T> implements TenantConnectionPool<T>{
    protected final Map<String, T> connectionPool = new ConcurrentHashMap<>();
    protected final Map<String, TenantConnection> connectionConfigs = new ConcurrentHashMap<>();

    @Override
    public void addConnection(TenantConnection connection) {
        T connectionObject = createConnectionObject(connection);
        connectionPool.put(connection.getTenantId(), connectionObject);
        connectionConfigs.put(connection.getTenantId(), connection);
    }

    @Override
    public Optional<T> getConnection() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new TenantContextException();
        }
        return getConnection(tenantId);
    }

    @Override
    public Optional<T> getConnection(String tenantId) {
        return Optional.ofNullable(connectionPool.get(tenantId));
    }

    @Override
    public boolean hasConnection(String tenantId) {
        return connectionPool.containsKey(tenantId);
    }

    @Override
    public void removeConnection(String tenantId) {
        T connection = connectionPool.remove(tenantId);
        connectionConfigs.remove(tenantId);
        if (connection != null) {
            closeConnection(connection);
        }
    }

    @Override
    public Set<String> getConfiguredTenants() {
        return connectionPool.keySet();
    }

    @Override
    public void clear() {
        connectionPool.values().forEach(this::closeConnection);
        connectionPool.clear();
        connectionConfigs.clear();
    }

    /**
     * Método abstracto para crear el objeto de conexión específico
     */
    protected abstract T createConnectionObject(TenantConnection connection);

    /**
     * Método para cerrar la conexión (puede ser override)
     */
    protected void closeConnection(T connection) {}
}
