package br.com.weg.workshop.notification.push;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.notifications.push.provider", havingValue = "none", matchIfMissing = true)
class NoOpPushProvider implements PushProvider {
    @Override
    public void send(String deviceToken, String title, String message, Map<String, String> data) {
        // The persistent notification centre remains authoritative until a provider is configured.
    }
}
