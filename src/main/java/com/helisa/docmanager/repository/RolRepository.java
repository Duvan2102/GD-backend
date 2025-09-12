package com.helisa.docmanager.repository;

import com.helisa.docmanager.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Integer> {
    
    /**
     * Buscar rol por descripción
     * @param descripcion Descripción del rol
     * @return Optional con el rol encontrado
     */
    Optional<Rol> findByDescripcion(String descripcion);
}

