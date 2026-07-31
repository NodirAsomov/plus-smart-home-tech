package ru.yandex.practicum.telemetry.collector.model.hub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ScenarioCondition {
    @NotBlank private String sensorId;
    @NotNull private ConditionType type;
    @NotNull private ConditionOperation operation;
    private Object value;
    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }
    public ConditionType getType() { return type; }
    public void setType(ConditionType type) { this.type = type; }
    public ConditionOperation getOperation() { return operation; }
    public void setOperation(ConditionOperation operation) { this.operation = operation; }
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
}
