CREATE TABLE IF NOT EXISTS customer_order (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_name   VARCHAR(255)   NOT NULL,
    customer_email  VARCHAR(255)   NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    total_amount    DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_item (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT         NOT NULL,
    product_name VARCHAR(255)   NOT NULL,
    quantity     INT            NOT NULL,
    unit_price   DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id)
        REFERENCES customer_order (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_order_status ON customer_order (status);
CREATE INDEX IF NOT EXISTS idx_order_customer_email ON customer_order (customer_email);
