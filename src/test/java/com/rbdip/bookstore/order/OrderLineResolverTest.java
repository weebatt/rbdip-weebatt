package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.rbdip.bookstore.product.Product;
import com.rbdip.bookstore.product.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderLineResolverTest {

    private static final Long PRODUCT_ID = 7L;

    @Mock
    private ProductRepository productRepository;

    private OrderLineResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new OrderLineResolver(productRepository);
    }

    @Test
    void resolvesProductAndUsesDefaultQuantity() {
        Product product = new Product("Clean Code", new BigDecimal("35.00"), null);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        List<ResolvedOrderLine> result = resolver.resolve(
                List.of(new CreateOrderRequest.Item(PRODUCT_ID, null)));

        assertThat(result).singleElement().satisfies(line -> {
            assertThat(line.product()).isSameAs(product);
            assertThat(line.quantity()).isEqualTo(1);
        });
    }

    @Test
    void rejectsUnknownProduct() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(List.of(new CreateOrderRequest.Item(PRODUCT_ID, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("product 7 not found");
    }

    @Test
    void rejectsNonPositiveQuantity() {
        Product product = new Product("Clean Code", new BigDecimal("35.00"), null);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> resolver.resolve(List.of(new CreateOrderRequest.Item(PRODUCT_ID, 0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("quantity must be positive");
    }
}
