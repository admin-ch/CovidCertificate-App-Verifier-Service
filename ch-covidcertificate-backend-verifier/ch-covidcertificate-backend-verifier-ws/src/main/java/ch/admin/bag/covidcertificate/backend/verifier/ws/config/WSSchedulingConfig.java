package ch.admin.bag.covidcertificate.backend.verifier.ws.config;

import ch.admin.bag.covidcertificate.backend.verifier.data.AppTokenDataService;
import ch.admin.bag.covidcertificate.backend.verifier.ws.config.model.ApiKeyConfig;
import ch.admin.bag.covidcertificate.backend.verifier.ws.controller.RevocationListController;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class WSSchedulingConfig {

    private final ApiKeyConfig apiKeyConfig;
    private final AppTokenDataService appTokenDataService;

    private static final Logger logger = LoggerFactory.getLogger(WSSchedulingConfig.class);

    public WSSchedulingConfig(ApiKeyConfig apiKeyConfig, AppTokenDataService appTokenDataService) {
        this.apiKeyConfig = apiKeyConfig;
        this.appTokenDataService = appTokenDataService;
    }

    // Call method every 5 minutes starting at 0am, of every day
    @Scheduled(cron = "${ws.authentication.cron:0 0/5 0 ? * *}")
    @Scheduled(fixedRate = 30000, initialDelay = 10000)
    public void updateAppTokens() {
        logger.info("Updating app tokens");
        final var appTokens = appTokenDataService.getAppTokens();
        final Map<String, String> apiKeys = new HashMap<>();
        for (var token : appTokens) {
            apiKeys.put(token.getDescription(), token.getApiKey());
        }
        apiKeyConfig.setApiKeys(apiKeys);
    }
}
