package br.com.weg.workshop.file.service;

record ValidatedUpload(byte[] content, String filename, String contentType, String extension, String checksumSha256) {
}
