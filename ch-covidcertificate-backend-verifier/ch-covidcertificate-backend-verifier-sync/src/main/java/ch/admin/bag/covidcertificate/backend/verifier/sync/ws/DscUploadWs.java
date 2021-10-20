/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.backend.verifier.sync.ws;

import ch.admin.bag.covidcertificate.backend.verifier.sync.exception.InvalidEcKeySizeException;
import ch.admin.bag.covidcertificate.backend.verifier.sync.syncer.DscUploadClient;
import ch.admin.bag.covidcertificate.backend.verifier.sync.utils.UnexpectedAlgorithmException;
import ch.admin.bag.covidcertificate.backend.verifier.sync.ws.model.DscUploadResponse;
import ch.ubique.openapi.docannotations.Documentation;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.operator.OperatorCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("dsc/upload")
public class DscUploadWs {
    private static final Logger logger = LoggerFactory.getLogger(DscUploadWs.class);
    private final DscUploadClient dscUploadClient;

    public DscUploadWs(DscUploadClient dscUploadClient) {
        this.dscUploadClient = dscUploadClient;
    }

    @Documentation(
            description = "Echo endpoint",
            responses = {"200 => Hello from internal dsc upload WS"})
    @GetMapping(value = "")
    public @ResponseBody String hello() {
        return "Hello from internal DSC upload WS";
    }

    @Documentation(description = "internal endpoint for triggering dsc upload")
    @GetMapping(value = "trigger", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<DscUploadResponse> triggerDscUpload()
            throws UnexpectedAlgorithmException, CertificateException, IOException,
                    NoSuchAlgorithmException, OperatorCreationException, CMSException,
                    InvalidEcKeySizeException {
        logger.info("uploading dscs");
        return ResponseEntity.ok(dscUploadClient.uploadDscs());
    }
}
