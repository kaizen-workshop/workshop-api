package br.com.weg.workshop.notification.push;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class NoOpPushProvider implements PushProvider {
    @Override
    public void send(String deviceToken, String title, String message, Map<String, String> data) {
        // The persistent notification centre remains authoritative until a provider is configured.
    }
}
