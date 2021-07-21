/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2021. All rights reserved.
 */

CREATE TABLE t_revoked_cert
(
    pk_revoked_cert_id serial                       NOT NULL,
    uvci               character varying(50) UNIQUE NOT NULL,
    CONSTRAINT pk_t_revoked_cert PRIMARY KEY (pk_revoked_cert_id)
);