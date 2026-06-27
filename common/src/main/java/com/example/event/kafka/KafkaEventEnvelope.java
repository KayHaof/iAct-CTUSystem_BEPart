package com.example.event.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaEventEnvelope<T> {
    private String eventId;
    private Integer eventVersion;
    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private String occurredAt;
    private String producer;
    private KafkaActor actor;
    private T payload;
    private KafkaEventMetadata metadata;
}

