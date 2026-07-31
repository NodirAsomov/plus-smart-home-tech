package ru.yandex.practicum.telemetry.collector.model.hub;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public class ScenarioRemovedEvent extends HubEvent {
    @NotBlank @Size(min = 3) private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    @Override public HubEventType getType() { return HubEventType.SCENARIO_REMOVED; }
}
