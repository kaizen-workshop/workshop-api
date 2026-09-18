package br.com.weg.workshop.auth.dto;
public record TokenResponse(String accessToken, String tokenType, boolean mustChangePassword) {}
