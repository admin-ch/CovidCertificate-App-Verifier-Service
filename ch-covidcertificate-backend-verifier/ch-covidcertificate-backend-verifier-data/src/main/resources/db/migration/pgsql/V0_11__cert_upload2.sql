ALTER TABLE t_cert_to_upload ADD COLUMN key_id character varying(20);
ALTER TABLE t_cert_to_upload ADD COLUMN do_upload boolean not null default false;
ALTER TABLE t_cert_to_upload ADD COLUMN do_insert boolean not null default false;

CREATE INDEX idx_cert_to_upload_key_id ON t_cert_to_upload (key_id);
CREATE INDEX idx_cert_to_upload_do_upload ON t_cert_to_upload (do_upload);
CREATE INDEX idx_cert_to_upload_do_insert ON t_cert_to_upload (do_insert);

UPDATE t_cert_to_upload SET do_insert = true, do_upload = true;
UPDATE t_cert_to_upload SET uploaded_at = null, do_upload = false where uploaded_at in ('2022-01-01 00:00:00+00', '2021-01-01 00:00:00+00');