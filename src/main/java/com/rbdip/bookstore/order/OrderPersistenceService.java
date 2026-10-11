package com.rbdip.bookstore.order;

import com.rbdip.bookstore.customer.Customer;
import com.rbdip.bookstore.customer.CustomerName;
import com.rbdip.bookstore.customer.CustomerNameParser;
import com.rbdip.bookstore.customer.CustomerRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderPersistenceService {

    private static final String NEW_ORDER_STATUS = "new";

    private final CustomerRepository customerRepository;
    private final CustomerNameParser customerNameParser;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderPersistenceService(
            CustomerRepository customerRepository,
            CustomerNameParser customerNameParser,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository) {
        this.customerRepository = customerRepository;
        this.customerNameParser = customerNameParser;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public Order save(CreateOrderRequest request, List<ResolvedOrderLine> lines) {
        Customer customer = findOrCreateCustomer(request);
        Order order = orderRepository.save(new Order(customer, NEW_ORDER_STATUS));
        List<OrderItem> items = lines.stream()
                .map(line -> new OrderItem(order, line.product(), line.quantity()))
                .toList();
        items.forEach(order::addItem);
        orderItemRepository.saveAll(items);
        return order;
    }

    private Customer findOrCreateCustomer(CreateOrderRequest request) {
        CustomerName name = customerNameParser.parse(request.customerFullName());
        return customerRepository.findFirstByFirstNameAndLastNameAndAddressAndPhone(
                        name.firstName(), name.lastName(), request.customerAddress(), request.customerPhone())
                .orElseGet(() -> customerRepository.save(new Customer(
                        name.firstName(), name.lastName(), request.customerAddress(), request.customerPhone())));
    }
}
