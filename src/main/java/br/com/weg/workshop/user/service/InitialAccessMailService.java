package br.com.weg.workshop.user.service;

public interface InitialAccessMailService {
    void send(String recipient, String username, String temporaryPassword);
}
