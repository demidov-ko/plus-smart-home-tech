package ru.yandex.practicum.analyzer.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.analyzer.deserializer.HubEventDeserializer;
import ru.yandex.practicum.analyzer.deserializer.SnapshotDeserializer;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.Properties;

@Configuration
@EnableConfigurationProperties(KafkaConsumerProperties.class)
public class KafkaClientConfig {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaConsumer<String, HubEventAvro> hubEventConsumer(KafkaConsumerProperties consumerProps) {
        Properties props = buildBaseProps(consumerProps, consumerProps.getHubEventsGroupId());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, HubEventDeserializer.class);
        return new KafkaConsumer<>(props);
    }

    @Bean
    public KafkaConsumer<String, SensorsSnapshotAvro> snapshotConsumer(KafkaConsumerProperties consumerProps) {
        Properties props = buildBaseProps(consumerProps, consumerProps.getSnapshotsGroupId());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, SnapshotDeserializer.class);
        return new KafkaConsumer<>(props);
    }

    private Properties buildBaseProps(KafkaConsumerProperties consumerProps, String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerProps.getAutoOffsetReset());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, consumerProps.isEnableAutoCommit());
        return props;
    }
}