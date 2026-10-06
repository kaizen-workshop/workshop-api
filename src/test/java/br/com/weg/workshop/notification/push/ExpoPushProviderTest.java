package br.com.weg.workshop.notification.push;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ExpoPushProviderTest {

    @Test
    void sendsAnAuthenticatedExpoNotification() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ExpoPushProvider provider = new ExpoPushProvider(builder, "https://example.test/push", "access-token");
        server.expect(once(), method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andExpect(jsonPath("$.to").value("ExponentPushToken[device]"))
                .andExpect(jsonPath("$.data.workshopId").value("workshop-id"))
                .andRespond(withSuccess("{\"data\":{\"status\":\"ok\",\"id\":\"ticket-id\"}}",
                        MediaType.APPLICATION_JSON));

        provider.send("ExponentPushToken[device]", "Workshop", "Starting soon",
                Map.of("workshopId", "workshop-id"));

        server.verify();
    }

    @Test
    void rejectsAnErrorTicketWithoutExposingProviderDetails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ExpoPushProvider provider = new ExpoPushProvider(builder, "https://example.test/push", "");
        server.expect(once(), method(HttpMethod.POST))
                .andRespond(withSuccess("{\"data\":{\"status\":\"error\",\"message\":\"DeviceNotRegistered\"}}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.send("invalid", "Title", "Message", Map.of()))
                .isInstanceOf(PushDeliveryException.class)
                .hasMessage("The push provider rejected the notification.");
    }
}
