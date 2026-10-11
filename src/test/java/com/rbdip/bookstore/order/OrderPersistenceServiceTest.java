package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rbdip.bookstore.customer.Customer;
import com.rbdip.bookstore.customer.CustomerNameParser;
import com.rbdip.bookstore.customer.CustomerRepository;
import com.rbdip.bookstore.product.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderPersistenceServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    private OrderPersistenceService persistenceService;

    @BeforeEach
    void setUp() {
        persistenceService = new OrderPersistenceService(
                customerRepository,
                new CustomerNameParser(),
                orderRepository,
                orderItemRepository);
    }

    @Test
    void savesOrderAndAllResolvedLines() {
        Customer customer = new Customer("Ivan", "Petrov", "Moscow", null);
        Order savedOrder = new Order(customer, "new");
        Product product = new Product("Clean Code", new BigDecimal("35.00"), null);
        CreateOrderRequest request = new CreateOrderRequest(
                "Ivan Petrov", "Moscow", null, "regular", null,
                List.of(new CreateOrderRequest.Item(1L, 2)));
        when(customerRepository.findFirstByFirstNameAndLastNameAndAddressAndPhone(
                        "Ivan", "Petrov", "Moscow", null))
                .thenReturn(Optional.of(customer));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        Order result = persistenceService.save(
                request,
                List.of(new ResolvedOrderLine(product, 2)));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        verify(orderItemRepository).saveAll(anyList());
        assertThat(result).isSameAs(savedOrder);
        assertThat(orderCaptor.getValue().getCustomerFullName()).isEqualTo("Ivan Petrov");
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo("new");
    }
}
