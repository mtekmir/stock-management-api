create table if not exists products(
  id bigserial unique primary key,
  barcode varchar not null,
  sku varchar not null,
  name varchar not null,
  price numeric(10,2),
  "discountPrice" numeric(10,2),
  qty int not null,
  variation varchar,
  "taxRate" int,
  "brandId" bigint references brands(id),
  "categoryId" bigint references categories(id)
)