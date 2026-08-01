package ru.yandex.practicum.telemetry.aggregator.service;

import jakarta.annotation.PreDestroy;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Component
public class AggregationStarter {
    private static final Logger log = LoggerFactory.getLogger(AggregationStarter.class);

    private final Consumer<String, SensorEventAvro> consumer;
    private final Producer<String, SpecificRecordBase> producer;
    private final SnapshotService snapshotService;
    private final String sensorTopic;
    private final String snapshotTopic;

    public AggregationStarter(
            Consumer<String, SensorEventAvro> consumer,
            Producer<String, SpecificRecordBase> producer,
            SnapshotService snapshotService,
            @Value("${aggregator.kafka.sensor-topic}") String sensorTopic,
            @Value("${aggregator.kafka.snapshot-topic}") String snapshotTopic) {
        this.consumer = consumer;
        this.producer = producer;
        this.snapshotService = snapshotService;
        this.sensorTopic = sensorTopic;
        this.snapshotTopic = snapshotTopic;
    }

    public void start() {
        try {
            consumer.subscribe(List.of(sensorTopic));
            while (true) {
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    snapshotService.updateState(record.value())
                            .ifPresent(this::sendSnapshot);
                }
                if (!records.isEmpty()) {
                    producer.flush();
                    consumer.commitSync();
                }
            }
        } catch (WakeupException ignored) {
            log.info("Aggregation stopping");
        } catch (Exception e) {
            log.error("Error while processing sensor events", e);
        } finally {
            try {
                producer.flush();
                consumer.commitSync();
            } finally {
                consumer.close();
                producer.close();
            }
        }
    }

    private void sendSnapshot(SensorsSnapshotAvro snapshot) {
        try {
            producer.send(new ProducerRecord<>(snapshotTopic, snapshot.getHubId(), snapshot)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing a snapshot", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to publish a snapshot", e.getCause());
        }
    }

    @PreDestroy
    public void stop() {
        consumer.wakeup();
    }
}
