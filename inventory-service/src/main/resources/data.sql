INSERT INTO products (id, name, description, price, stock_quantity, version, created_at, updated_at)
VALUES
    (1,  'Laptop Pro 15',               'High-performance laptop with 16GB RAM',  1299.99, 9999, 0, NOW(), NOW()),
    (2,  'Wireless Mouse',              'Ergonomic wireless mouse',                  29.99, 9999, 0, NOW(), NOW()),
    (3,  'Mechanical Keyboard',         'RGB mechanical gaming keyboard',             89.99, 9999, 0, NOW(), NOW()),
    (4,  '27" Monitor',                 '4K UHD monitor with HDR',                  399.99, 9999, 0, NOW(), NOW()),
    (5,  'USB-C Hub',                   '7-in-1 USB-C hub',                           49.99, 9999, 0, NOW(), NOW()),
    (6,  'Webcam HD',                   '1080p webcam with microphone',               79.99, 9999, 0, NOW(), NOW()),
    (7,  'External SSD 1TB',            'Portable external SSD',                     149.99, 9999, 0, NOW(), NOW()),
    (8,  'Noise-Cancelling Headphones', 'Over-ear wireless headphones',              249.99, 9999, 0, NOW(), NOW()),
    (9,  'Desk Lamp',                   'LED desk lamp with adjustable brightness',   39.99, 9999, 0, NOW(), NOW()),
    (10, 'Phone Stand',                 'Adjustable phone and tablet stand',          19.99, 9999, 0, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET
    stock_quantity = EXCLUDED.stock_quantity,
    updated_at     = NOW();

-- Reset the sequence so new auto-generated IDs don't collide with the seeded rows
SELECT setval(pg_get_serial_sequence('products', 'id'), (SELECT MAX(id) FROM products));
