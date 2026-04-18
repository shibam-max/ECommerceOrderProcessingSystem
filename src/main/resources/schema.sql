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

CREATE TABLE IF NOT EXISTS order_audit_log (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT       NOT NULL,
    event_type   VARCHAR(30)  NOT NULL,
    old_status   VARCHAR(20),
    new_status   VARCHAR(20),
    detail       VARCHAR(500),
    occurred_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_order FOREIGN KEY (order_id)
        REFERENCES customer_order (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS app_user (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50)  NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
    enabled  BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_order_status ON customer_order (status);
CREATE INDEX IF NOT EXISTS idx_order_customer_email ON customer_order (customer_email);
CREATE INDEX IF NOT EXISTS idx_audit_order_id ON order_audit_log (order_id);
