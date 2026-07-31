package ru.yandex.practicum.telemetry.collector.service;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.telemetry.collector.mapper.EventMapper;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent;

@Service
public class EventCollectorService {
    private final Producer<String, SpecificRecordBase> producer;
    private final EventMapper mapper;
    private final String sensorTopic;
    private final String hubTopic;

    public EventCollectorService(Producer<String, SpecificRecordBase> producer, EventMapper mapper,
            @Value("${collector.kafka.sensor-topic}") String sensorTopic,
            @Value("${collector.kafka.hub-topic}") String hubTopic) {
        this.producer = producer;
        this.mapper = mapper;
        this.sensorTopic = sensorTopic;
        this.hubTopic = hubTopic;
    }

    public void collect(SensorEvent event) {
        producer.send(new ProducerRecord<>(sensorTopic, event.getHubId(), mapper.toAvro(event)));
    }

    public void collect(HubEvent event) {
        producer.send(new ProducerRecord<>(hubTopic, event.getHubId(), mapper.toAvro(event)));
    }
}
