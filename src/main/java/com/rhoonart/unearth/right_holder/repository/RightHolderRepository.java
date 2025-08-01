package com.rhoonart.unearth.right_holder.repository;

import com.rhoonart.unearth.right_holder.entity.HolderType;
import com.rhoonart.unearth.right_holder.entity.RightHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
          ORDER BY r.createdAt DESC
      """)
  Page<RightHolder> search(
      @Param("holderType") HolderType holderType,
      @Param("holderName") String holderName,
      @Param("contractDate") LocalDate contractDate,
      Pageable pageable);

  boolean existsByHolderName(String holderName);

  @Query("SELECT rh.holderName FROM RightHolder rh ORDER BY rh.holderName")
  List<String> findAllHolderNames();

  Optional<RightHolder> findByHolderName(String holderName);

  Optional<RightHolder> findByUserId(String userId);

  boolean existsByBusinessNumber(String businessNumber);

}