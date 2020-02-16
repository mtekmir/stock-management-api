alter table sales add column status varchar;

update sales set status = 'Sale Completed' where status is null;

alter table sales alter column status set not null;