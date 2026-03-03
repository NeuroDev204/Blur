package org.identityservice.entity;

import java.time.Instant;

import jakarta.persistence.*;

import org.identityservice.enums.OutboxStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "outbox_event")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String aggregateType;

    @Column(nullable = false, length = 100)
    private String aggregateId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, columnDefinition = "json")
    private String payload;

    @Column(nullable = false, columnDefinition = "json")
    private String headers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private Instant occurredAt;

    private Instant publishedAt;

    public static OutboxEvent of(
            String aggregateType, String aggregateId, String evenType, String payload, String headers) {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateId(aggregateId);
        event.setAggregateType(aggregateType);
        event.setEventType(evenType);
        event.setHeaders(headers);
        event.setStatus(OutboxStatus.PENDING);
        event.setOccurredAt(Instant.now());
        return event;
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }
}
