package br.com.weg.workshop.notification.push;

public class PushDeliveryException extends RuntimeException {
    public PushDeliveryException(String message) {
        super(message);
    }
}
