TRUNCATE TABLE orders;
TRUNCATE TABLE order_items;
TRUNCATE TABLE customer_orders;

INSERT INTO orders (id, total) VALUES
    ('1', 1000.00),
    ('2', 2000.00),
    ('3', 300.00),
    ('4', 400.00);

INSERT INTO order_items VALUES
    ('001', 1.00, 1),
    ('002', 2.00, 1),
    ('002', 3.00, 2),
    ('003', 1.00, 3),
    ('004', 1.00, 4);

INSERT INTO customer_orders VALUES
    ('1', 'ami', '2026-01-01 10:00:00'),
    ('2', 'ami', '2026-02-01 10:00:00'),
    ('3', 'jason', '2026-03-01 10:00:00');
