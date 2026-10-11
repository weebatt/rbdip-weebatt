package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rbdip.bookstore.customer.Customer;
import com.rbdip.bookstore.product.Product;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRequestValidator requestValidator;

    @Mock
    private OrderLineResolver orderLineResolver;

    @Mock
    private PricingCalculator pricingCalculator;

    @Mock
    private OrderPersistenceService persistenceService;

    @Mock
    private OrderConfirmationSender confirmationSender;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                requestValidator,
                orderLineResolver,
                pricingCalculator,
                persistenceService,
                confirmationSender);
    }

    @Test
    void orchestratesOrderCreationAndDefaultsCustomerType() {
        CreateOrderRequest request = request();
        Product product = new Product("Clean Code", new BigDecimal("35.00"), null);
        List<ResolvedOrderLine> lines = List.of(new ResolvedOrderLine(product, 2));
        BigDecimal total = new BigDecimal("70.00");
        Customer customer = new Customer("Ivan", "Petrov", "Moscow", null);
        Order savedOrder = new Order(customer, "new");
        when(orderLineResolver.resolve(request.items())).thenReturn(lines);
        when(pricingCalculator.calculateOrderTotal(
                        argThat(items -> items.size() == 1
                                && items.getFirst().price().compareTo(new BigDecimal("35.00")) == 0
                                && items.getFirst().quantity() == 2),
                        eq("regular"),
                        isNull()))
                .thenReturn(total);
        when(persistenceService.save(request, lines)).thenReturn(savedOrder);

        Order result = orderService.createOrder(request);

        assertThat(result).isSameAs(savedOrder);
        verify(requestValidator).validate(request);
        verify(confirmationSender).send("Ivan Petrov", savedOrder.getId(), total);
    }

    @Test
    void stopsWhenValidationFails() {
        CreateOrderRequest request = request();
        doThrow(new IllegalArgumentException("invalid")).when(requestValidator).validate(request);

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid");

        verifyNoInteractions(orderLineResolver, pricingCalculator, persistenceService, confirmationSender);
    }

    private CreateOrderRequest request() {
        return new CreateOrderRequest(
                "Ivan Petrov", "Moscow", null, null, null,
                List.of(new CreateOrderRequest.Item(1L, 2)));
    }
}
