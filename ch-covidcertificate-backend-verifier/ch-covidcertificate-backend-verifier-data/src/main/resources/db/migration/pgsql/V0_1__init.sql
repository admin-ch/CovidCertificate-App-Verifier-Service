/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2021. All rights reserved.
 */

CREATE TABLE t_country_specific_certificate_authority
(
    pk_csca_id      serial NOT NULL,
    key_id          character varying(20) NOT NULL,
    certificate_raw  text NOT NULL,
    imported_at     timestamp with time zone NOT NULL DEFAULT now(),
    origin          character varying(10) NOT NULL,
    subject_principal_name  character varying(255) NOT NULL,
    CONSTRAINT PK_t_country_specific_certificate_authority PRIMARY KEY ( pk_csca_id ),
    CONSTRAINT csca_unique_key_id UNIQUE ( key_id )
);

CREATE TABLE t_document_signer_certificate
(
    pk_dsc_id               serial NOT NULL,
    key_id                  character varying(20) NOT NULL,
    fk_csca_id              integer NOT NULL,
    certificate_raw          text NOT NULL,
    imported_at             timestamp with time zone NOT NULL DEFAULT now(),
    origin                  character varying(10) NOT NULL,
    use                     character varying(10) NOT NULL,
    alg                     character varying(10) NOT NULL,
    n                       character varying(1000),
    e                       character varying(255),
    subject_public_key_info character varying(1500),
    crv                     character varying(10),
    x                       character varying(255),
    y                       character varying(255),
    CONSTRAINT PK_t_document_signer_certificate PRIMARY KEY ( pk_dsc_id ),
    CONSTRAINT dsc_unique_key_id UNIQUE ( key_id ),
    CONSTRAINT FK_45 FOREIGN KEY ( fk_csca_id ) REFERENCES t_country_specific_certificate_authority ( pk_csca_id )
);

CREATE INDEX fkIdx_46 ON t_document_signer_certificate ( fk_csca_id );