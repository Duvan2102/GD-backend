package com.helisa.docmanager.repository;

import com.helisa.docmanager.model.Departamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartamentoRepository extends JpaRepository<Departamento, Integer> {

    Optional<Departamento> findByDescripcion(String descripcion);

    @Query("SELECT d FROM Departamento d WHERE LOWER(d.descripcion) LIKE LOWER(CONCAT('%', :descripcion, '%'))")
    List<Departamento> findByDescripcionContainingIgnoreCase(@Param("descripcion") String descripcion);

    boolean existsByDescripcion(String descripcion);

    List<Departamento> findAllByOrderByDescripcionAsc();
}
