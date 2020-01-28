create table if not exists sold_products(
  id bigserial unique primary key,
  qty int not null,
  synced boolean not null,
  "productId" bigint not null references products(id),
  "saleId" bigint not null references sales(id)
)