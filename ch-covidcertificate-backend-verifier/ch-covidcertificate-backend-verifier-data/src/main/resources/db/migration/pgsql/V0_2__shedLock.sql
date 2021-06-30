/*
 * Created by Ubique Innovation AG
 * https://www.ubique.ch
 * Copyright (c) 2021. All rights reserved.
 */

/*
 Do not manually delete lock row from the DB table. ShedLock has an in-memory cache of existing lock
 rows so the row will NOT be automatically recreated until application restart. If you need to, you
 can edit the row/document, risking only that multiple locks will be held.
 */

CREATE TABLE t_shedlock
(
    name       VARCHAR(64),
    lock_until TIMESTAMP(3) NULL,
    locked_at  TIMESTAMP(3) NULL,
    locked_by  VARCHAR(255),
    CONSTRAINT pk_t_shedlock PRIMARY KEY (name)
);