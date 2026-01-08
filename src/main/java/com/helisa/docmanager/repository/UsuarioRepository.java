package com.helisa.docmanager.repository;

import com.helisa.docmanager.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByUsuario(String usuario);
    List<Usuario> findByIdUsuarioIn(Collection<Integer> ids);

    Optional<Usuario> findByIdentificacion(String identificacion);

    boolean existsByUsuario(String usuario);

    boolean existsByIdentificacion(String identificacion);

    List<Usuario> findByNombresContainingIgnoreCaseOrApellidosContainingIgnoreCase(String nombres, String apellidos);

    List<Usuario> findByCargo_IdCargo(Integer cargoId);

    Optional<Usuario> findByCorreoEmpresarial(String correoEmpresarial);

    boolean existsByCorreoEmpresarial(String correoEmpresarial);

    Optional<Usuario> findByCorreoPersonal(String correoPersonal);

    @Query("SELECT u FROM Usuario u WHERE " +
            "LOWER(CONCAT(u.nombres, ' ', u.apellidos)) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
            "LOWER(CONCAT(u.apellidos, ' ', u.nombres)) LIKE LOWER(CONCAT('%', :termino, '%'))")
    List<Usuario> findByNombreCompleto(@Param("termino") String termino);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.cargo.idCargo = :cargoId")
    Long countByCargo(@Param("cargoId") Integer cargoId);

    Page<Usuario> findAll(Pageable pageable);

    List<Usuario> findByEstado(com.helisa.docmanager.model.Estado estado);
}
