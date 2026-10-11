package com.rbdip.bookstore.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.rbdip.bookstore.order.OrderItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderPurchaseVerificationAdapterTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderPurchaseVerificationAdapter adapter;

    @Test
    void delegatesToEfficientExistsQuery() {
        when(orderItemRepository.existsByProduct_Id(9L)).thenReturn(true);

        assertThat(adapter.hasPurchaseForProduct(9L)).isTrue();
    }
}
