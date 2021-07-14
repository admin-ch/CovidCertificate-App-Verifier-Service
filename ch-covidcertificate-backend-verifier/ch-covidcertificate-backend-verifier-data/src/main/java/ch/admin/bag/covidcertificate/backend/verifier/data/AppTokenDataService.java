package ch.admin.bag.covidcertificate.backend.verifier.data;

import ch.admin.bag.covidcertificate.backend.verifier.model.AppToken;
import java.util.List;

/**
 * Dataservice allowing the fetching of stored app tokens. Insertion and removal are done by hand.
 */
public interface AppTokenDataService {

    /** Fetches all app tokens contained in the database */
    public List<AppToken> getAppTokens();
}
