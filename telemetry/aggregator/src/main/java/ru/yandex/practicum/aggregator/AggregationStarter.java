package ru.yandex.practicum.aggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.aggregator.service.SnapshotService;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private final KafkaConsumer<String, SensorEventAvro> consumer;
    private final Producer<String, SensorsSnapshotAvro> producer;
    private final SnapshotService snapshotService;

    @Value("${kafka.topics.sensors}")
    private String sensorTopic;

    @Value("${kafka.topics.snapshots}")
    private String snapshotsTopic;

    // Запуск бесконечного цикла опроса Kafka (Poll Loop)
    public void start() {
        log.info("Агрегатор приступил к обработке потока событий...");
        // позволяет корректно прервать poll loop извне (по сигналу остановки JVM)
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            // подписываем консьюмер на нужный топик
            consumer.subscribe(List.of(sensorTopic));

            while (true) {
                // poll с таймаутом 1с, чтобы можно было корректно обработать WakeupException
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    SensorEventAvro event = record.value();
                    handleEvent(event);
                }
                // commitSync() синхронно сохраняет оффсеты прочитанных сообщений в Kafka
                // это гарантирует, что при перезапуске сервиса мы продолжим читать с того места, где остановились
                consumer.commitSync();
            }

        } catch (WakeupException ignored) {
            // ожидаемое исключение при остановке через shutdown hook (вызов consumer.wakeup()) просто выходим в finally
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {
            // выполняется всегда: и при штатном завершении (wakeup), и при ошибке
            try {
                // producer.flush() гарантирует, что все сообщения, находящиеся во внутреннем буфере продюсера,
                // будут отправлены в Kafka перед закрытием. Без этого последние снапшоты могут потеряться.
                producer.flush();
                // Еще раз фиксируем оффсеты на случай, если последний commitSync не успел сработать
                consumer.commitSync();
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
                log.info("Закрываем продюсер");
                producer.close();
            }
        }
    }

    // обрабатывает одно событие от датчика
    private void handleEvent(SensorEventAvro event) {
        // snapshotService.updateState(event) возвращает Optional:
        // - Optional.of(snapshot) если состояние изменилось и нужно отправить новый снапшот
        // - Optional.empty() если событие старое, дубликат или не влияет на текущее состояние
        Optional<SensorsSnapshotAvro> updatedSnapshot = snapshotService.updateState(event);

        // если снапшот обновился (Optional не пуст), отправляем его в топик
        updatedSnapshot.ifPresent(this::sendSnapshot);
    }

    // отправляет готовый снапшот в топик telemetry.snapshots.v1
    private void sendSnapshot(SensorsSnapshotAvro snapshot) {
        // создаем запись для отправки
        // ключ - hubId, все снапшоты одного хаба будут попадать в одну и ту же партицию, сохраняя порядок обновлений
        ProducerRecord<String, SensorsSnapshotAvro> record =
                new ProducerRecord<>(snapshotsTopic, snapshot.getHubId(), snapshot);
        producer.send(record);
    }
}
