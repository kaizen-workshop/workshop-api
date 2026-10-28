package br.com.weg.workshop.administration.dto;

public record ParticipantExport(byte[] content, String contentType, String filename) {
}
