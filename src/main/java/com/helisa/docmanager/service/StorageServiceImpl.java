package com.helisa.docmanager.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.codec.digest.DigestUtils;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

@Service
@Slf4j
public class StorageServiceImpl implements StorageService {

    @Value("${storage.base-path:/opt/gd/files}")
    private String basePath;

    @Value("${storage.max-pdf-bytes:52428800}")
    private long maxPdfBytes;

    @Value("${storage.max-attachment-bytes:10485760}")
    private long maxAttachmentBytes;

    @Value("#{'${storage.allowed-attachment-ext:jpg,png,pdf,docx,xlsx,txt}'.split(',')}")
    private String[] allowedExtensions;

    @Override
    public String guardarArchivo(MultipartFile file, String prefijo) throws Exception {
        String nombreSeguro = generarNombreSeguro(file.getOriginalFilename());
        String nombreFinal = prefijo + "_" + nombreSeguro;

        Path rutaCompleta = Paths.get(basePath, nombreFinal);
        Files.createDirectories(rutaCompleta.getParent());

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, rutaCompleta, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Archivo guardado: {}", rutaCompleta);
        return nombreFinal;
    }

    @Override
    public InputStream leerArchivo(String path) throws Exception {
        Path rutaCompleta = Paths.get(basePath, path);
        if (!Files.exists(rutaCompleta)) {
            throw new FileNotFoundException("Archivo no encontrado: " + path);
        }
        return Files.newInputStream(rutaCompleta);
    }

    @Override
    public void borrarArchivo(String path) {
        try {
            Path rutaCompleta = Paths.get(basePath, path);
            Files.deleteIfExists(rutaCompleta);
            log.info("Archivo borrado: {}", rutaCompleta);
        } catch (Exception e) {
            log.error("Error al borrar archivo: {}", path, e);
        }
    }

    @Override
    public String generarNombreSeguro(String originalName) {
        String extension = FilenameUtils.getExtension(originalName);
        String hash = DigestUtils.md5Hex(originalName + System.currentTimeMillis());
        return hash + "." + extension;
    }

    @Override
    public boolean validarExtension(String fileName, String[] extensionesPermitidas) {
        String extension = FilenameUtils.getExtension(fileName).toLowerCase();
        return Arrays.stream(extensionesPermitidas)
                .anyMatch(ext -> ext.toLowerCase().equals(extension));
    }

    @Override
    public boolean validarTamano(long size, long maxSize) {
        return size <= maxSize;
    }

    @Override
    public String detectarMimeType(MultipartFile file) {
        try {
            return Files.probeContentType(Paths.get(file.getOriginalFilename()));
        } catch (Exception e) {
            return file.getContentType();
        }
    }
}
