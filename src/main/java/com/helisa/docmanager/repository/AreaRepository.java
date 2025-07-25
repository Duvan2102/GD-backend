package com.helisa.docmanager.repository;

import com.helisa.docmanager.model.Area;
import com.helisa.docmanager.model.Departamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AreaRepository extends JpaRepository<Area, Integer> {

    Optional<Area> findByDescripcion(String descripcion);

    @Query("SELECT a FROM Area a WHERE LOWER(a.descripcion) LIKE LOWER(CONCAT('%', :descripcion, '%'))")
    List<Area> findByDescripcionContainingIgnoreCase(@Param("descripcion") String descripcion);

    boolean existsByDescripcion(String descripcion);

    boolean existsByDescripcionAndDepartamento(String descripcion, Departamento departamento);

    List<Area> findByDepartamento(Departamento departamento);

    List<Area> findByDepartamentoIdDepartamento(Integer idDepartamento);

    List<Area> findAllByOrderByDescripcionAsc();

    List<Area> findByDepartamentoOrderByDescripcionAsc(Departamento departamento);

    List<Area> findByDepartamentoIdDepartamentoOrderByDescripcionAsc(Integer idDepartamento);

    @Query("SELECT COUNT(a) FROM Area a WHERE a.departamento.idDepartamento = :idDepartamento")
    long countByDepartamentoId(@Param("idDepartamento") Integer idDepartamento);
}
