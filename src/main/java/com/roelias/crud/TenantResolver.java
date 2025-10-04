package com.roelias.crud;

@FunctionalInterface
public interface TenantResolver {

    /**
     * Resuelve el tenant ID desde el contexto actual
     * Puede ser desde HTTP request, JMS message, etc.
     */
    String resolveTenantId();

}
