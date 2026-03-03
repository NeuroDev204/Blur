package org.identityservice.entity;

import java.time.Instant;

import jakarta.persistence.*;

import org.identityservice.enums.SageStateStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class SagaState {
    @Id
    private String sagaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SageStateStatus status;

    @Column(nullable = false, length = 100)
    private String sagaType;

    @Column(nullable = false, length = 100)
    private String lastEvent;

    @Column(nullable = false)
    private Instant updatedAt;
}
