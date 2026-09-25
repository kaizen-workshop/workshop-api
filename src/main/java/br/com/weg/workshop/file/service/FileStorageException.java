package br.com.weg.workshop.file.service;

public class FileStorageException extends RuntimeException {
    public FileStorageException() {
        super("File storage operation failed.");
    }
}
