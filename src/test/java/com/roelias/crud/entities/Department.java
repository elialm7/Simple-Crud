package com.roelias.crud.entities;

public enum Department {
    SALES("SALE"), MARKETING("MKTG"), ENGINEERING("ENG"), HR("HRES");

    private final String code;
    Department(String code) { this.code = code; }
    public String getCode() { return code; }  // Required for CODE mode
}
