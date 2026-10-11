package com.rbdip.bookstore.order;

import com.rbdip.bookstore.product.Product;
import com.rbdip.bookstore.product.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderLineResolver {

    private static final int DEFAULT_QUANTITY = 1;

    private final ProductRepository productRepository;

    public OrderLineResolver(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ResolvedOrderLine> resolve(List<CreateOrderRequest.Item> items) {
        return items.stream().map(item -> resolveLine(item)).toList();
    }

    private ResolvedOrderLine resolveLine(CreateOrderRequest.Item item) {
        Product product = productRepository.findById(item.productId())
                .orElseThrow(() -> new IllegalArgumentException("product " + item.productId() + " not found"));
        int quantity = item.quantity() == null ? DEFAULT_QUANTITY : item.quantity();
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        return new ResolvedOrderLine(product, quantity);
    }
}
