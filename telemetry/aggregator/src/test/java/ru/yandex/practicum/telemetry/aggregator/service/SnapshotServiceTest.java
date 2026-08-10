package ru.yandex.practicum.telemetry.aggregator.service;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SnapshotServiceTest {
    private final SnapshotService service = new SnapshotService();

    @Test
    void shouldCreateSnapshotAndPreserveStatesOfDifferentSensors() {
        var first = service.updateState(event("hub-1", "switch-1", 10,
                SwitchSensorAvro.newBuilder().setState(true).build())).orElseThrow();
        var second = service.updateState(event("hub-1", "temperature-1", 11,
                TemperatureSensorAvro.newBuilder().setTemperatureC(20).setTemperatureF(68).build()))
                .orElseThrow();

        assertThat(first).isSameAs(second);
        assertThat(second.getSensorsState()).containsOnlyKeys("switch-1", "temperature-1");
        assertThat(second.getTimestamp()).isEqualTo(Instant.ofEpochSecond(11));
    }

    @Test
    void shouldIgnoreDuplicateAndOlderEvents() {
        service.updateState(event("hub-1", "switch-1", 10,
                SwitchSensorAvro.newBuilder().setState(true).build()));

        assertThat(service.updateState(event("hub-1", "switch-1", 11,
                SwitchSensorAvro.newBuilder().setState(true).build()))).isEmpty();
        assertThat(service.updateState(event("hub-1", "switch-1", 9,
                SwitchSensorAvro.newBuilder().setState(false).build()))).isEmpty();
    }

    @Test
    void shouldUpdateSnapshotWhenNewerDataChanges() {
        service.updateState(event("hub-1", "switch-1", 10,
                SwitchSensorAvro.newBuilder().setState(true).build()));

        var snapshot = service.updateState(event("hub-1", "switch-1", 11,
                SwitchSensorAvro.newBuilder().setState(false).build())).orElseThrow();

        assertThat(((SwitchSensorAvro) snapshot.getSensorsState().get("switch-1").getData()).getState())
                .isFalse();
        assertThat(snapshot.getTimestamp()).isEqualTo(Instant.ofEpochSecond(11));
    }

    private SensorEventAvro event(String hubId, String sensorId, long epochSecond, Object payload) {
        return SensorEventAvro.newBuilder()
                .setHubId(hubId)
                .setId(sensorId)
                .setTimestamp(Instant.ofEpochSecond(epochSecond))
                .setPayload(payload)
                .build();
    }
}
