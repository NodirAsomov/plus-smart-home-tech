package ru.yandex.practicum.telemetry.analyzer.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.model.*;
import ru.yandex.practicum.telemetry.analyzer.model.Action;
import ru.yandex.practicum.telemetry.analyzer.service.HubEventService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:analyzer;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ScenarioRepositoryTest {
    @Autowired SensorRepository sensors;
    @Autowired ScenarioRepository scenarios;

    @Test
    void shouldStoreScenarioFromRealHubEvents() {
        HubEventService service = new HubEventService(sensors, scenarios);
        service.handle(HubEventAvro.newBuilder().setHubId("hub-1").setTimestamp(Instant.now())
                .setPayload(DeviceAddedEventAvro.newBuilder().setId("temperature-1")
                        .setType(DeviceTypeAvro.TEMPERATURE_SENSOR).build()).build());
        service.handle(HubEventAvro.newBuilder().setHubId("hub-1").setTimestamp(Instant.now())
                .setPayload(DeviceAddedEventAvro.newBuilder().setId("heater-1")
                        .setType(DeviceTypeAvro.SWITCH_SENSOR).build()).build());
        service.handle(HubEventAvro.newBuilder().setHubId("hub-1").setTimestamp(Instant.now())
                .setPayload(ScenarioAddedEventAvro.newBuilder().setName("heating")
                        .setConditions(List.of(ScenarioConditionAvro.newBuilder()
                                .setSensorId("temperature-1").setType(ConditionTypeAvro.TEMPERATURE)
                                .setOperation(ConditionOperationAvro.LOWER_THAN).setValue(15).build()))
                        .setActions(List.of(DeviceActionAvro.newBuilder().setSensorId("heater-1")
                                .setType(ActionTypeAvro.ACTIVATE).setValue(null).build())).build()).build());

        Scenario loaded = scenarios.findByHubId("hub-1").getFirst();
        assertThat(loaded.getConditions()).hasSize(1);
        assertThat(loaded.getActions()).hasSize(1);
    }

    @Test
    void shouldStoreAndLoadConditionsAndActionsWithTheirSensors() {
        Sensor source = sensors.save(new Sensor("temperature-1", "hub-1"));
        Sensor target = sensors.save(new Sensor("heater-1", "hub-1"));
        Scenario scenario = new Scenario("hub-1", "heating");
        scenario.replace(Map.of(source,
                        new Condition(ConditionTypeAvro.TEMPERATURE, ConditionOperationAvro.LOWER_THAN, 15)),
                Map.of(target, new Action(ActionTypeAvro.ACTIVATE, null)));
        scenarios.saveAndFlush(scenario);

        Scenario loaded = scenarios.findByHubId("hub-1").getFirst();

        assertThat(loaded.getConditions()).hasSize(1).containsKey(source);
        assertThat(loaded.getActions()).hasSize(1).containsKey(target);
    }
}
