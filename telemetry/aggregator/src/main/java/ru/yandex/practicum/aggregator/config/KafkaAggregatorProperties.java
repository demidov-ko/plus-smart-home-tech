package ru.yandex.practicum.aggregator.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "kafka.aggregator")
public class KafkaAggregatorProperties {
    private String groupId;
    private String autoOffsetReset = "earliest";
    private boolean enableAutoCommit = false;
}
