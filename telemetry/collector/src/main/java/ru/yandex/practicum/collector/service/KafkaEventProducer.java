package ru.yandex.practicum.collector.service;

import jakarta.annotation.PreDestroy;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Component
public class KafkaEventProducer {

    @Value("${kafka.topics.sensors}")
    private String sensorsTopic;

    @Value("${kafka.topics.hubs}")
    private String hubsTopic;

    private final KafkaProducer<String, SpecificRecordBase> producer;

    public KafkaEventProducer(KafkaProducer<String, SpecificRecordBase> producer) {
        this.producer = producer;
    }

    public void sendSensorEvent(SensorEventAvro event) {
        send(sensorsTopic, event.getHubId(), event);
    }

    public void sendHubEvent(HubEventAvro event) {
        send(hubsTopic, event.getHubId(), event);
    }

    private void send(String topic, String key, SpecificRecordBase value) {
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(topic, key, value);
        producer.send(record);
    }

    @PreDestroy
    public void close() {
        producer.flush();
        producer.close();
    }
}
