package com.helisa.docmanager.repository;

import com.helisa.docmanager.model.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CargoRepository extends JpaRepository<Cargo, Integer> {

    List<Cargo> findByArea_IdArea(Integer areaId);
    List<Cargo> findByDescripcionContainingIgnoreCase(String descripcion);
    boolean existsByDescripcion(String descripcion);
}
