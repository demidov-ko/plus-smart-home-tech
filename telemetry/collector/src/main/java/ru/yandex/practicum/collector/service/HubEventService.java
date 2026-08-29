package ru.yandex.practicum.collector.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.collector.mapper.hub.HubEventMapper;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HubEventService {

    private final Map<HubEventProto.PayloadCase, HubEventMapper> mappers;
    private final KafkaEventProducer producer;

    public HubEventService(List<HubEventMapper> mapperList, KafkaEventProducer producer) {
        this.mappers = mapperList.stream()
                .collect(Collectors.toMap(HubEventMapper::getPayloadCase, Function.identity()));
        this.producer = producer;
    }

    public void collect(HubEventProto event) {
        HubEventMapper mapper = mappers.get(event.getPayloadCase());
        if (mapper == null) {
            throw new IllegalArgumentException("Не найден маппер для типа события: " + event.getPayloadCase());
        }
        HubEventAvro avroEvent = mapper.map(event);
        producer.sendHubEvent(avroEvent);
    }
}