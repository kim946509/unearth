package com.rhoonart.unearth.right_holder.entity;

import com.rhoonart.unearth.global.BaseEntity;
import com.rhoonart.unearth.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.JoinColumn;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "right_holder")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RightHolder extends BaseEntity {
    @Id
    @UuidGenerator
    @Column(length = 36, nullable = false, updatable = false, unique = true)
    private String id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "holder_type", nullable = false)
    private HolderType holderType;

    @Column(name = "holder_name", nullable = false, unique = true)
    private String holderName;

    @Column(name = "contract_start", nullable = false)
    private LocalDate contractStart;

    @Column(name = "contract_end", nullable = false)
    private LocalDate contractEnd;

    @Column(name = "business_number", nullable = false)
    private String businessNumber;
}