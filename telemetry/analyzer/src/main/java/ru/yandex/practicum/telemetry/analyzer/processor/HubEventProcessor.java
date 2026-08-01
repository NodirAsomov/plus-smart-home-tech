package ru.yandex.practicum.telemetry.analyzer.processor;

import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.telemetry.analyzer.service.HubEventService;
import java.time.Duration;
import java.util.List;

@Component
public class HubEventProcessor implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(HubEventProcessor.class);
    private final Consumer<String, HubEventAvro> consumer; private final HubEventService service; private final String topic;
    public HubEventProcessor(@Qualifier("hubEventConsumer") Consumer<String, HubEventAvro> consumer,
            HubEventService service, @Value("${analyzer.kafka.hub-topic}") String topic) {
        this.consumer = consumer; this.service = service; this.topic = topic;
    }
    @Override public void run() {
        try {
            consumer.subscribe(List.of(topic));
            while (true) {
                ConsumerRecords<String, HubEventAvro> records = consumer.poll(Duration.ofMillis(500));
                records.forEach(record -> service.handle(record.value()));
                if (!records.isEmpty()) consumer.commitSync();
            }
        } catch (WakeupException ignored) { log.info("Hub event processor stopping"); }
        catch (Exception e) { log.error("Hub event processor failed", e); }
        finally { consumer.close(); }
    }
    @PreDestroy public void stop() { consumer.wakeup(); }
}
