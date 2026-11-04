package com.swiftroute.repository;

import com.swiftroute.entity.UrlClick;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UrlClickRepository extends JpaRepository<UrlClick, Long> {
    long countByUrlId(Long urlId);
    Page<UrlClick> findByUrlId(Long urlId, Pageable pageable);
}
