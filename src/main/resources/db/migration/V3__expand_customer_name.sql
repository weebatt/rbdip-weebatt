ALTER TABLE customers ADD COLUMN first_name VARCHAR(255);
ALTER TABLE customers ADD COLUMN last_name VARCHAR(255);

WITH normalized_names AS (
    SELECT
        id,
        btrim(regexp_replace(full_name, '\s+', ' ', 'g')) AS normalized_name
    FROM customers
)
UPDATE customers AS customer
SET first_name = split_part(source.normalized_name, ' ', 1),
    last_name = CASE
        WHEN strpos(source.normalized_name, ' ') > 0
            THEN substr(source.normalized_name, strpos(source.normalized_name, ' ') + 1)
        ELSE NULL
    END
FROM normalized_names AS source
WHERE source.id = customer.id;

CREATE FUNCTION sync_customer_name_versions()
RETURNS trigger AS $$
DECLARE
    normalized_name TEXT;
BEGIN
    IF NEW.first_name IS NULL THEN
        normalized_name := btrim(regexp_replace(NEW.full_name, '\s+', ' ', 'g'));
        NEW.first_name := split_part(normalized_name, ' ', 1);
        NEW.last_name := CASE
            WHEN strpos(normalized_name, ' ') > 0
                THEN substr(normalized_name, strpos(normalized_name, ' ') + 1)
            ELSE NULL
        END;
    ELSE
        NEW.full_name := concat_ws(' ', NEW.first_name, nullif(NEW.last_name, ''));
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_customers_name_compatibility
BEFORE INSERT OR UPDATE ON customers
FOR EACH ROW EXECUTE FUNCTION sync_customer_name_versions();

CREATE OR REPLACE FUNCTION sync_order_customer_legacy_columns()
RETURNS trigger AS $$
DECLARE
    normalized_name TEXT;
    parsed_first_name TEXT;
    parsed_last_name TEXT;
BEGIN
    IF NEW.customer_id IS NULL THEN
        normalized_name := btrim(regexp_replace(NEW.customer_full_name, '\s+', ' ', 'g'));
        parsed_first_name := split_part(normalized_name, ' ', 1);
        parsed_last_name := CASE
            WHEN strpos(normalized_name, ' ') > 0
                THEN substr(normalized_name, strpos(normalized_name, ' ') + 1)
            ELSE NULL
        END;

        INSERT INTO customers (full_name, first_name, last_name, address, phone)
        VALUES (
            normalized_name,
            parsed_first_name,
            parsed_last_name,
            NEW.customer_address,
            NEW.customer_phone)
        ON CONFLICT DO NOTHING;

        SELECT id INTO NEW.customer_id
        FROM customers
        WHERE full_name = normalized_name
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

ALTER TABLE customers ALTER COLUMN first_name SET NOT NULL;

CREATE INDEX idx_customers_structured_name
    ON customers(first_name, last_name);
