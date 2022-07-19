ALTER TABLE t_cert_to_upload ADD COLUMN pk_slot integer not null default 0;

ALTER TABLE t_cert_to_upload DROP CONSTRAINT pk_t_cert_to_upload;
ALTER TABLE t_cert_to_upload ADD CONSTRAINT pk_t_cert_to_upload PRIMARY KEY (pk_alias, pk_slot);