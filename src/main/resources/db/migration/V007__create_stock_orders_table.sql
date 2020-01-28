create table if not exists stock_orders(
  id bigserial unique primary key,
  created timestamp not null
)