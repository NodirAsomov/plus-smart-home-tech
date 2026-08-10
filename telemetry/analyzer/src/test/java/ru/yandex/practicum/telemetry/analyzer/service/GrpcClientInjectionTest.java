package ru.yandex.practicum.telemetry.analyzer.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:grpc-injection;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
class GrpcClientInjectionTest {
    @Autowired
    private ScenarioEvaluator evaluator;

    @Test
    void shouldInjectHubRouterClientIntoSpringBean() {
        assertThat(ReflectionTestUtils.getField(evaluator, "hubRouter")).isNotNull();
    }

    @Test
    void shouldUseHubRouterMethodNameFromExternalContract() {
        assertThat(ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc
                .getHandleDeviceActionMethod().getFullMethodName())
                .isEqualTo("telemetry.service.hubrouter.HubRouterController/handleDeviceAction");
    }
}
