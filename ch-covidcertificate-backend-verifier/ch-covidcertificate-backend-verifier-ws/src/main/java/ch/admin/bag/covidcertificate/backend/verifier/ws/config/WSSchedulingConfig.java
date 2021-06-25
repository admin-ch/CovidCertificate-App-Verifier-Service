package ch.admin.bag.covidcertificate.backend.verifier.ws.config;

import ch.admin.bag.covidcertificate.backend.verifier.data.AppTokenDataService;
import ch.admin.bag.covidcertificate.backend.verifier.model.AppToken;
import ch.admin.bag.covidcertificate.backend.verifier.ws.config.model.ApiKeyConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class WSSchedulingConfig {

    private final ApiKeyConfig apiKeyConfig;
    private final AppTokenDataService appTokenDataService;

    public WSSchedulingConfig(
        ApiKeyConfig apiKeyConfig,
        AppTokenDataService appTokenDataService) {
        this.apiKeyConfig = apiKeyConfig;
        this.appTokenDataService = appTokenDataService;
    }

    @Scheduled(cron = "${ws.authentication.cron:0 0/5 0 ? * *}")
    public void updateAppTokens() {
        final var appTokens = appTokenDataService.getAppTokens();
        final Map<String, String> apiKeys = new HashMap<>();
        for (var token: appTokens) {
            apiKeys.put(token.getDescription(), token.getApiKey());
        }
        apiKeyConfig.setApiKeys(apiKeys);
    }
}
