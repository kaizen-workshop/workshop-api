package br.com.weg.workshop.notification.push;

import java.util.Map;

public interface PushProvider {
    void send(String deviceToken, String title, String message, Map<String, String> data);
}
