create table if not exists ordered_products(
  id bigserial unique not null,
  qty int not null,
  synced boolean not null,
  "productId" bigint references products(id),
  "stockOrderId" bigint references stock_orders(id)
)