-- Demo catalog and stock. INSERT IGNORE avoids resetting application state
-- when the persistent MySQL database is reused across runs.

INSERT IGNORE INTO categories (id, uri, title) VALUES
    ('1', 'books', 'books'),
    ('2', 'games-toys', 'games and toys'),
    ('3', 'others', 'others');

INSERT IGNORE INTO products (id, title, description, price) VALUES
    ('1', 'Domain-Driven Design', 'by Eric Evans', 45.00),
    ('2', 'Object Thinking', 'by David West', 35.00),
    ('3', 'Release It!', 'by Michael Nygard', 32.50),
    ('4', 'Chess', 'Deluxe edition of the classic game.', 3.20),
    ('5', 'Domino', 'In black or white.', 1.50),
    ('6', 'Klein bottle', 'Two-dimensional manifold made from glass.', 25.00);

INSERT IGNORE INTO products_in_categories (product_id, category_id) VALUES
    ('1', '1'),
    ('2', '1'),
    ('3', '1'),
    ('4', '2'),
    ('5', '2'),
    ('6', '3');

INSERT IGNORE INTO products_in_stock (product_id, amount) VALUES
    ('1', 5),
    ('2', 0),
    ('3', 13),
    ('4', 55),
    ('5', 102),
    ('6', 1);

-- Demo users. Passwords are synthetic and for local/demo login only:
-- ami / Ami@1234, jason / Jason@1234.
INSERT IGNORE INTO users (username, password_hash, first_name, last_name, phone, address, email) VALUES
    ('ami', '$2y$10$G.WEppl09rdxnkVTqX8x1ugTDZLj3nUXtL/MrCnnc8hBbOGyiz87K', 'Ami', 'Tanaka', '+1 555 0101', '12 Sakura Lane, Springfield', 'ami@example.com'),
    ('jason', '$2y$10$.kSu7tWaO/.69fKjAsb5KuIQR/PVIx2GhPeHFs1AZUiiHMAhdosJ.', 'Jason', 'Lee', '+1 555 0102', '48 Maple Street, Springfield', 'jason@example.com');
