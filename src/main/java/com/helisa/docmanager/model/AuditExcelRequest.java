package com.helisa.docmanager.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AuditExcelRequest {
    
    @NotNull
    @NotEmpty
    private List<String> selectedIds;
}