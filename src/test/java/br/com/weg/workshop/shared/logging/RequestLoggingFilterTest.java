package br.com.weg.workshop.shared.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.assertThat;

class RequestLoggingFilterTest {
    @Test void requestLogsDoNotIncludeCredentialsOrUnmappedUserPaths() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            var request = new MockHttpServletRequest("POST", "/secret-path");
            request.addHeader("Authorization", "Bearer secret-token");
            request.setQueryString("password=secret-password");
            request.setContent("secret-body".getBytes());
            var response = new MockHttpServletResponse();
            new RequestLoggingFilter().doFilter(request, response, (req, res) -> res.setContentType("application/json"));
            assertThat(response.getHeader("X-Request-Id")).isNotBlank();
            var event = appender.list.getFirst();
            String logged = event.getFormattedMessage() + event.getKeyValuePairs().toString();
            assertThat(logged).contains("unmapped", "requestId", "durationMs")
                    .doesNotContain("secret-token", "secret-password", "secret-body", "secret-path");
            assertThat(MDC.get("requestId")).isNull();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
