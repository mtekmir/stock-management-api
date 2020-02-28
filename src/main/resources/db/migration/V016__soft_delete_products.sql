alter table products add column deleted boolean;

update products set deleted = FALSE;

alter table products alter column deleted set not null;