alter table t_country_specific_certificate_authority add column deleted_at timestamp with time zone;
alter table t_document_signer_certificate add column deleted_at timestamp with time zone;