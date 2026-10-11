package com.rbdip.bookstore.order;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderRequestValidator {

    public void validate(CreateOrderRequest request) {
        if (isBlank(request.customerFullName())) {
            throw new IllegalArgumentException("customerFullName is required");
        }
        if (isBlank(request.customerAddress())) {
            throw new IllegalArgumentException("customerAddress is required");
        }
        if (isEmpty(request.items())) {
            throw new IllegalArgumentException("order must contain at least one item");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isEmpty(List<CreateOrderRequest.Item> items) {
        return items == null || items.isEmpty();
    }
}
