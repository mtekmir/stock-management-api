create table if not exists sales_events(
  id bigserial unique primary key,
  created timestamp not null,
  event varchar not null,
  message text not null,
  "saleId" bigint not null references sales(id),
  "userId" uuid references users(id)
)