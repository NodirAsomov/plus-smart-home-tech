package ru.yandex.practicum.telemetry.analyzer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.model.Action;
import ru.yandex.practicum.telemetry.analyzer.model.Condition;
import ru.yandex.practicum.telemetry.analyzer.model.Scenario;
import ru.yandex.practicum.telemetry.analyzer.model.Sensor;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.SensorRepository;
import java.util.*;

@Service
public class HubEventService {
    private final SensorRepository sensors;
    private final ScenarioRepository scenarios;
    public HubEventService(SensorRepository sensors, ScenarioRepository scenarios) {
        this.sensors = sensors; this.scenarios = scenarios;
    }

    @Transactional
    public void handle(HubEventAvro event) {
        Object payload = event.getPayload();
        if (payload instanceof DeviceAddedEventAvro added) {
            sensors.findById(added.getId()).ifPresentOrElse(existing -> {
                if (!existing.getHubId().equals(event.getHubId()))
                    throw new IllegalArgumentException("Sensor belongs to another hub: " + added.getId());
            }, () -> sensors.save(new Sensor(added.getId(), event.getHubId())));
        } else if (payload instanceof DeviceRemovedEventAvro removed) {
            sensors.findByIdAndHubId(removed.getId(), event.getHubId()).ifPresent(sensors::delete);
        } else if (payload instanceof ScenarioAddedEventAvro added) {
            saveScenario(event.getHubId(), added);
        } else if (payload instanceof ScenarioRemovedEventAvro removed) {
            scenarios.findByHubIdAndName(event.getHubId(), removed.getName()).ifPresent(scenarios::delete);
        } else {
            throw new IllegalArgumentException("Unsupported hub event payload: " + payload);
        }
    }

    private void saveScenario(String hubId, ScenarioAddedEventAvro event) {
        Scenario scenario = scenarios.findByHubIdAndName(hubId, event.getName())
                .orElseGet(() -> new Scenario(hubId, event.getName()));
        Map<Sensor, Condition> conditions = new LinkedHashMap<>();
        for (ScenarioConditionAvro value : event.getConditions()) {
            Sensor sensor = sensor(value.getSensorId(), hubId);
            conditions.put(sensor, new Condition(value.getType(), value.getOperation(), intValue(value.getValue())));
        }
        Map<Sensor, Action> actions = new LinkedHashMap<>();
        for (DeviceActionAvro value : event.getActions()) {
            Sensor sensor = sensor(value.getSensorId(), hubId);
            actions.put(sensor, new Action(value.getType(), intValue(value.getValue())));
        }
        scenario.replace(conditions, actions);
        scenarios.save(scenario);
    }

    private Sensor sensor(String id, String hubId) {
        return sensors.findByIdAndHubId(id, hubId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown sensor " + id + " for hub " + hubId));
    }

    private Integer intValue(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean b) return b ? 1 : 0;
        return ((Number) value).intValue();
    }
}
