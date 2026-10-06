package br.com.weg.workshop.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

class GlobalExceptionHandlerTest {

    @Test
    void unexpectedFailuresAreLoggedWithoutRequestCredentials() {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            var request = new MockHttpServletRequest("POST", "/api/v1/workshops/secret-resource-id");
            request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v1/workshops/{id}");
            request.addHeader("Authorization", "Bearer secret-token");
            request.setContent("password=secret-password".getBytes());
            var handler = new GlobalExceptionHandler(new ApiErrorFactory());

            var response = handler.handleUnexpected(request, new IllegalStateException("database unavailable"));

            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred.");
            assertThat(appender.list).hasSize(1);
            var event = appender.list.getFirst();
            assertThat(event.getFormattedMessage())
                    .contains("/api/v1/workshops/{id}")
                    .doesNotContain("secret-resource-id", "secret-token", "secret-password");
            assertThat(event.getThrowableProxy().getClassName())
                    .isEqualTo(IllegalStateException.class.getName());
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
