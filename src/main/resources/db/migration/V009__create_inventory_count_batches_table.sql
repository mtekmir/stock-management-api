create table if not exists inventory_count_batches(
  id bigserial unique primary key,
  status varchar not null,
  started timestamp not null,
  finished timestamp,
  name varchar,
  "categoryId" bigint references categories(id),
  "brandId" bigint references brands(id)
)