create table if not exists sales (
  id bigserial unique primary key,
  created timestamp not null,
  total numeric(10,2) not null,
  discount numeric(10,2) not null,
  outlet varchar not null
)