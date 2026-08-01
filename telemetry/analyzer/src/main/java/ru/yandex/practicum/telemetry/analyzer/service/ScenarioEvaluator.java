package ru.yandex.practicum.telemetry.analyzer.service;

import com.google.protobuf.Timestamp;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.model.*;
import ru.yandex.practicum.telemetry.analyzer.model.Action;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;
import java.time.Instant;

@Service
public class ScenarioEvaluator {
    private final ScenarioRepository scenarios;
    private final HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouter;
    public ScenarioEvaluator(ScenarioRepository scenarios,
            @GrpcClient("hub-router") HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouter) {
        this.scenarios = scenarios; this.hubRouter = hubRouter;
    }

    public void evaluate(SensorsSnapshotAvro snapshot) {
        scenarios.findByHubId(snapshot.getHubId()).stream()
                .filter(scenario -> scenario.getConditions().entrySet().stream()
                        .allMatch(entry -> matches(snapshot, entry.getKey(), entry.getValue())))
                .forEach(scenario -> scenario.getActions().forEach((sensor, action) ->
                        hubRouter.handleDeviceAction(request(snapshot.getHubId(), scenario, sensor, action))));
    }

    private boolean matches(SensorsSnapshotAvro snapshot, Sensor sensor, Condition condition) {
        SensorStateAvro state = snapshot.getSensorsState().get(sensor.getId());
        if (state == null || condition.getValue() == null) return false;
        Integer actual = value(state.getData(), condition.getType());
        if (actual == null) return false;
        return switch (condition.getOperation()) {
            case EQUALS -> actual.equals(condition.getValue());
            case GREATER_THAN -> actual > condition.getValue();
            case LOWER_THAN -> actual < condition.getValue();
        };
    }

    private Integer value(Object data, ConditionTypeAvro type) {
        return switch (type) {
            case MOTION -> data instanceof MotionSensorAvro v ? (v.getMotion() ? 1 : 0) : null;
            case LUMINOSITY -> data instanceof LightSensorAvro v ? v.getLuminosity() : null;
            case SWITCH -> data instanceof SwitchSensorAvro v ? (v.getState() ? 1 : 0) : null;
            case TEMPERATURE -> data instanceof TemperatureSensorAvro v ? v.getTemperatureC()
                    : data instanceof ClimateSensorAvro v ? v.getTemperatureC() : null;
            case CO2LEVEL -> data instanceof ClimateSensorAvro v ? v.getCo2Level() : null;
            case HUMIDITY -> data instanceof ClimateSensorAvro v ? v.getHumidity() : null;
        };
    }

    private DeviceActionRequest request(String hubId, Scenario scenario, Sensor sensor, Action action) {
        DeviceActionProto.Builder actionBuilder = DeviceActionProto.newBuilder()
                .setSensorId(sensor.getId()).setType(ActionTypeProto.valueOf(action.getType().name()));
        if (action.getValue() != null) actionBuilder.setValue(action.getValue());
        Instant now = Instant.now();
        return DeviceActionRequest.newBuilder().setHubId(hubId).setScenarioName(scenario.getName())
                .setAction(actionBuilder).setTimestamp(Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond()).setNanos(now.getNano())).build();
    }
}
