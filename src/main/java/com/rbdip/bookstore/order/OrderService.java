package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final String DEFAULT_CUSTOMER_TYPE = "regular";

    private final OrderRequestValidator requestValidator;
    private final OrderLineResolver orderLineResolver;
    private final PricingCalculator pricingCalculator;
    private final OrderPersistenceService persistenceService;
    private final OrderConfirmationSender confirmationSender;

    public OrderService(
            OrderRequestValidator requestValidator,
            OrderLineResolver orderLineResolver,
            PricingCalculator pricingCalculator,
            OrderPersistenceService persistenceService,
            OrderConfirmationSender confirmationSender) {
        this.requestValidator = requestValidator;
        this.orderLineResolver = orderLineResolver;
        this.pricingCalculator = pricingCalculator;
        this.persistenceService = persistenceService;
        this.confirmationSender = confirmationSender;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        requestValidator.validate(request);
        List<ResolvedOrderLine> lines = orderLineResolver.resolve(request.items());
        BigDecimal total = calculateTotal(request, lines);
        Order order = persistenceService.save(request, lines);
        confirmationSender.send(request.customerFullName(), order.getId(), total);
        return order;
    }

    private BigDecimal calculateTotal(CreateOrderRequest request, List<ResolvedOrderLine> lines) {
        List<PricingCalculator.LineItem> pricingItems = lines.stream()
                .map(ResolvedOrderLine::toPricingLineItem)
                .toList();
        String customerType = request.customerType() == null
                ? DEFAULT_CUSTOMER_TYPE
                : request.customerType();
        return pricingCalculator.calculateOrderTotal(pricingItems, customerType, request.couponCode());
    }
}
