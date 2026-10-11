package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;
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
class OrderQueryServiceTest {

    @Mock
    private OrderRepository orderRepository;

    private OrderQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new OrderQueryService(orderRepository);
    }

    @Test
    void mapsFetchedAggregateToApiResponse() {
        Customer customer = new Customer("Anna", "Sidorova", "SPb", null);
        Product product = new Product("Refactoring", new BigDecimal("40.00"), null);
        Order order = new Order(customer, "new");
        order.addItem(new OrderItem(order, product, 2));
        when(orderRepository.findAllWithDetails()).thenReturn(List.of(order));

        List<OrderResponse> result = queryService.listOrders();

        assertThat(result).singleElement().satisfies(response -> {
            assertThat(response.customerFullName()).isEqualTo("Anna Sidorova");
            assertThat(response.status()).isEqualTo("new");
            assertThat(response.items()).singleElement().satisfies(item -> {
                assertThat(item.productName()).isEqualTo("Refactoring");
                assertThat(item.quantity()).isEqualTo(2);
            });
        });
    }
}
