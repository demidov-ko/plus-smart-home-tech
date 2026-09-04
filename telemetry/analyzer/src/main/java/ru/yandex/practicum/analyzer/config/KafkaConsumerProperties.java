package ru.yandex.practicum.analyzer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "kafka.consumer")
public class KafkaConsumerProperties {
    private String hubEventsGroupId;
    private String snapshotsGroupId;
    private String autoOffsetReset = "earliest";
    private boolean enableAutoCommit = false;
}