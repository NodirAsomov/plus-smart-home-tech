package ru.yandex.practicum.telemetry.analyzer.processor;

import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.analyzer.service.ScenarioEvaluator;
import java.time.Duration;
import java.util.List;

@Component
public class SnapshotProcessor {
    private static final Logger log = LoggerFactory.getLogger(SnapshotProcessor.class);
    private final Consumer<String, SensorsSnapshotAvro> consumer; private final ScenarioEvaluator evaluator; private final String topic;
    public SnapshotProcessor(@Qualifier("snapshotConsumer") Consumer<String, SensorsSnapshotAvro> consumer,
            ScenarioEvaluator evaluator, @Value("${analyzer.kafka.snapshot-topic}") String topic) {
        this.consumer = consumer; this.evaluator = evaluator; this.topic = topic;
    }
    public void start() {
        try {
            consumer.subscribe(List.of(topic));
            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(Duration.ofMillis(500));
                records.forEach(record -> {
                    log.info("Received snapshot for hub {} at partition {} offset {}",
                            record.value().getHubId(), record.partition(), record.offset());
                    try {
                        evaluator.evaluate(record.value());
                    } catch (Exception e) {
                        log.error("Failed to evaluate snapshot at partition {} offset {}",
                                record.partition(), record.offset(), e);
                    }
                });
                if (!records.isEmpty()) consumer.commitSync();
            }
        } catch (WakeupException ignored) { log.info("Snapshot processor stopping"); }
        catch (Exception e) { log.error("Snapshot processor failed", e); }
        finally { consumer.close(); }
    }
    @PreDestroy public void stop() { consumer.wakeup(); }
}
