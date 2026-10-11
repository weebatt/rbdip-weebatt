package com.rbdip.bookstore.order;

import java.math.BigDecimal;

public interface OrderConfirmationSender {

    void send(String customerName, Long orderId, BigDecimal total);
}
