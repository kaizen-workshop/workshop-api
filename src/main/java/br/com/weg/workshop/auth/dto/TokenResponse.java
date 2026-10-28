package br.com.weg.workshop.auth.dto;
public record TokenResponse(String accessToken, String refreshToken, String tokenType, boolean mustChangePassword) {}
