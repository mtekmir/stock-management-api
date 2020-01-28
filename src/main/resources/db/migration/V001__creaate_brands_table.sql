create table if not exists brands(
  id bigserial unique primary key,
  name varchar unique not null
)