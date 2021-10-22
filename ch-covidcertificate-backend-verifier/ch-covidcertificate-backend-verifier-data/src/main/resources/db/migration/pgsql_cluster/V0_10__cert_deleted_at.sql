alter table t_country_specific_certificate_authority add column deleted_at timestamp with time zone;
alter table t_country_specific_certificate_authority drop constraint csca_unique_key_id;
alter table t_country_specific_certificate_authority add constraint csca_unique_key_id unique ( key_id, deleted_at );

alter table t_document_signer_certificate add column deleted_at timestamp with time zone;
alter table t_document_signer_certificate drop constraint dsc_unique_key_id;
alter table t_document_signer_certificate add constraint dsc_unique_key_id unique ( key_id, deleted_at );
