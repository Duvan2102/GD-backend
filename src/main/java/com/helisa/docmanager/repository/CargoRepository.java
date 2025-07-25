package com.helisa.docmanager.repository;

import com.helisa.docmanager.model.Area;
import com.helisa.docmanager.model.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CargoRepository extends JpaRepository<Cargo, Integer> {

    Optional<Cargo> findByDescripcion(String descripcion);

    @Query("SELECT c FROM Cargo c WHERE LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :descripcion, '%'))")
    List<Cargo> findByDescripcionContainingIgnoreCase(@Param("descripcion") String descripcion);

    boolean existsByDescripcion(String descripcion);

    boolean existsByDescripcionAndArea(String descripcion, Area area);

    List<Cargo> findByArea(Area area);

    List<Cargo> findByAreaIdArea(Integer idArea);

    List<Cargo> findAllByOrderByDescripcionAsc();

    List<Cargo> findByAreaOrderByDescripcionAsc(Area area);

    List<Cargo> findByAreaIdAreaOrderByDescripcionAsc(Integer idArea);

    @Query("SELECT c FROM Cargo c WHERE c.area.departamento.idDepartamento = :idDepartamento ORDER BY c.descripcion ASC")
    List<Cargo> findByDepartamentoIdOrderByDescripcionAsc(@Param("idDepartamento") Integer idDepartamento);

    @Query("SELECT COUNT(c) FROM Cargo c WHERE c.area.idArea = :idArea")
    long countByAreaId(@Param("idArea") Integer idArea);

    @Query("SELECT COUNT(c) FROM Cargo c WHERE c.area.departamento.idDepartamento = :idDepartamento")
    long countByDepartamentoId(@Param("idDepartamento") Integer idDepartamento);

    @Query("SELECT c FROM Cargo c JOIN FETCH c.area a JOIN FETCH a.departamento d ORDER BY c.descripcion ASC")
    List<Cargo> findAllWithAreaAndDepartamento();

    @Query("SELECT c FROM Cargo c JOIN FETCH c.area a JOIN FETCH a.departamento d WHERE c.idCargo = :id")
    Optional<Cargo> findByIdWithAreaAndDepartamento(@Param("id") Integer id);

}
