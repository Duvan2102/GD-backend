package com.helisa.docmanager.repository;

import com.helisa.docmanager.model.Tipologia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipologiaRepository extends JpaRepository<Tipologia, Integer> {
}
