package com.polyshop.common.events;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class EventEnvelope<T> {

    private String eventId;
    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private String correlationId;
    private String causationId;
    private Instant occurredAt;
    private T payload;
    private Map<String, Object> metadata;

    public EventEnvelope() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
    }

    public static <T> EventEnvelope<T> of(
            String eventType,
            String aggregateType,
            String aggregateId,
            T payload) {

        EventEnvelope<T> env = new EventEnvelope<>();
        env.setEventType(eventType);
        env.setAggregateType(aggregateType);
        env.setAggregateId(aggregateId);
        env.setPayload(payload);
        return env;
    }
}
