package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PricingCalculatorTest {

    private final PricingCalculator calculator = new PricingCalculator();

    @Test
    void returnsZeroForEmptyOrder() {
        assertTotal(List.of(), "regular", null, "0.00");
    }

    @Test
    void addsMultipleOrderLines() {
        assertTotal(
                List.of(line("10.00", 2), line("5.00", 3)),
                "regular",
                null,
                "35.00");
    }

    @Test
    void appliesBulkDiscountOnlyAboveTenItems() {
        assertTotal(List.of(line("10.00", 10)), "regular", null, "100.00");
        assertTotal(List.of(line("10.00", 11)), "regular", null, "104.50");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "regular, 100.00",
        "vip, 90.00",
        "wholesale, 85.00",
        "unknown, 100.00",
        "NULL, 100.00"
    }, nullValues = "NULL")
    void appliesDiscountForKnownCustomerTypes(String customerType, String expected) {
        assertTotal(List.of(line("100.00", 1)), customerType, null, expected);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "SAVE10, 90.00",
        "SAVE20PERCENT, 80.00",
        "UNKNOWN, 100.00",
        "NULL, 100.00"
    }, nullValues = "NULL")
    void appliesKnownCoupons(String couponCode, String expected) {
        assertTotal(List.of(line("100.00", 1)), "regular", couponCode, expected);
    }

    @Test
    void preventsFixedCouponFromCreatingNegativeTotal() {
        assertTotal(List.of(line("5.00", 1)), "regular", "SAVE10", "0.00");
    }

    @Test
    void appliesLargeOrderDiscountOnlyAboveThreshold() {
        assertTotal(List.of(line("1000.00", 1)), "regular", null, "1000.00");
        assertTotal(List.of(line("1000.01", 1)), "regular", null, "980.01");
    }

    @Test
    void preservesDiscountApplicationOrder() {
        assertTotal(List.of(line("100.00", 11)), "vip", "SAVE20PERCENT", "752.40");
    }

    @Test
    void roundsMonetaryResultHalfUpToTwoDecimalPlaces() {
        assertTotal(List.of(line("1.005", 1)), "regular", null, "1.01");
    }

    private void assertTotal(
            List<PricingCalculator.LineItem> items,
            String customerType,
            String couponCode,
            String expected) {
        BigDecimal result = calculator.calculateOrderTotal(items, customerType, couponCode);
        assertThat(result).isEqualByComparingTo(expected);
    }

    private PricingCalculator.LineItem line(String price, int quantity) {
        return new PricingCalculator.LineItem(new BigDecimal(price), quantity);
    }
}
