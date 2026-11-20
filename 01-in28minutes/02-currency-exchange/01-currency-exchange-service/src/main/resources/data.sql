-- Example rates loaded into the in-memory database after JPA creates the table.
INSERT INTO currency_exchange
(currency_from, currency_to, conversion_multiple, port)
VALUES
    ('USD', 'INR', 85.50, '8000'),
    ('EUR', 'INR', 92.75, '8000'),
    ('GBP', 'INR', 108.25, '8000'),
    ('AUD', 'INR', 55.80, '8000'),
    ('CAD', 'INR', 62.40, '8000'),
    ('USD', 'EUR', 0.92, '8000'),
    ('USD', 'GBP', 0.79, '8000'),
    ('INR', 'USD', 0.0117, '8000'),
    ('INR', 'EUR', 0.0108, '8000'),
    ('EUR', 'USD', 1.09, '8000');