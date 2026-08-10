package ru.yandex.practicum.telemetry.analyzer;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SchemaSqlParsingTest {
    @Test
    void shouldPassTriggerFunctionToJdbcAsOneCompleteStatement() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(false);

        ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));

        var sql = mockingDetails(statement).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("execute"))
                .map(invocation -> (String) invocation.getArgument(0))
                .filter(value -> value.startsWith("CREATE OR REPLACE FUNCTION check_hub_id"))
                .findFirst()
                .orElseThrow();
        assertThat(sql).contains("RAISE EXCEPTION", "END;", "LANGUAGE plpgsql");
    }
}
