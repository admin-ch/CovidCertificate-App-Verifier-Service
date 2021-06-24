/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2021. All rights reserved.
 */

CREATE TABLE t_app_tokens
(
    api_key     character varying(50)  NOT NULL,
    description character varying(100) NOT NULL,
    CONSTRAINT pk_t_app_tokens PRIMARY KEY (api_key)
);