package com.helisa.docmanager.model;

public enum EstadoSolicitudEnum {
    PENDIENTE("PENDIENTE"),
    APROBADO("APROBADO"),
    RECHAZADO("RECHAZADO"),
    CANCELADA("CANCELADA");

    private final String valor;

    EstadoSolicitudEnum(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static EstadoSolicitudEnum fromString(String text) {
        for (EstadoSolicitudEnum estado : EstadoSolicitudEnum.values()) {
            if (estado.valor.equalsIgnoreCase(text)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado no válido: " + text);
    }
}
