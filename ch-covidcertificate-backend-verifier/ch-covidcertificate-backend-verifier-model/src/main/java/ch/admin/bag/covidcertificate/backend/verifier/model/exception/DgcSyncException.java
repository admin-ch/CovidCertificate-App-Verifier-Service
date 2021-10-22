/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package ch.admin.bag.covidcertificate.backend.verifier.model.exception;

public class DgcSyncException extends Exception {
    public static final String EXCEPTION_TAG = "[FATAL ERROR] [DgcSyncException] [Gateway Sync Rollback]";
    private final Exception innerException;

    public DgcSyncException(Exception innerException) {
        this.innerException = innerException;
    }
    /** @return the innerException */
    public Exception getInnerException() {
        return innerException;
    }
}
