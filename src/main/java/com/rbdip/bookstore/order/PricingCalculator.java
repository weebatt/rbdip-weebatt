package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Рассчитывает итог заказа по установленной последовательности скидок.
 * Все ветви алгоритма должны быть зафиксированы характеризационными
 * тестами в ЛР2 и усилены mutation-тестами в ЛР5.
 */
@Component
public class PricingCalculator {

    private static final int BULK_QUANTITY_THRESHOLD = 10;
    private static final BigDecimal BULK_DISCOUNT_FACTOR = new BigDecimal("0.95");
    private static final String VIP_CUSTOMER_TYPE = "vip";
    private static final BigDecimal VIP_DISCOUNT_FACTOR = new BigDecimal("0.90");
    private static final String WHOLESALE_CUSTOMER_TYPE = "wholesale";
    private static final BigDecimal WHOLESALE_DISCOUNT_FACTOR = new BigDecimal("0.85");
    private static final String FIXED_DISCOUNT_COUPON = "SAVE10";
    private static final BigDecimal FIXED_DISCOUNT_AMOUNT = BigDecimal.TEN;
    private static final String PERCENT_DISCOUNT_COUPON = "SAVE20PERCENT";
    private static final BigDecimal PERCENT_DISCOUNT_FACTOR = new BigDecimal("0.80");
    private static final BigDecimal LARGE_ORDER_THRESHOLD = new BigDecimal("1000");
    private static final BigDecimal LARGE_ORDER_DISCOUNT_FACTOR = new BigDecimal("0.98");
    private static final int MONEY_SCALE = 2;

    public record LineItem(BigDecimal price, int quantity) {
    }

    public BigDecimal calculateOrderTotal(List<LineItem> items, String customerType, String couponCode) {
        BigDecimal total = BigDecimal.ZERO;

        for (LineItem item : items) {
            BigDecimal linePrice = item.price().multiply(BigDecimal.valueOf(item.quantity()));
            if (item.quantity() > BULK_QUANTITY_THRESHOLD) {
                linePrice = linePrice.multiply(BULK_DISCOUNT_FACTOR);
            }
            total = total.add(linePrice);
        }

        if (VIP_CUSTOMER_TYPE.equals(customerType)) {
            total = total.multiply(VIP_DISCOUNT_FACTOR);
        } else if (WHOLESALE_CUSTOMER_TYPE.equals(customerType)) {
            total = total.multiply(WHOLESALE_DISCOUNT_FACTOR);
        }

        if (FIXED_DISCOUNT_COUPON.equals(couponCode)) {
            total = total.subtract(FIXED_DISCOUNT_AMOUNT);
        } else if (PERCENT_DISCOUNT_COUPON.equals(couponCode)) {
            total = total.multiply(PERCENT_DISCOUNT_FACTOR);
        }

        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        if (total.compareTo(LARGE_ORDER_THRESHOLD) > 0) {
            total = total.multiply(LARGE_ORDER_DISCOUNT_FACTOR);
        }

        return total.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
