package ru.yandex.practicum.order;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.client.ProductClient;
import ru.yandex.practicum.order.client.InventoryClient;
import ru.yandex.practicum.order.client.dto.ProductResponse;
import ru.yandex.practicum.order.client.dto.InventoryResponse;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("unchecked")
class OrderServiceAcceptanceTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @MockBean private ProductClient productClient;
    @MockBean private InventoryClient inventoryClient;
    @Autowired private OrderRepository orderRepository;

    @Test
    void shouldCreateOrderStoreProductSnapshotAndFindOrderByIdAndEmail() throws Exception {
        when(productClient.findById(1L)).thenReturn(new ProductResponse(1L, "Acceptance Smart Lamp", new BigDecimal("3490.00"), true));
        when(productClient.findById(2L)).thenReturn(new ProductResponse(2L, "Acceptance Smart Plug", new BigDecimal("1290.00"), true));
        when(inventoryClient.reserve(any())).thenReturn(new InventoryResponse(true, 10, "reserved"));
        CreateOrderRequest request = new CreateOrderRequest(
                "Acceptance Buyer",
                "acceptance-buyer@example.com",
                List.of(
                        new OrderItemRequest(1L, "Acceptance Smart Lamp", 2, new BigDecimal("3490.00")),
                        new OrderItemRequest(2L, "Acceptance Smart Plug", 1, new BigDecimal("1290.00"))
                )
        );

        MvcResult createResponse = postJson("/api/orders", request);

        assertThat(status(createResponse))
                .as("POST /api/orders должен создавать заказ и возвращать HTTP 201 Created")
                .isEqualTo(201);
        Map<String, Object> created = readMap(createResponse);
        Long orderId = asLong(created.get("id"));
        assertThat(orderId)
                .as("Созданный заказ должен содержать поле id")
                .isNotNull();
        assertThat(created.get("status"))
                .as("На текущем этапе новый заказ должен сохраняться в статусе CREATED")
                .isEqualTo("CONFIRMED");
        assertThat(asDecimal(created.get("totalPrice")))
                .as("order-service должен сам рассчитывать totalPrice по снимку товаров из запроса")
                .isEqualByComparingTo("8270.00");
        assertThat((List<?>) created.get("items"))
                .as("Заказ должен хранить позиции заказа")
                .hasSize(2)
                .anySatisfy(item -> assertThat((Map<String, Object>) item)
                        .as("Позиция заказа должна хранить снимок названия и цены товара из запроса")
                        .containsEntry("productName", "Acceptance Smart Lamp"));

        MvcResult byIdResponse = mvc.perform(get("/api/orders/{id}", orderId)).andReturn();

        assertThat(status(byIdResponse))
                .as("GET /api/orders/{id} должен возвращать созданный заказ")
                .isEqualTo(200);
        assertThat(readMap(byIdResponse).get("customerEmail"))
                .as("GET /api/orders/{id} должен вернуть заказ с ожидаемым email клиента")
                .isEqualTo("acceptance-buyer@example.com");

        MvcResult byEmailResponse = mvc.perform(get("/api/orders/by-email")
                .param("email", "acceptance-buyer@example.com"))
                .andReturn();

        assertThat(status(byEmailResponse))
                .as("GET /api/orders/by-email?email=... должен возвращать заказы клиента")
                .isEqualTo(200);
        assertThat(readList(byEmailResponse))
                .as("Поиск заказов по email должен вернуть созданный заказ")
                .anySatisfy(item -> assertThat(item)
                        .containsEntry("customerEmail", "acceptance-buyer@example.com"));
    }

    @Test
    void shouldReturnBadRequestForInvalidOrderPayload() throws Exception {
        CreateOrderRequest invalidRequest = new CreateOrderRequest(
                "",
                "not-an-email",
                List.of()
        );

        MvcResult response = postJson("/api/orders", invalidRequest);

        assertThat(status(response))
                .as("POST /api/orders с невалидным телом запроса должен возвращать HTTP 400 Bad Request")
                .isEqualTo(400);
        assertThat(readMap(response))
                .as("Ответ ошибки должен содержать сообщение и детали валидации")
                .containsKeys("message", "validationErrors");
    }

    @Test
    void shouldSavePendingOrderWithPlaceholderWhenProductServiceIsUnavailable() throws Exception {
        when(productClient.findById(10L)).thenThrow(new ProductServiceUnavailableException(10L, new RuntimeException("timeout")));
        when(inventoryClient.reserve(any())).thenReturn(new InventoryResponse(true, 5, "reserved"));

        MvcResult response = postJson("/api/orders", orderRequest("product-degraded@example.com", 10L, 1));

        assertThat(status(response)).isEqualTo(201);
        Map<String, Object> created = readMap(response);
        assertThat(created.get("status")).isEqualTo("PENDING_CONFIRMATION");
        assertThat(created.get("statusDetails").toString()).contains("10");
        assertThat((List<Map<String, Object>>) created.get("items"))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.get("productName").toString()).contains("#10");
                    assertThat(asDecimal(item.get("price"))).isZero();
                });
    }

    @Test
    void shouldSavePendingOrderWhenInventoryServiceIsUnavailable() throws Exception {
        when(productClient.findById(11L)).thenReturn(new ProductResponse(11L, "Lamp", new BigDecimal("100.00"), true));
        when(inventoryClient.reserve(any())).thenThrow(
                new InventoryServiceUnavailableException("reserving", 11L, new RuntimeException("circuit open")));

        MvcResult response = postJson("/api/orders", orderRequest("inventory-degraded@example.com", 11L, 2));

        assertThat(status(response)).isEqualTo(201);
        assertThat(readMap(response).get("status")).isEqualTo("PENDING_CONFIRMATION");
    }

    @Test
    void shouldRejectBusinessInventoryFailureWithoutSavingPendingOrder() throws Exception {
        when(productClient.findById(12L)).thenReturn(new ProductResponse(12L, "Plug", new BigDecimal("50.00"), true));
        when(inventoryClient.reserve(any())).thenReturn(new InventoryResponse(false, 0, "insufficient stock"));
        long ordersBefore = orderRepository.count();

        MvcResult response = postJson("/api/orders", orderRequest("business-failure@example.com", 12L, 100));

        assertThat(status(response)).isEqualTo(422);
        assertThat(orderRepository.count()).isEqualTo(ordersBefore);
    }

    private static CreateOrderRequest orderRequest(String email, long productId, int quantity) {
        return new CreateOrderRequest("Buyer", email,
                List.of(new OrderItemRequest(productId, "Ignored client snapshot", quantity, BigDecimal.ONE)));
    }

    private MvcResult postJson(String url, Object body) throws Exception {
        return mvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)))
                .andReturn();
    }

    private static int status(MvcResult result) {
        return result.getResponse().getStatus();
    }

    private Map<String, Object> readMap(MvcResult result) throws Exception {
        return json.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });
    }

    private List<Map<String, Object>> readList(MvcResult result) throws Exception {
        return json.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });
    }

    private static Long asLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static BigDecimal asDecimal(Object value) {
        return new BigDecimal(value.toString());
    }
}
