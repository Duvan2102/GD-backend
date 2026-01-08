package com.helisa.docmanager.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "storage")
@Data
public class StorageProperties {
    private String basePath = "/opt/gd/files";
    private long maxPdfBytes = 52428800L;
    private long maxAttachmentBytes = 10485760L;
    private String[] allowedAttachmentExt = {"jpg", "png", "pdf", "docx", "xlsx", "txt"};
}
