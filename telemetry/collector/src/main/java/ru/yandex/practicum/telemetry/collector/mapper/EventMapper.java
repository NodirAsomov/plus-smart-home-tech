package ru.yandex.practicum.telemetry.collector.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.collector.model.hub.*;
import ru.yandex.practicum.telemetry.collector.model.sensor.*;

import java.util.stream.Collectors;

@Component
public class EventMapper {
    public SensorEventAvro toAvro(SensorEvent event) {
        Object payload;
        if (event instanceof ClimateSensorEvent e) {
            payload = ClimateSensorAvro.newBuilder().setTemperatureC(e.getTemperatureC())
                    .setHumidity(e.getHumidity()).setCo2Level(e.getCo2Level()).build();
        } else if (event instanceof LightSensorEvent e) {
            payload = LightSensorAvro.newBuilder().setLinkQuality(e.getLinkQuality())
                    .setLuminosity(e.getLuminosity()).build();
        } else if (event instanceof MotionSensorEvent e) {
            payload = MotionSensorAvro.newBuilder().setLinkQuality(e.getLinkQuality())
                    .setMotion(e.isMotion()).setVoltage(e.getVoltage()).build();
        } else if (event instanceof SwitchSensorEvent e) {
            payload = SwitchSensorAvro.newBuilder().setState(e.isState()).build();
        } else if (event instanceof TemperatureSensorEvent e) {
            payload = TemperatureSensorAvro.newBuilder().setTemperatureC(e.getTemperatureC())
                    .setTemperatureF(e.getTemperatureF()).build();
        } else {
            throw new IllegalArgumentException("Unsupported sensor event: " + event.getClass());
        }
        return SensorEventAvro.newBuilder().setId(event.getId()).setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp()).setPayload(payload).build();
    }

    public HubEventAvro toAvro(HubEvent event) {
        Object payload;
        if (event instanceof DeviceAddedEvent e) {
            payload = DeviceAddedEventAvro.newBuilder().setId(e.getId())
                    .setType(DeviceTypeAvro.valueOf(e.getDeviceType().name())).build();
        } else if (event instanceof DeviceRemovedEvent e) {
            payload = DeviceRemovedEventAvro.newBuilder().setId(e.getId()).build();
        } else if (event instanceof ScenarioAddedEvent e) {
            payload = ScenarioAddedEventAvro.newBuilder().setName(e.getName())
                    .setConditions(e.getConditions().stream().map(this::toAvro).collect(Collectors.toList()))
                    .setActions(e.getActions().stream().map(this::toAvro).collect(Collectors.toList())).build();
        } else if (event instanceof ScenarioRemovedEvent e) {
            payload = ScenarioRemovedEventAvro.newBuilder().setName(e.getName()).build();
        } else {
            throw new IllegalArgumentException("Unsupported hub event: " + event.getClass());
        }
        return HubEventAvro.newBuilder().setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp()).setPayload(payload).build();
    }

    private ScenarioConditionAvro toAvro(ScenarioCondition condition) {
        return ScenarioConditionAvro.newBuilder().setSensorId(condition.getSensorId())
                .setType(ConditionTypeAvro.valueOf(condition.getType().name()))
                .setOperation(ConditionOperationAvro.valueOf(condition.getOperation().name()))
                .setValue(condition.getValue()).build();
    }

    private DeviceActionAvro toAvro(DeviceAction action) {
        return DeviceActionAvro.newBuilder().setSensorId(action.getSensorId())
                .setType(ActionTypeAvro.valueOf(action.getType().name()))
                .setValue(action.getValue()).build();
    }
}
