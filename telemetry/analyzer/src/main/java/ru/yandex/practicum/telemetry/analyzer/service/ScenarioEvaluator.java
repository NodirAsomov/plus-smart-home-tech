package ru.yandex.practicum.telemetry.analyzer.service;

import com.google.protobuf.Timestamp;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.model.*;
import ru.yandex.practicum.telemetry.analyzer.model.Action;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;
import java.time.Instant;
import java.util.List;

@Service
public class ScenarioEvaluator {
    private static final Logger log = LoggerFactory.getLogger(ScenarioEvaluator.class);
    private static final int SCENARIO_LOOKUP_ATTEMPTS = 20;
    private static final long SCENARIO_LOOKUP_DELAY_MS = 100;
    private final ScenarioRepository scenarios;
    private final HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouter;
    public ScenarioEvaluator(ScenarioRepository scenarios,
            @GrpcClient("hub-router") HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouter) {
        this.scenarios = scenarios; this.hubRouter = hubRouter;
    }

    @Transactional(readOnly = true)
    public void evaluate(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        List<Scenario> hubScenarios = findScenarios(hubId);
        log.info("Evaluating snapshot for hub {} against {} scenario(s)", hubId, hubScenarios.size());
        hubScenarios.stream()
                .filter(scenario -> scenario.getConditions().entrySet().stream()
                        .allMatch(entry -> matches(snapshot, entry.getKey(), entry.getValue())))
                .forEach(scenario -> scenario.getActions().forEach((sensor, action) -> {
                    DeviceActionRequest request = request(hubId, scenario, sensor, action);
                    log.info("Sending action {} to device {} for scenario {} in hub {}",
                            action.getType(), sensor.getId(), scenario.getName(), hubId);
                    hubRouter.handleDeviceAction(request);
                }));
    }

    private List<Scenario> findScenarios(String hubId) {
        for (int attempt = 1; attempt <= SCENARIO_LOOKUP_ATTEMPTS; attempt++) {
            List<Scenario> result = scenarios.findByHubId(hubId);
            if (!result.isEmpty() || attempt == SCENARIO_LOOKUP_ATTEMPTS) return result;
            try {
                Thread.sleep(SCENARIO_LOOKUP_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return List.of();
            }
        }
        return List.of();
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
