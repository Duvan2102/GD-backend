package com.helisa.docmanager.repository;

import com.helisa.docmanager.model.Estado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoRepository extends JpaRepository<Estado, Integer> {

    Optional<Estado> findByDescripcion(String descripcion);

    // Métodos de conveniencia para obtener estados específicos
    default Estado getEstadoPendiente() {
        return findById(1).orElseThrow(() ->
                new IllegalStateException("Estado PENDIENTE no encontrado en BD"));
    }

    default Estado getEstadoAprobado() {
        return findById(2).orElseThrow(() ->
                new IllegalStateException("Estado APROBADO no encontrado en BD"));
    }

    default Estado getEstadoRechazado() {
        return findById(3).orElseThrow(() ->
                new IllegalStateException("Estado RECHAZADO no encontrado en BD"));
    }

    default Estado getEstadoCancelado() {
        return findById(4).orElseThrow(() ->
                new IllegalStateException("Estado CANCELADO no encontrado en BD"));
    }
    
    default Estado getEstadoaAprobPendiente() {
        return findById(9).orElseThrow(() ->
                new IllegalStateException("Estado APROBADO PENDIENTE no encontrado en BD"));
    }
}
