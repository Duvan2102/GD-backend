package com.helisa.docmanager.util;

import java.text.Normalizer;

/**
 * Utilidades para manipulación de cadenas de texto
 */
public class StringUtils {

    /**
     * Normaliza una cadena de texto eliminando acentos y convirtiéndola a minúsculas
     * para comparaciones insensibles a acentos y mayúsculas/minúsculas.
     * 
     * Ejemplos:
     * - "Tecnología" -> "tecnologia"
     * - "DESARROLLO" -> "desarrollo"
     * - "Área" -> "area"
     * 
     * @param text Texto a normalizar
     * @return Texto normalizado sin acentos y en minúsculas
     */
    public static String normalizeForComparison(String text) {
        if (text == null) {
            return null;
        }
        
        // Eliminar espacios al inicio y final
        String normalized = text.trim();
        
        // Convertir a minúsculas
        normalized = normalized.toLowerCase();
        
        // Eliminar acentos usando NFD (Canonical Decomposition)
        // Luego eliminar los caracteres diacríticos
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");
        
        return normalized;
    }
    
    /**
     * Compara dos cadenas de texto de forma normalizada
     * (insensible a acentos y mayúsculas/minúsculas)
     * 
     * @param text1 Primera cadena
     * @param text2 Segunda cadena
     * @return true si son iguales después de normalizar, false en caso contrario
     */
    public static boolean equalsNormalized(String text1, String text2) {
        if (text1 == null && text2 == null) {
            return true;
        }
        if (text1 == null || text2 == null) {
            return false;
        }
        return normalizeForComparison(text1).equals(normalizeForComparison(text2));
    }
}

