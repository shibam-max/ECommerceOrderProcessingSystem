-- Seed data: pre-populate a few sample orders so the API has data on startup.
-- Useful for demos, Swagger UI testing, and reviewer walkthroughs.

INSERT INTO customer_order (customer_name, customer_email, status, total_amount, created_at, updated_at)
VALUES ('Alice Wonderland', 'alice@example.com', 'PENDING', 1050.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO order_item (order_id, product_name, quantity, unit_price) VALUES (1, 'Laptop', 1, 999.99);
INSERT INTO order_item (order_id, product_name, quantity, unit_price) VALUES (1, 'Mouse', 2, 25.50);

INSERT INTO customer_order (customer_name, customer_email, status, total_amount, created_at, updated_at)
VALUES ('Bob Builder', 'bob@example.com', 'PROCESSING', 45.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO order_item (order_id, product_name, quantity, unit_price) VALUES (2, 'Hammer', 3, 15.00);

INSERT INTO customer_order (customer_name, customer_email, status, total_amount, created_at, updated_at)
VALUES ('Charlie Brown', 'charlie@example.com', 'SHIPPED', 199.98, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO order_item (order_id, product_name, quantity, unit_price) VALUES (3, 'Headphones', 2, 99.99);

INSERT INTO customer_order (customer_name, customer_email, status, total_amount, created_at, updated_at)
VALUES ('Diana Prince', 'diana@example.com', 'PENDING', 35.97, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO order_item (order_id, product_name, quantity, unit_price) VALUES (4, 'Notebook', 3, 11.99);
