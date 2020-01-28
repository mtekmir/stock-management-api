create table if not exists inventory_count_products(
  id bigserial unique primary key,
  "batchId" bigint not null references inventory_count_batches(id),
  "productId" bigint not null references products(id),
  expected int not null,
  counted int,
  synced boolean not null
)