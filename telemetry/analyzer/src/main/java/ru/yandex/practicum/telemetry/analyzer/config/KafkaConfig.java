package ru.yandex.practicum.telemetry.analyzer.config;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.kafka.deserializer.HubEventDeserializer;
import ru.yandex.practicum.kafka.deserializer.SensorsSnapshotDeserializer;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import java.util.Properties;

@Configuration
public class KafkaConfig {
    private Properties properties(String bootstrap, String group, Class<?> deserializer) {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ConsumerConfig.GROUP_ID_CONFIG, group);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return p;
    }

    @Bean(destroyMethod = "")
    Consumer<String, HubEventAvro> hubEventConsumer(@Value("${analyzer.kafka.bootstrap-servers}") String bootstrap,
            @Value("${analyzer.kafka.hub-group-id}") String group) {
        return new KafkaConsumer<>(properties(bootstrap, group, HubEventDeserializer.class));
    }

    @Bean(destroyMethod = "")
    Consumer<String, SensorsSnapshotAvro> snapshotConsumer(@Value("${analyzer.kafka.bootstrap-servers}") String bootstrap,
            @Value("${analyzer.kafka.snapshot-group-id}") String group) {
        return new KafkaConsumer<>(properties(bootstrap, group, SensorsSnapshotDeserializer.class));
    }
}
