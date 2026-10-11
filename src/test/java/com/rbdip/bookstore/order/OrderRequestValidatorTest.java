package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class OrderRequestValidatorTest {

    private final OrderRequestValidator validator = new OrderRequestValidator();

    @Test
    void acceptsValidRequest() {
        CreateOrderRequest request = request("Ivan Petrov", "Moscow", List.of(item()));

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingCustomerName() {
        CreateOrderRequest request = request(" ", "Moscow", List.of(item()));

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("customerFullName is required");
    }

    @Test
    void rejectsMissingCustomerAddress() {
        CreateOrderRequest request = request("Ivan Petrov", null, List.of(item()));

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("customerAddress is required");
    }

    @Test
    void rejectsEmptyOrder() {
        CreateOrderRequest request = request("Ivan Petrov", "Moscow", List.of());

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("order must contain at least one item");
    }

    private CreateOrderRequest request(
            String customerName, String customerAddress, List<CreateOrderRequest.Item> items) {
        return new CreateOrderRequest(customerName, customerAddress, null, null, null, items);
    }

    private CreateOrderRequest.Item item() {
        return new CreateOrderRequest.Item(1L, 1);
    }
}
