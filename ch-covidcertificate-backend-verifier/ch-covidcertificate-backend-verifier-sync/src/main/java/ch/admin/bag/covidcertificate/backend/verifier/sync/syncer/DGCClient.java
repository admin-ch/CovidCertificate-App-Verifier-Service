package ch.admin.bag.covidcertificate.backend.verifier.sync.syncer;

import ch.admin.bag.covidcertificate.backend.verifier.model.sync.CertificateType;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.ProblemReport;
import ch.admin.bag.covidcertificate.backend.verifier.model.sync.TrustList;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class DGCClient {

    private static final Logger logger = LoggerFactory.getLogger(DGCClient.class);
    private static final String UPLOAD_PATH = "/signerCertificate";
    private static final String DOWNLOAD_PATH = "/trustList/%s";
    private final String baseUrl;
    @Autowired RestTemplate rt;

    public DGCClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public TrustList[] download(CertificateType certType) {
        final var uri =
                UriComponentsBuilder.fromHttpUrl(
                                baseUrl + String.format(DOWNLOAD_PATH, certType.name()))
                        .build()
                        .toUri();
        final var request = RequestEntity.get(uri).headers(createDownloadHeaders()).build();
        final ResponseEntity<String> response;
        try {
            logger.info("Downloading certificates of type {}", certType.name());
            response = rt.exchange(request, String.class);
        } catch (HttpStatusCodeException e) {
            var responseBody = e.getResponseBodyAsString();
            logger.error("Error downloading certificates of type {}", certType.name());
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)
                    || e.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
                final ProblemReport problemReport;
                try {
                    problemReport = new ObjectMapper().readValue(responseBody, ProblemReport.class);
                    logger.error("Problem: {}", problemReport.getProblem());
                    logger.error("Details: {}", problemReport.getDetails());
                } catch (IOException ioe) {
                    logger.error("Error parsing trustList error response: {}", responseBody);
                    return new TrustList[0];
                }
                return new TrustList[0];
            } else {
                throw e;
            }
        }
        if (response.getBody() != null) {
            final TrustList[] trustList;
            try {
                trustList =
                        new ObjectMapper().readValue(response.getBody(), TrustList[].class);
            } catch (IOException e) {
                logger.error("Error parsing trustList response: {}", response.getBody());
                return new TrustList[0];
            }
            return trustList;
        } else {
            logger.error("Response body was null");
            return new TrustList[0];
        }
    }

    public void upload(Object cms) {
        // TODO: Implement
    }

    private HttpHeaders createDownloadHeaders() {
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, "application/json");
        return headers;
    }
}