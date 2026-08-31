package ru.yandex.practicum.analyzer.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.handler.HubEventHandler;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
// отдельный поток (Runnable), который непрерывно опрашивает Kafka-топик с событиями хабов
// его задача распределять входящие события по спец. обработчикам (HubEventHandler) в зависимости от payload
public class HubEventProcessor implements Runnable {

    private final KafkaConsumer<String, HubEventAvro> consumer;
    private final Map<Class<?>, HubEventHandler> handlers;

    @Value("${kafka.topics.hubs}")
    private String hubsTopic;

    public HubEventProcessor(KafkaConsumer<String, HubEventAvro> hubEventConsumer, List<HubEventHandler> handlerList) {
        this.consumer = hubEventConsumer;
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(HubEventHandler::getPayloadType, Function.identity()));

        log.info("Инициализировано {} обработчиков для событий хабов", handlers.size());
    }

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            consumer.subscribe(List.of(hubsTopic));
            log.info("HubEventProcessor начал чтение из топика: {}", hubsTopic);

            while (true) {
                ConsumerRecords<String, HubEventAvro> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    handleEvent(record.value());
                }

                // асинхронно фиксируем смещение(если приложение упадет до коммита, при рестарте сообщения придут снова
                consumer.commitAsync();
            }

        } catch (WakeupException ignored) {
            // ожидаем при остановке
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий хаба", e);
        } finally {
            try {
                consumer.commitSync();
            } finally {
                log.info("Закрываем консьюмер HubEventProcessor");
                consumer.close();
            }
        }
    }

    private void handleEvent(HubEventAvro event) {
        HubEventHandler handler = handlers.get(event.getPayload().getClass());
        if (handler == null) {
            log.warn("Не найден обработчик для события с payload: {}", event.getPayload().getClass());
            return;
        }
        log.info("Обрабатываю событие хаба {} типа {}", event.getHubId(), event.getPayload().getClass().getSimpleName());
        handler.handle(event);
    }
}