package com.helisa.docmanager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class ExcelAuditRequest {
    
    @JsonProperty("selectedIds")
    @NotNull
    @NotEmpty
    private List<String> selectedIds;
    
    // Getters y Setters
    public List<String> getSelectedIds() {
        return selectedIds;
    }
    
    public void setSelectedIds(List<String> selectedIds) {
        this.selectedIds = selectedIds;
    }
}