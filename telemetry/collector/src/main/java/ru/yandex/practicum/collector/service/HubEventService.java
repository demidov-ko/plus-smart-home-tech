package ru.yandex.practicum.collector.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.mapper.hub.HubEventMapper;
import ru.yandex.practicum.collector.model.hub.HubEvent;
import ru.yandex.practicum.collector.model.hub.types.HubEventType;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HubEventService {

    private final Map<HubEventType, HubEventMapper> mappers;
    private final KafkaEventProducer producer;

    public HubEventService(List<HubEventMapper> mapperList, KafkaEventProducer producer) {
        this.mappers = mapperList.stream()
                .collect(Collectors.toMap(HubEventMapper::getType, Function.identity()));
        this.producer = producer;
    }

    public void collect(HubEvent event) {
        HubEventMapper mapper = mappers.get(event.getType());
        if (mapper == null) {
            throw new IllegalArgumentException("Не найден маппер для типа события: " + event.getType());
        }
        HubEventAvro avroEvent = mapper.map(event);
        producer.sendHubEvent(avroEvent);
    }
}