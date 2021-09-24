CREATE TABLE t_cert_to_upload
(
    pk_alias    character varying(100) NOT NULL,
    uploaded_at timestamp with time zone,
    inserted_at timestamp with time zone,
    CONSTRAINT pk_t_cert_to_upload PRIMARY KEY (pk_alias)
);

CREATE INDEX idx_cert_to_upload_uploaded_at ON t_cert_to_upload (uploaded_at);
CREATE INDEX idx_cert_to_upload_inserted_at ON t_cert_to_upload (inserted_at);