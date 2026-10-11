DROP TRIGGER trg_orders_customer_compatibility ON orders;
DROP FUNCTION sync_order_customer_legacy_columns();

DROP TRIGGER trg_order_items_product_compatibility ON order_items;
DROP FUNCTION sync_order_item_product_legacy_columns();

DROP TRIGGER trg_customers_name_compatibility ON customers;
DROP FUNCTION sync_customer_name_versions();

ALTER TABLE orders
    DROP COLUMN customer_full_name,
    DROP COLUMN customer_address,
    DROP COLUMN customer_phone;

ALTER TABLE order_items
    DROP COLUMN product_name,
    DROP COLUMN product_price;

DROP INDEX uq_customers_legacy_identity;
ALTER TABLE customers DROP COLUMN full_name;

CREATE UNIQUE INDEX uq_customers_identity
    ON customers (first_name, last_name, address, phone) NULLS NOT DISTINCT;
