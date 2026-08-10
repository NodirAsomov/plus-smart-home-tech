package ru.yandex.practicum.telemetry.analyzer.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.telemetry.analyzer.model.Sensor;
import java.util.*;
public interface SensorRepository extends JpaRepository<Sensor, String> {
    Optional<Sensor> findByIdAndHubId(String id, String hubId);
    boolean existsByIdInAndHubId(Collection<String> ids, String hubId);
}
