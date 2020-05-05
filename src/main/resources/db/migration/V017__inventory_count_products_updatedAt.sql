alter table inventory_count_products add column updated_at timestamp;

update inventory_count_products set updated_at = CURRENT_TIMESTAMP;

alter table inventory_count_products alter column updated_at set not null;