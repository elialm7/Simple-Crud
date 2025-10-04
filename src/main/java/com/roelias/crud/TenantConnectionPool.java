package com.roelias.crud;

import java.util.Optional;
import java.util.Set;

public interface TenantConnectionPool<T> {
    /**
     * Agrega una conexión para un tenant específico
     */
    void addConnection(TenantConnection connection);

    /**
     * Obtiene la conexión para el tenant actual
     */
    Optional<T> getConnection();

    /**
     * Obtiene la conexión para un tenant específico
     */
    Optional<T> getConnection(String tenantId);

    /**
     * Verifica si existe conexión para un tenant
     */
    boolean hasConnection(String tenantId);

    /**
     * Remueve la conexión de un tenant
     */
    void removeConnection(String tenantId);

    /**
     * Obtiene todos los tenantIds configurados
     */
    Set<String> getConfiguredTenants();

    /**
     * Limpia todas las conexiones
     */
    void clear();
}
