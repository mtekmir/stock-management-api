alter table sales add column payment_method varchar(20);

update sales set payment_method = 'Cash';

alter table sales alter column payment_method set not null;

