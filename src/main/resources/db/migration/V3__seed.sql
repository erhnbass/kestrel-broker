INSERT INTO customers (username, password, role)
VALUES (
           'C1',
           '{bcrypt}$2a$12$7ZJPxyIsck1z.RWSgIFNxeYN5ZwlvOSqS30SjLeLtrO/BEdgDwbO6', -- password = 1234
           'CUSTOMER'
       );

INSERT INTO asset (customer_id, asset_name, size, usable_size)
VALUES
    ('C1', 'TRY', 1000000, 1000000),
    ('C1', 'AAPL', 100, 100);


INSERT INTO customers (username, password, role)
VALUES ('C2', '{bcrypt}$2a$12$7ZJPxyIsck1z.RWSgIFNxeYN5ZwlvOSqS30SjLeLtrO/BEdgDwbO6', 'CUSTOMER');

INSERT INTO asset (customer_id, asset_name, size, usable_size)
VALUES
    ('C2', 'TRY', 1000000, 1000000),
    ('C2', 'AAPL', 100, 100);


