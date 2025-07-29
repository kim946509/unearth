package com.rhoonart.unearth.global;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // // 임시로 사용할 필드 (테스트 데이터용)
    // private LocalDateTime customCreatedAt;

    // public void setCustomCreatedAt(LocalDateTime customCreatedAt) {
    // this.customCreatedAt = customCreatedAt;
    // }

    // @PrePersist
    // protected void onCreate() {
    // if (customCreatedAt != null) {
    // this.createdAt = customCreatedAt;
    // this.updatedAt = customCreatedAt;
    // }
    // }
}