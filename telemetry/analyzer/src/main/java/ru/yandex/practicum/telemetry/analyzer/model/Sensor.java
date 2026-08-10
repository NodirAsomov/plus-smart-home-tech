package ru.yandex.practicum.telemetry.analyzer.model;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "sensors")
public class Sensor {
    @Id private String id;
    @Column(name = "hub_id", nullable = false) private String hubId;
    protected Sensor() {}
    public Sensor(String id, String hubId) { this.id = id; this.hubId = hubId; }
    public String getId() { return id; }
    public String getHubId() { return hubId; }
    @Override public boolean equals(Object o) { return o instanceof Sensor s && Objects.equals(id, s.id); }
    @Override public int hashCode() { return Objects.hashCode(id); }
}
