/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2021. All rights reserved.
 */

alter table t_country_specific_certificate_authority add column source character varying(10);
update t_country_specific_certificate_authority set source = 'SYNC';
alter table t_country_specific_certificate_authority alter column source set not null;
update t_country_specific_certificate_authority set source = 'MANUAL' where origin = 'CH';

alter table t_document_signer_certificate add column source character varying(10);
update t_document_signer_certificate set source = 'SYNC';
alter table t_document_signer_certificate alter column source set not null;
update t_document_signer_certificate set source = 'MANUAL' where origin = 'CH';
