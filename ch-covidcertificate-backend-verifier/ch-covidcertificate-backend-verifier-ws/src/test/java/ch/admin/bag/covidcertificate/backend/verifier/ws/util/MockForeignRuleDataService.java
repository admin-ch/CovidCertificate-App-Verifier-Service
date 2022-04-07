package ch.admin.bag.covidcertificate.backend.verifier.ws.util;

import static ch.admin.bag.covidcertificate.backend.verifier.ws.util.MockRulesContent.AT1_CONTENT;
import static ch.admin.bag.covidcertificate.backend.verifier.ws.util.MockRulesContent.AT2_CONTENT;

import ch.admin.bag.covidcertificate.backend.verifier.data.ForeignRulesDataService;
import ch.admin.bag.covidcertificate.backend.verifier.model.ForeignRule;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MockForeignRuleDataService implements ForeignRulesDataService {
    private final ForeignRule at1 = new ForeignRule();
    private final ForeignRule at2 = new ForeignRule();

    public MockForeignRuleDataService() {
        at1.setId("GR-AT-0039");
        at1.setVersion("0.0.1");
        at1.setCountry("AT");
        at1.setValidUntil(LocalDateTime.now().plus(1, ChronoUnit.YEARS));
        at1.setValidFrom(LocalDateTime.now().minus(1, ChronoUnit.DAYS));
        at1.setContent(AT1_CONTENT);

        at2.setId("GR-AT-0039");
        at2.setVersion("0.0.2");
        at2.setCountry("AT");
        at2.setValidUntil(LocalDateTime.now().plus(1, ChronoUnit.YEARS));
        at2.setValidFrom(LocalDateTime.now().plus(1, ChronoUnit.DAYS));
        at2.setContent(AT2_CONTENT);
    }

    @Override
    public List<String> getCountries() {
        return Arrays.asList("AT", "DE");
    }

    @Override
    public List<ForeignRule> getRulesForCountry(String country) {
        if (country.equals("AT")) {
            return Arrays.asList(at1, at2);
        } else if (country.equals("DE")) {
            return new ArrayList<>();
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public void insertRule(ForeignRule rule) {}

    @Override
    public void removeRuleSet(String country) {}
}
