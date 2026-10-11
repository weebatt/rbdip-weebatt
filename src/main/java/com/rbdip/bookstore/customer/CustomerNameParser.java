package com.rbdip.bookstore.customer;

import org.springframework.stereotype.Component;

@Component
public class CustomerNameParser {

    private static final int NAME_PARTS_LIMIT = 2;

    public CustomerName parse(String fullName) {
        String normalized = fullName.trim().replaceAll("\\s+", " ");
        String[] parts = normalized.split(" ", NAME_PARTS_LIMIT);
        String lastName = parts.length == NAME_PARTS_LIMIT ? parts[1] : null;
        return new CustomerName(parts[0], lastName);
    }
}
