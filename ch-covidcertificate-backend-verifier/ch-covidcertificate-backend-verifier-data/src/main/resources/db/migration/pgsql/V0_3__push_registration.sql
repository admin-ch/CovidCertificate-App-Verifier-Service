/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2021. All rights reserved.
 */

CREATE TABLE t_push_registration
(
    pk_push_registration_id integer                NOT NULL GENERATED ALWAYS AS IDENTITY,
    push_type               character varying(20)  NOT NULL,
    push_token              character varying(100) NOT NULL,
    device_id               character varying(255) NOT NULL,
    CONSTRAINT pk_t_push_registration PRIMARY KEY (pk_push_registration_id),
    CONSTRAINT u_push_token UNIQUE (push_token),
    CONSTRAINT u_device_id UNIQUE (device_id)
);