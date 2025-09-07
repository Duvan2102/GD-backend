package com.helisa.docmanager.repository;

import com.helisa.docmanager.model.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {
    boolean existsByJti(String jti);
    long deleteByExpiresAtBefore(LocalDateTime dateTime);
}

