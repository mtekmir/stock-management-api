update products set price = 0 where price isnull;

alter table products alter column price set not null;