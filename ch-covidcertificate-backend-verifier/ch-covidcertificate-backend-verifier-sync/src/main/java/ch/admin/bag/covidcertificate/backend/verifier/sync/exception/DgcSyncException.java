// Copyright (c) 2021 Patrick Amrein <amrein@ubique.ch>
// 
// This software is released under the MIT License.
// https://opensource.org/licenses/MIT

package ch.admin.bag.covidcertificate.backend.verifier.sync.exception;

public class DgcSyncException extends Exception  {
    private Exception innerException;
    public DgcSyncException(Exception innerException) {
        this.innerException = innerException;
    }
    /**
     * @return the innerException
     */
    public Exception getInnerException() {
        return innerException;
    }
}
