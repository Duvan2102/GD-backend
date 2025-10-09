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
    
    @Query("SELECT t FROM Token t WHERE t.usuario = :usuario AND t.tipoValidacion = :tipo AND t.fechaExp > :now ORDER BY t.fechaExp DESC")
    Optional<Token> findValidTokenByUsuarioAndTipo(@Param("usuario") Usuario usuario, 
                                                   @Param("tipo") String tipo, 
                                                   @Param("now") LocalDateTime now);
    
    
    @Query("SELECT t FROM Token t WHERE t.usuario = :usuario AND t.tipoValidacion = :tipo AND t.fechaExp > :now ORDER BY t.fechaExp DESC")
    List<Token> findValidTokensByUsuarioAndTipoOrderByFechaExpDesc(@Param("usuario") Usuario usuario, 
                                                                    @Param("tipo") String tipo, 
                                                                    @Param("now") LocalDateTime now);
    
    
    @Query("SELECT t FROM Token t WHERE t.codigo = :codigo AND t.tipoValidacion = :tipo AND t.fechaExp > :now")
    Optional<Token> findValidTokenByCodigoAndTipo(@Param("codigo") String codigo, 
                                                  @Param("tipo") String tipo, 
                                                  @Param("now") LocalDateTime now);
    
    @Query("SELECT t FROM Token t WHERE t.codigo = :codigo AND t.usuario = :usuario AND t.tipoValidacion = :tipo AND t.fechaExp > :now")
    Optional<Token> findValidTokenByCodigoUsuarioAndTipo(@Param("codigo") String codigo,
                                                          @Param("usuario") Usuario usuario,
                                                          @Param("tipo") String tipo, 
                                                          @Param("now") LocalDateTime now);
    
    
    @Modifying
    @Transactional
    @Query("DELETE FROM Token t WHERE t.fechaExp < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    List<Token> findByUsuarioOrderByFechaExpDesc(Usuario usuario);
    
    @Query("SELECT COUNT(t) FROM Token t WHERE t.usuario = :usuario AND t.tipoValidacion = :tipo AND t.fechaExp > :since")
    Long countTokensByUsuarioAndTipoSince(@Param("usuario") Usuario usuario, 
                                          @Param("tipo") String tipo, 
                                          @Param("since") LocalDateTime since);

    
    @Query("SELECT t FROM Token t WHERE t.usuario.usuario = :usuario AND t.tipoValidacion = :tipo AND t.fechaExp > :now")
    Optional<Token> findValidTokenByUsuarioAndTipo(@Param("usuario") String usuario, @Param("tipo") String tipo, @Param("now") LocalDateTime now);

    
    @Modifying
    @Transactional
    @Query("DELETE FROM Token t WHERE t.usuario.usuario = :usuario AND t.tipoValidacion = :tipo")
    void deleteByUsuarioAndTipo(@Param("usuario") String usuario, @Param("tipo") String tipo);

    
    @Query("SELECT COUNT(t) > 0 FROM Token t WHERE t.codigo = :jti AND t.tipoValidacion = 'JWT_REVOKED'")
    boolean existsRevokedJwtByJti(@Param("jti") String jti);
}
