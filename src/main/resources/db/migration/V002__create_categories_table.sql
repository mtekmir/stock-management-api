create table if not exists categories(
  id bigserial unique primary key,
  name varchar unique not null
)