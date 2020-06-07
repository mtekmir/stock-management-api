alter table inventory_count_products add column is_new bool;

update inventory_count_products set is_new = false;

alter table inventory_count_products alter column is_new set not null;