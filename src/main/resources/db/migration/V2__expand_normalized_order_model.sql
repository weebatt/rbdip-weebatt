CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    phone VARCHAR(50)
);

CREATE UNIQUE INDEX uq_customers_legacy_identity
    ON customers (full_name, address, phone) NULLS NOT DISTINCT;

ALTER TABLE orders ADD COLUMN customer_id BIGINT;
ALTER TABLE order_items ADD COLUMN product_id BIGINT;

INSERT INTO customers (full_name, address, phone)
SELECT DISTINCT customer_full_name, customer_address, customer_phone
FROM orders;

UPDATE orders AS target
SET customer_id = customer.id
FROM customers AS customer
WHERE customer.full_name = target.customer_full_name
  AND customer.address IS NOT DISTINCT FROM target.customer_address
  AND customer.phone IS NOT DISTINCT FROM target.customer_phone;

UPDATE order_items AS item
SET product_id = (
    SELECT min(product.id)
    FROM products AS product
    WHERE product.name = item.product_name
      AND product.price = item.product_price
);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM orders WHERE customer_id IS NULL) THEN
        RAISE EXCEPTION 'Cannot map every order to a customer';
    END IF;
    IF EXISTS (SELECT 1 FROM order_items WHERE product_id IS NULL) THEN
        RAISE EXCEPTION 'Cannot map every order item to a product';
    END IF;
END
$$;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_customer
    FOREIGN KEY (customer_id) REFERENCES customers(id);
ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_product
    FOREIGN KEY (product_id) REFERENCES products(id);

CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

CREATE FUNCTION sync_order_customer_legacy_columns()
RETURNS trigger AS $$
BEGIN
    IF NEW.customer_id IS NULL THEN
        INSERT INTO customers (full_name, address, phone)
        VALUES (NEW.customer_full_name, NEW.customer_address, NEW.customer_phone)
        ON CONFLICT DO NOTHING;

        SELECT id INTO NEW.customer_id
        FROM customers
        WHERE full_name = NEW.customer_full_name
          AND address IS NOT DISTINCT FROM NEW.customer_address
          AND phone IS NOT DISTINCT FROM NEW.customer_phone
        LIMIT 1;
    ELSE
        SELECT full_name, address, phone
        INTO NEW.customer_full_name, NEW.customer_address, NEW.customer_phone
        FROM customers
        WHERE id = NEW.customer_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_orders_customer_compatibility
BEFORE INSERT OR UPDATE ON orders
FOR EACH ROW EXECUTE FUNCTION sync_order_customer_legacy_columns();

CREATE FUNCTION sync_order_item_product_legacy_columns()
RETURNS trigger AS $$
BEGIN
    IF NEW.product_id IS NULL THEN
        SELECT min(id) INTO NEW.product_id
        FROM products
        WHERE name = NEW.product_name
          AND price = NEW.product_price;
    ELSE
        SELECT name, price
        INTO NEW.product_name, NEW.product_price
        FROM products
        WHERE id = NEW.product_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_order_items_product_compatibility
BEFORE INSERT OR UPDATE ON order_items
FOR EACH ROW EXECUTE FUNCTION sync_order_item_product_legacy_columns();

ALTER TABLE orders ALTER COLUMN customer_id SET NOT NULL;
ALTER TABLE order_items ALTER COLUMN product_id SET NOT NULL;
