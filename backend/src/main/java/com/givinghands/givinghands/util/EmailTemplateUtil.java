package com.givinghands.givinghands.util;

import com.givinghands.givinghands.notification.EmailTemplateName;
import com.givinghands.givinghands.notification.TemplateContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Year;
import java.util.Collections;
import java.util.Map;

/**
 * Reusable utility for rendering HTML email bodies from Thymeleaf templates.
 * Templates live under {@code src/main/resources/templates/emails/}.
 */
@Component
public class EmailTemplateUtil {

    private static final Logger log = LoggerFactory.getLogger(EmailTemplateUtil.class);
    private static final String TEMPLATE_PREFIX = "emails/";

    private final TemplateEngine templateEngine;

    public EmailTemplateUtil(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String render(EmailTemplateName templateName, Map<String, Object> variables) {
        String templatePath = TEMPLATE_PREFIX + toSnakeCaseLower(templateName.name());
        Context context = new Context();
        if (variables != null) {
            context.setVariables(variables);
        }
        try {
            return templateEngine.process(templatePath, context);
        } catch (Exception e) {
            log.error("Failed to render email template. template={}, keys={}",
                    templatePath, variables != null ? variables.keySet() : "null", e);
            throw e;
        }
    }

    public String render(EmailTemplateName templateName, TemplateContext context) {
        Map<String, Object> variables = context != null ? context.asMap() : Collections.emptyMap();
        return render(templateName, variables);
    }

    public static TemplateContext baseContext() {
        return new TemplateContext().put("year", Year.now().getValue());
    }

    static String toSnakeCaseLower(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value
                .trim()
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .replace('-', '_')
                .toLowerCase();
    }
}
