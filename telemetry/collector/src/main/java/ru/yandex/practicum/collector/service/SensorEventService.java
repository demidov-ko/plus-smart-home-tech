package ru.yandex.practicum.collector.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.mapper.sensor.SensorEventMapper;
import ru.yandex.practicum.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.collector.model.sensor.SensorEventType;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SensorEventService {


    private final Map<SensorEventType, SensorEventMapper> mappers;
    private final KafkaEventProducer producer;

    public SensorEventService(java.util.List<SensorEventMapper> mapperList, KafkaEventProducer producer) {
        this.mappers = mapperList.stream()
                .collect(Collectors.toMap(SensorEventMapper::getType, Function.identity()));
        this.producer = producer;
    }

    public void collect(SensorEvent event) {
        SensorEventMapper mapper = mappers.get(event.getType());
        if (mapper == null) {
            throw new IllegalArgumentException("Не найден маппер для типа события: " + event.getType());
        }
        SensorEventAvro avroEvent = mapper.map(event);
        producer.sendSensorEvent(avroEvent);
    }
}
