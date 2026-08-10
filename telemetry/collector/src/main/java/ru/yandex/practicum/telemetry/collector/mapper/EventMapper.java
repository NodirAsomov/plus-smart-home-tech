package ru.yandex.practicum.telemetry.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioConditionProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.ActionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

import java.time.Instant;

@Component
public class EventMapper {
    public SensorEventAvro toAvro(SensorEventProto event) {
        Object payload = switch (event.getPayloadCase()) {
            case CLIMATE_SENSOR -> ClimateSensorAvro.newBuilder()
                    .setTemperatureC(event.getClimateSensor().getTemperatureC())
                    .setHumidity(event.getClimateSensor().getHumidity())
                    .setCo2Level(event.getClimateSensor().getCo2Level())
                    .build();
            case LIGHT_SENSOR -> LightSensorAvro.newBuilder()
                    .setLinkQuality(event.getLightSensor().getLinkQuality())
                    .setLuminosity(event.getLightSensor().getLuminosity())
                    .build();
            case MOTION_SENSOR -> MotionSensorAvro.newBuilder()
                    .setLinkQuality(event.getMotionSensor().getLinkQuality())
                    .setMotion(event.getMotionSensor().getMotion())
                    .setVoltage(event.getMotionSensor().getVoltage())
                    .build();
            case SWITCH_SENSOR -> SwitchSensorAvro.newBuilder()
                    .setState(event.getSwitchSensor().getState())
                    .build();
            case TEMPERATURE_SENSOR -> TemperatureSensorAvro.newBuilder()
                    .setTemperatureC(event.getTemperatureSensor().getTemperatureC())
                    .setTemperatureF(event.getTemperatureSensor().getTemperatureF())
                    .build();
            case PAYLOAD_NOT_SET -> throw new IllegalArgumentException("Sensor event payload is not set");
        };

        return SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(toInstant(event.getTimestamp()))
                .setPayload(payload)
                .build();
    }

    public HubEventAvro toAvro(HubEventProto event) {
        Object payload = switch (event.getPayloadCase()) {
            case DEVICE_ADDED -> DeviceAddedEventAvro.newBuilder()
                    .setId(event.getDeviceAdded().getId())
                    .setType(DeviceTypeAvro.valueOf(event.getDeviceAdded().getType().name()))
                    .build();
            case DEVICE_REMOVED -> DeviceRemovedEventAvro.newBuilder()
                    .setId(event.getDeviceRemoved().getId())
                    .build();
            case SCENARIO_ADDED -> ScenarioAddedEventAvro.newBuilder()
                    .setName(event.getScenarioAdded().getName())
                    .setConditions(event.getScenarioAdded().getConditionList().stream()
                            .map(this::toAvro)
                            .toList())
                    .setActions(event.getScenarioAdded().getActionList().stream()
                            .map(this::toAvro)
                            .toList())
                    .build();
            case SCENARIO_REMOVED -> ScenarioRemovedEventAvro.newBuilder()
                    .setName(event.getScenarioRemoved().getName())
                    .build();
            case PAYLOAD_NOT_SET -> throw new IllegalArgumentException("Hub event payload is not set");
        };

        return HubEventAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(toInstant(event.getTimestamp()))
                .setPayload(payload)
                .build();
    }

    private ScenarioConditionAvro toAvro(ScenarioConditionProto condition) {
        Object value = switch (condition.getValueCase()) {
            case BOOL_VALUE -> condition.getBoolValue();
            case INT_VALUE -> condition.getIntValue();
            case VALUE_NOT_SET -> throw new IllegalArgumentException("Scenario condition value is not set");
        };

        return ScenarioConditionAvro.newBuilder()
                .setSensorId(condition.getSensorId())
                .setType(ConditionTypeAvro.valueOf(condition.getType().name()))
                .setOperation(ConditionOperationAvro.valueOf(condition.getOperation().name()))
                .setValue(value)
                .build();
    }

    private DeviceActionAvro toAvro(DeviceActionProto action) {
        return DeviceActionAvro.newBuilder()
                .setSensorId(action.getSensorId())
                .setType(ActionTypeAvro.valueOf(action.getType().name()))
                .setValue(action.hasValue() ? action.getValue() : null)
                .build();
    }

    private Instant toInstant(com.google.protobuf.Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}
