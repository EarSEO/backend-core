package com.earseo.core.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaEvent<T> {
    private String eventId;
    private String eventType;
    private String eventTime;
    private T data;
}
