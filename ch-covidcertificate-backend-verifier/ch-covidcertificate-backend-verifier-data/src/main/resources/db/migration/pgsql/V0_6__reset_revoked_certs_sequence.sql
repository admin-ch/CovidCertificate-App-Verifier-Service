/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2021. All rights reserved.
 */

delete from t_revoked_cert;
select setval('t_revoked_cert_pk_revoked_cert_id_seq', 1);