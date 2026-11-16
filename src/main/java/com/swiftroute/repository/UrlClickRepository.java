package com.swiftroute.repository;

import com.swiftroute.entity.UrlClick;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UrlClickRepository extends JpaRepository<UrlClick, Long> {
    long countByUrlId(Long urlId);
    Page<UrlClick> findByUrlId(Long urlId, Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM UrlClick c WHERE c.url.id = :urlId")
    void deleteByUrlId(Long urlId);
}
