package ru.yandex.practicum.kafka.serializer;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;

/**
 * Kafka serializer shared by all telemetry services that publish generated
 * Avro specific records.
 */
public class GeneralAvroSerializer implements Serializer<SpecificRecordBase> {
    @Override
    public byte[] serialize(String topic, SpecificRecordBase data) {
        if (data == null) return null;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(output, null);
            new SpecificDatumWriter<SpecificRecordBase>(data.getSchema()).write(data, encoder);
            encoder.flush();
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize Avro message", e);
        }
    }
}
