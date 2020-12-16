package org.sakaiproject.conversations.tool.lib;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.Sanitizers;
import org.owasp.html.PolicyFactory;

public class HTMLSanitizer {
    public static String sanitize(String html) {
        PolicyFactory customFactory = new HtmlPolicyBuilder()
            .allowElements("figcaption")
            .allowElements("figure")
            .allowAttributes("class").onElements("figure")
            .toFactory();

        PolicyFactory policy = Sanitizers.FORMATTING
            .and(Sanitizers.LINKS)
            .and(Sanitizers.TABLES)
            .and(Sanitizers.IMAGES)
            .and(Sanitizers.BLOCKS)
            .and(customFactory);

        return policy.sanitize(html);
    }
}
