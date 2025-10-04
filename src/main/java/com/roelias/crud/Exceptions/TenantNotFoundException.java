package com.roelias.crud.Exceptions;

public class TenantNotFoundException extends TenantException {
    public TenantNotFoundException(String tenantId) {
        super("Tenant not found: " + tenantId);
    }
}
