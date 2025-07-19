package com.helisa.docmanager.repository;

import com.helisa.docmanager.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    /**
     * Buscar usuario por nombre de usuario
     * @param usuario Nombre de usuario
     * @return Optional con el usuario encontrado
     */
    Optional<Usuario> findByUsuario(String usuario);

    /**
     * Buscar usuario por identificación
     * @param identificacion Identificación del usuario
     * @return Optional con el usuario encontrado
     */
    Optional<Usuario> findByIdentificacion(String identificacion);

    /**
     * Verificar si existe un usuario por nombre de usuario
     * @param usuario Nombre de usuario
     * @return true si existe, false si no
     */
    boolean existsByUsuario(String usuario);

    /**
     * Verificar si existe un usuario por identificación
     * @param identificacion Identificación del usuario
     * @return true si existe, false si no
     */
    boolean existsByIdentificacion(String identificacion);

    /**
     * Buscar usuarios por nombre o apellido (búsqueda parcial, insensible a mayúsculas)
     * @param nombres Término de búsqueda para nombres
     * @param apellidos Término de búsqueda para apellidos
     * @return Lista de usuarios encontrados
     */
    List<Usuario> findByNombresContainingIgnoreCaseOrApellidosContainingIgnoreCase(String nombres, String apellidos);

    /**
     * Buscar usuarios por cargo
     * @param cargoId ID del cargo
     * @return Lista de usuarios con el cargo especificado
     */
    List<Usuario> findByCargo_IdCargo(Integer cargoId);

    /**
     * Buscar usuarios por correo empresarial
     * @param correoEmpresarial Correo empresarial
     * @return Optional con el usuario encontrado
     */
    Optional<Usuario> findByCorreoEmpresarial(String correoEmpresarial);

    /**
     * Buscar usuarios por correo personal
     * @param correoPersonal Correo personal
     * @return Optional con el usuario encontrado
     */
    Optional<Usuario> findByCorreoPersonal(String correoPersonal);

    /**
     * Buscar usuarios que tengan doble autenticación habilitada
     * @param dobleAutenticacion Estado de doble autenticación
     * @return Lista de usuarios con doble autenticación habilitada/deshabilitada
     */
    List<Usuario> findByDobleAutenticacion(Boolean dobleAutenticacion);

    /**
     * Buscar usuarios por nombre completo (nombres + apellidos)
     * @param termino Término de búsqueda
     * @return Lista de usuarios encontrados
     */
    @Query("SELECT u FROM Usuario u WHERE " +
            "LOWER(CONCAT(u.nombres, ' ', u.apellidos)) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
            "LOWER(CONCAT(u.apellidos, ' ', u.nombres)) LIKE LOWER(CONCAT('%', :termino, '%'))")
    List<Usuario> findByNombreCompleto(@Param("termino") String termino);

    /**
     * Contar usuarios por cargo
     * @param cargoId ID del cargo
     * @return Número de usuarios con el cargo especificado
     */
    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.cargo.idCargo = :cargoId")
    Long countByCargo(@Param("cargoId") Integer cargoId);
}
