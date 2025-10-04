package com.roelias.crud.Exceptions;

public class TenantContextException extends TenantException{
    public TenantContextException() {
        super("No tenant context set for current request");
    }
}
