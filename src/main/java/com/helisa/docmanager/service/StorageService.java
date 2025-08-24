package com.helisa.docmanager.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.nio.file.Path;

public interface StorageService {
    String guardarArchivo(MultipartFile file, String prefijo) throws Exception;
    InputStream leerArchivo(String path) throws Exception;
    void borrarArchivo(String path);
    String generarNombreSeguro(String originalName);
    boolean validarExtension(String fileName, String[] extensionesPermitidas);
    boolean validarTamano(long size, long maxSize);
    String detectarMimeType(MultipartFile file);
}
