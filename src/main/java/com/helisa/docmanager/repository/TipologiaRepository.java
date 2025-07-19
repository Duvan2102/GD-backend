package com.helisa.docmanager.repository;

import com.helisa.docmanager.model.Cargo;
import com.helisa.docmanager.model.Tipologia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipologiaRepository extends JpaRepository<Tipologia, Integer> {

    Optional<Tipologia> findByDescripcion(String descripcion);

    @Query("SELECT t FROM Tipologia t WHERE LOWER(t.descripcion) LIKE LOWER(CONCAT('%', :descripcion, '%'))")

    List<Tipologia> findByDescripcionContainingIgnoreCase(@Param("descripcion") String descripcion);
    boolean existsByDescripcion(String descripcion);

    boolean existsByDescripcionAndCargo(String descripcion, Cargo cargo);

    List<Tipologia> findByCargo(Cargo cargo);

    List<Tipologia> findByCargoIdCargo(Integer idCargo);

    List<Tipologia> findAllByOrderByDescripcionAsc();

    List<Tipologia> findByCargoOrderByDescripcionAsc(Cargo cargo);

    List<Tipologia> findByCargoIdCargoOrderByDescripcionAsc(Integer idCargo);

    @Query("SELECT t FROM Tipologia t WHERE t.cargo.area.idArea = :idArea ORDER BY t.descripcion ASC")
    List<Tipologia> findByAreaIdOrderByDescripcionAsc(@Param("idArea") Integer idArea);

    @Query("SELECT t FROM Tipologia t WHERE t.cargo.area.departamento.idDepartamento = :idDepartamento ORDER BY t.descripcion ASC")
    List<Tipologia> findByDepartamentoIdOrderByDescripcionAsc(@Param("idDepartamento") Integer idDepartamento);

    @Query("SELECT COUNT(t) FROM Tipologia t WHERE t.cargo.idCargo = :idCargo")
    long countByCargoId(@Param("idCargo") Integer idCargo);

    @Query("SELECT COUNT(t) FROM Tipologia t WHERE t.cargo.area.idArea = :idArea")
    long countByAreaId(@Param("idArea") Integer idArea);

    @Query("SELECT COUNT(t) FROM Tipologia t WHERE t.cargo.area.departamento.idDepartamento = :idDepartamento")
    long countByDepartamentoId(@Param("idDepartamento") Integer idDepartamento);

    @Query("SELECT t FROM Tipologia t JOIN FETCH t.cargo c JOIN FETCH c.area a JOIN FETCH a.departamento d ORDER BY t.descripcion ASC")
    List<Tipologia> findAllWithCargoAreaAndDepartamento();

    @Query("SELECT t FROM Tipologia t JOIN FETCH t.cargo c JOIN FETCH c.area a JOIN FETCH a.departamento d WHERE t.idTipologia = :id")
    Optional<Tipologia> findByIdWithCargoAreaAndDepartamento(@Param("id") Integer id);

    @Query("SELECT t FROM Tipologia t JOIN FETCH t.cargo c JOIN FETCH c.area a JOIN FETCH a.departamento d WHERE c.idCargo = :idCargo ORDER BY t.descripcion ASC")
    List<Tipologia> findByCargoIdWithCompleteInfo(@Param("idCargo") Integer idCargo);
}
