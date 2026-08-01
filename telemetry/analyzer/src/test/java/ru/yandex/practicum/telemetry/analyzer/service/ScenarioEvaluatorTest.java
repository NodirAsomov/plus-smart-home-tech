package ru.yandex.practicum.telemetry.analyzer.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.model.Action;
import ru.yandex.practicum.telemetry.analyzer.model.Condition;
import ru.yandex.practicum.telemetry.analyzer.model.Scenario;
import ru.yandex.practicum.telemetry.analyzer.model.Sensor;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ScenarioEvaluatorTest {
    @Test
    void shouldSendScenarioActionsWhenAllConditionsMatch() {
        ScenarioRepository repository = mock(ScenarioRepository.class);
        HubRouterControllerGrpc.HubRouterControllerBlockingStub client =
                mock(HubRouterControllerGrpc.HubRouterControllerBlockingStub.class);
        Sensor temperature = new Sensor("temperature-1", "hub-1");
        Sensor heater = new Sensor("heater-1", "hub-1");
        Scenario scenario = new Scenario("hub-1", "warm-floor");
        scenario.replace(Map.of(temperature,
                        new Condition(ConditionTypeAvro.TEMPERATURE, ConditionOperationAvro.LOWER_THAN, 15)),
                Map.of(heater, new Action(ActionTypeAvro.ACTIVATE, null)));
        when(repository.findByHubId("hub-1")).thenReturn(List.of(scenario));

        Instant snapshotTimestamp = Instant.parse("2026-08-01T11:37:31.389Z");
        SensorsSnapshotAvro snapshot = SensorsSnapshotAvro.newBuilder()
                .setHubId("hub-1").setTimestamp(snapshotTimestamp)
                .setSensorsState(Map.of("temperature-1", SensorStateAvro.newBuilder()
                        .setTimestamp(Instant.now()).setData(TemperatureSensorAvro.newBuilder()
                                .setTemperatureC(10).setTemperatureF(50).build()).build()))
                .build();

        new ScenarioEvaluator(repository, client).evaluate(snapshot);

        ArgumentCaptor<DeviceActionRequest> request = ArgumentCaptor.forClass(DeviceActionRequest.class);
        verify(client).handleDeviceAction(request.capture());
        assertThat(request.getValue().getHubId()).isEqualTo("hub-1");
        assertThat(request.getValue().getScenarioName()).isEqualTo("warm-floor");
        assertThat(request.getValue().getAction().getSensorId()).isEqualTo("heater-1");
        assertThat(request.getValue().getAction().getType()).isEqualTo(ActionTypeProto.ACTIVATE);
        assertThat(request.getValue().getAction().hasValue()).isFalse();
        assertThat(request.getValue().getTimestamp().getSeconds()).isEqualTo(snapshotTimestamp.getEpochSecond());
        assertThat(request.getValue().getTimestamp().getNanos()).isEqualTo(snapshotTimestamp.getNano());
    }

    @Test
    void shouldNotSendActionsWhenConditionDoesNotMatch() {
        ScenarioRepository repository = mock(ScenarioRepository.class);
        HubRouterControllerGrpc.HubRouterControllerBlockingStub client =
                mock(HubRouterControllerGrpc.HubRouterControllerBlockingStub.class);
        Sensor sensor = new Sensor("switch-1", "hub-1");
        Scenario scenario = new Scenario("hub-1", "off-only");
        scenario.replace(Map.of(sensor,
                        new Condition(ConditionTypeAvro.SWITCH, ConditionOperationAvro.EQUALS, 0)),
                Map.of(sensor, new Action(ActionTypeAvro.ACTIVATE, null)));
        when(repository.findByHubId("hub-1")).thenReturn(List.of(scenario));
        SensorsSnapshotAvro snapshot = SensorsSnapshotAvro.newBuilder()
                .setHubId("hub-1").setTimestamp(Instant.now())
                .setSensorsState(Map.of("switch-1", SensorStateAvro.newBuilder()
                        .setTimestamp(Instant.now()).setData(SwitchSensorAvro.newBuilder().setState(true).build()).build()))
                .build();

        new ScenarioEvaluator(repository, client).evaluate(snapshot);

        verifyNoInteractions(client);
    }
}
