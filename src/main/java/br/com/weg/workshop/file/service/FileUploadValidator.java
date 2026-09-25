package br.com.weg.workshop.file.service;

import java.io.IOException;
import java.security.*;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileUploadValidator {

    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png", "webp", "image/webp");

    public ValidatedUpload validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("A non-empty file is required.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException("File size must not exceed 10 MB.");
        }

        String filename = filename(file.getOriginalFilename());
        String extension = extension(filename);
        String expectedContentType = CONTENT_TYPES.get(extension);
        if (expectedContentType == null || !expectedContentType.equals(file.getContentType())) {
            throw new InvalidFileException("File extension and content type must be JPEG, PNG or WebP.");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new FileStorageException();
        }
        if (content.length == 0 || content.length > MAX_FILE_SIZE || !expectedContentType.equals(detectContentType(content))) {
            throw new InvalidFileException("File content does not match its declared type.");
        }
        return new ValidatedUpload(content, filename, expectedContentType, extension, sha256(content));
    }

    private String filename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank() || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new InvalidFileException("File name is invalid.");
        }
        return originalFilename.trim();
    }

    private String extension(String filename) {
        int separator = filename.lastIndexOf('.');
        if (separator < 1 || separator == filename.length() - 1) {
            throw new InvalidFileException("File extension is required.");
        }
        return filename.substring(separator + 1).toLowerCase(Locale.ROOT);
    }

    private String detectContentType(byte[] content) {
        if (content.length >= 3 && (content[0] & 0xff) == 0xff && (content[1] & 0xff) == 0xd8 && (content[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        if (content.length >= 8 && content[0] == (byte) 0x89 && content[1] == 'P' && content[2] == 'N' && content[3] == 'G'
                && content[4] == 0x0d && content[5] == 0x0a && content[6] == 0x1a && content[7] == 0x0a) {
            return "image/png";
        }
        if (content.length >= 12 && content[0] == 'R' && content[1] == 'I' && content[2] == 'F' && content[3] == 'F'
                && content[8] == 'W' && content[9] == 'E' && content[10] == 'B' && content[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private String sha256(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
