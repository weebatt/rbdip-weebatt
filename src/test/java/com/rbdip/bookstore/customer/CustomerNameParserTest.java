package com.rbdip.bookstore.customer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CustomerNameParserTest {

    private final CustomerNameParser parser = new CustomerNameParser();

    @Test
    void splitsFirstTokenFromRemainingName() {
        CustomerName name = parser.parse("  Anna   Maria Sidorova  ");

        assertThat(name.firstName()).isEqualTo("Anna");
        assertThat(name.lastName()).isEqualTo("Maria Sidorova");
    }

    @Test
    void supportsSinglePartName() {
        CustomerName name = parser.parse("Madonna");

        assertThat(name.firstName()).isEqualTo("Madonna");
        assertThat(name.lastName()).isNull();
    }
}
