package ru.yandex.practicum.aggregator.serializer;

import org.apache.avro.Schema;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.io.UncheckedIOException;

public class BaseAvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {
    // Фабрика для создания декодеров (отвечает за чтение бинарного потока)
    private final DecoderFactory decoderFactory;
    // Читатель данных, который может превратить бинарные данные в конкретный Java объект T по переданной схеме (Schema)
    private final DatumReader<T> datumReader;

    public BaseAvroDeserializer(Schema schema) {
        this(DecoderFactory.get(), schema);
    }

    // Позволяет передать свою фабрику декодеров и схему.
    public BaseAvroDeserializer(DecoderFactory decoderFactory, Schema schema) {
        this.decoderFactory = decoderFactory;
        // SpecificDatumReader работает с конкретными Java классами (SpecificRecord),
        // а не с универсальными GenericRecord. Ему обязательно нужна схема.
        this.datumReader = new SpecificDatumReader<>(schema);
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            // Создаем декодер из сырых байтов
            // binaryDecoder читает бинарный формат Avro. Второй аргумент (null) - это буфер,
            // если передать null, фабрика создаст новый внутренний буфер
            Decoder decoder = decoderFactory.binaryDecoder(data, null);

            // Читаем данные. Первый аргумент (null) означает, что мы не пытаемся обновить
            // существующий объект, а создаем новый. Декодер содержит поток байтов
            return datumReader.read(null, decoder);
        } catch (IOException e) {
            throw new UncheckedIOException("Ошибка десериализации Avro-объекта в топик : " + topic, e);
        }
    }
}
