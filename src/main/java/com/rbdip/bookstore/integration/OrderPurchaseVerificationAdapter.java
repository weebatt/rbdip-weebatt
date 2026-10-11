package com.rbdip.bookstore.integration;

import com.rbdip.bookstore.order.OrderItemRepository;
import com.rbdip.bookstore.review.PurchaseVerification;
import org.springframework.stereotype.Component;

@Component
public class OrderPurchaseVerificationAdapter implements PurchaseVerification {

    private final OrderItemRepository orderItemRepository;

    public OrderPurchaseVerificationAdapter(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public boolean hasPurchaseForProduct(Long productId) {
        return orderItemRepository.existsByProduct_Id(productId);
    }
}
