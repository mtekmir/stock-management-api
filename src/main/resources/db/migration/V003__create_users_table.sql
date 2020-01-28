create table if not exists users(
  id uuid constraint users_pkey primary key,
  email varchar not null,
  name varchar not null,
  password varchar not null
)