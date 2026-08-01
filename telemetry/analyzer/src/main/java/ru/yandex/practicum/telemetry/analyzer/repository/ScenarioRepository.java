package ru.yandex.practicum.telemetry.analyzer.repository;
import org.springframework.data.jpa.repository.*;
import ru.yandex.practicum.telemetry.analyzer.model.Scenario;
import java.util.*;
public interface ScenarioRepository extends JpaRepository<Scenario, Long> {
    @EntityGraph(attributePaths = {"conditions", "actions"})
    List<Scenario> findByHubId(String hubId);
    Optional<Scenario> findByHubIdAndName(String hubId, String name);
}
