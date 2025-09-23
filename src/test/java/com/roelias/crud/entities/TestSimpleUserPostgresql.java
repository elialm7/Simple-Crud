package com.roelias.crud.entities;

import com.fasterxml.jackson.databind.JsonNode;
import com.roelias.crud.CRUD;

import java.time.LocalDateTime;
import java.util.List;

public class TestSimpleUserPostgresql {


    // Basic column mapping
    @CRUD.Column("entity_name")
    private String name;

    // UUID auto-generation
    @CRUD.UUID(autoGenerate = true)
    @CRUD.Column("uuid_field")
    private String uuid;

    // Manual UUID (when you want to provide it)
    @CRUD.UUID(autoGenerate = false)
    @CRUD.Column("manual_uuid")
    private String manualUuid;


    @CRUD.JsonColumn
    @CRUD.Column("raw_json")
    private JsonNode rawJson;  // JsonNode

    // Array handling
    @CRUD.ArrayColumn(separator = ",")
    private List<String> tags;

    // Enum handling - STRING mode
    @CRUD.EnumColumn(CRUD.EnumColumn.EnumType.STRING)
    private Status status;

    // Enum handling - ORDINAL mode
    @CRUD.EnumColumn(CRUD.EnumColumn.EnumType.ORDINAL)
    private Priority priority;

    // Enum handling - CODE mode (requires getCode() method)
    @CRUD.EnumColumn(CRUD.EnumColumn.EnumType.CODE)
    private Department department;


    // Default values
    @CRUD.Default("true")
    private Boolean active;

    @CRUD.Default("0")
    private Integer attempts;

    // Automatic timestamps
    @CRUD.CreatedDate
    @CRUD.Column("created_at")
    private LocalDateTime createdAt;

    @CRUD.UpdatedDate
    @CRUD.Column("updated_at")
    private LocalDateTime updatedAt;


    @Override
    public String toString() {
        return "TestSimpleUserPostgresql{" +
                "name='" + name + '\'' +
                ", uuid='" + uuid + '\'' +
                ", manualUuid='" + manualUuid + '\'' +
                ", rawJson=" + rawJson +
                ", tags=" + tags +
                ", status=" + status +
                ", priority=" + priority +
                ", department=" + department +
                ", active=" + active +
                ", attempts=" + attempts +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getManualUuid() {
        return manualUuid;
    }

    public void setManualUuid(String manualUuid) {
        this.manualUuid = manualUuid;
    }

    public JsonNode getRawJson() {
        return rawJson;
    }

    public void setRawJson(JsonNode rawJson) {
        this.rawJson = rawJson;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
