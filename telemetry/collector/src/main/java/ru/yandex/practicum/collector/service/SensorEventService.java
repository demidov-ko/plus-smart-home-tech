package ru.yandex.practicum.collector.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.mapper.sensor.SensorEventMapper;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SensorEventService {
    private final Map<SensorEventProto.PayloadCase, SensorEventMapper> mappers;
    private final KafkaEventProducer producer;

    public SensorEventService(List<SensorEventMapper> mapperList, KafkaEventProducer producer) {
        this.mappers = mapperList.stream()
                .collect(Collectors.toMap(SensorEventMapper::getPayloadCase, Function.identity()));
        this.producer = producer;
    }

    public void collect(SensorEventProto event) {
        SensorEventMapper mapper = mappers.get(event.getPayloadCase());
        if (mapper == null) {
            throw new IllegalArgumentException("Не найден маппер для типа события: " + event.getPayloadCase());
        }
        SensorEventAvro avroEvent = mapper.map(event);
        producer.sendSensorEvent(avroEvent);
    }
}
