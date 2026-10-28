package br.com.weg.workshop.notification.push;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "app.notifications.push.provider", havingValue = "expo")
class ExpoPushProvider implements PushProvider {
    private final RestClient client;
    private final String accessToken;

    ExpoPushProvider(
            RestClient.Builder builder,
            @Value("${app.notifications.push.expo.endpoint:https://exp.host/--/api/v2/push/send}") String endpoint,
            @Value("${app.notifications.push.expo.access-token:}") String accessToken
    ) {
        this.client = builder.baseUrl(endpoint).build();
        this.accessToken = accessToken;
    }

    @Override
    public void send(String deviceToken, String title, String message, Map<String, String> data) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("to", deviceToken);
        payload.put("title", title);
        payload.put("body", message);
        payload.put("data", data == null ? Map.of() : data);

        RestClient.RequestBodySpec request = client.post();
        if (!accessToken.isBlank()) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        ExpoResponse response = request.body(payload)
                .retrieve()
                .body(ExpoResponse.class);
        if (response == null || response.data() == null || !"ok".equals(response.data().status())) {
            throw new PushDeliveryException("The push provider rejected the notification.");
        }
    }

    private record ExpoResponse(ExpoTicket data) {
    }

    private record ExpoTicket(String status, String id, String message) {
    }
}
