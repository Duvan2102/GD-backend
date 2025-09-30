package com.helisa.docmanager.repository;

import com.helisa.docmanager.model.Token;
import com.helisa.docmanager.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Integer> {
    
    // Buscar token válido por usuario y tipo (el más reciente)
    @Query("SELECT t FROM Token t WHERE t.usuario = :usuario AND t.tipoValidacion = :tipo AND t.fechaExp > :now ORDER BY t.fechaExp DESC")
    Optional<Token> findValidTokenByUsuarioAndTipo(@Param("usuario") Usuario usuario, 
                                                   @Param("tipo") String tipo, 
                                                   @Param("now") LocalDateTime now);
    
    // Buscar todos los tokens válidos por usuario y tipo ordenados por fecha de expiración descendente
    @Query("SELECT t FROM Token t WHERE t.usuario = :usuario AND t.tipoValidacion = :tipo AND t.fechaExp > :now ORDER BY t.fechaExp DESC")
    List<Token> findValidTokensByUsuarioAndTipoOrderByFechaExpDesc(@Param("usuario") Usuario usuario, 
                                                                    @Param("tipo") String tipo, 
                                                                    @Param("now") LocalDateTime now);
    
    // Buscar token por código y tipo
    @Query("SELECT t FROM Token t WHERE t.codigo = :codigo AND t.tipoValidacion = :tipo AND t.fechaExp > :now")
    Optional<Token> findValidTokenByCodigoAndTipo(@Param("codigo") String codigo, 
                                                  @Param("tipo") String tipo, 
                                                  @Param("now") LocalDateTime now);
    
    // Eliminar tokens expirados
    @Modifying
    @Transactional
    @Query("DELETE FROM Token t WHERE t.fechaExp < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    // Buscar todos los tokens de un usuario
    List<Token> findByUsuarioOrderByFechaExpDesc(Usuario usuario);
    
    // Contar intentos de token por usuario en un período
    @Query("SELECT COUNT(t) FROM Token t WHERE t.usuario = :usuario AND t.tipoValidacion = :tipo AND t.fechaExp > :since")
    Long countTokensByUsuarioAndTipoSince(@Param("usuario") Usuario usuario, 
                                          @Param("tipo") String tipo, 
                                          @Param("since") LocalDateTime since);

    /**
     * Busca tokens válidos por usuario y tipo (sobrecarga con String)
     */
    @Query("SELECT t FROM Token t WHERE t.usuario.usuario = :usuario AND t.tipoValidacion = :tipo AND t.fechaExp > :now")
    Optional<Token> findValidTokenByUsuarioAndTipo(@Param("usuario") String usuario, @Param("tipo") String tipo, @Param("now") LocalDateTime now);

    /**
     * Elimina tokens por usuario y tipo
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Token t WHERE t.usuario.usuario = :usuario AND t.tipoValidacion = :tipo")
    void deleteByUsuarioAndTipo(@Param("usuario") String usuario, @Param("tipo") String tipo);
}
