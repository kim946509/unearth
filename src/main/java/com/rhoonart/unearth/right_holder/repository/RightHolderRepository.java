package com.rhoonart.unearth.right_holder.repository;

import com.rhoonart.unearth.right_holder.entity.HolderType;
import com.rhoonart.unearth.right_holder.entity.RightHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface RightHolderRepository
        extends org.springframework.data.jpa.repository.JpaRepository<RightHolder, String> {
    @Query("""
                SELECT r FROM RightHolder r
                WHERE (:holderType IS NULL OR r.holderType = :holderType)
                  AND (:holderName IS NULL OR r.holderName LIKE CONCAT(:holderName, '%'))
                  AND (
                    :contractDate IS NULL
                    OR (r.contractStart <= :contractDate AND r.contractEnd >= :contractDate)
                  )
                ORDER BY
                  CASE WHEN :holderName IS NOT NULL AND r.holderName LIKE CONCAT(:holderName, '%') THEN 0 ELSE 1 END,
                  r.contractStart DESC,
                  r.holderName ASC
            """)
    Page<RightHolder> search(
            @Param("holderType") HolderType holderType,
            @Param("holderName") String holderName,
            @Param("contractDate") LocalDate contractDate,
            Pageable pageable);

    boolean existsByHolderName(String holderName);
}