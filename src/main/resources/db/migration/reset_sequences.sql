SELECT setval('brands_id_seq', max(id)) FROM brands;
SELECT setval('categories_id_seq', max(id)) FROM categories;
SELECT setval('products_id_seq', max(id)) FROM products;
SELECT setval('sales_id_seq', max(id)) FROM sales;
SELECT setval('sold_products_id_seq', max(id)) FROM sold_products;
SELECT setval('stock_orders_id_seq', max(id)) FROM stock_orders;
SELECT setval('ordered_products_id_seq', max(id)) FROM ordered_products;
SELECT setval('sales_events_id_seq', max(id)) FROM sales_events;
