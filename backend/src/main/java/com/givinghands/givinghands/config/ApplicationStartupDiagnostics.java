package com.givinghands.givinghands.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Logs schema and mail readiness at startup. Never blocks application startup.
 */
@Component
@Order(0)
public class ApplicationStartupDiagnostics implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ApplicationStartupDiagnostics.class);

    private final SchemaHealthChecker schemaHealthChecker;
    private final MailProperties mailProperties;

    public ApplicationStartupDiagnostics(SchemaHealthChecker schemaHealthChecker,
                                         MailProperties mailProperties) {
        this.schemaHealthChecker = schemaHealthChecker;
        this.mailProperties = mailProperties;
    }

    @Override
    public void run(String... args) {
        log.info("========== GivingHands startup diagnostics ==========");

        SchemaHealthChecker.HealthReport schema = schemaHealthChecker.check();
        schemaHealthChecker.logReport(schema);

        if (mailProperties.isConfigured()) {
            log.info("[mail] CONFIGURED — {}", mailProperties.configurationHint());
        } else {
            log.error("[mail] NOT CONFIGURED — {}", mailProperties.configurationHint());
            log.error("[mail] Automatic Gmail notifications will be skipped until SMTP credentials are set");
        }

        log.info("=====================================================");
    }
}
