package br.com.weg.workshop.file.storage;

import java.io.*;
import java.nio.file.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

@Component
public class LocalFileStorage implements FileStorage {

    private final Path root;

    public LocalFileStorage(@Value("${app.file-storage.local-directory:./storage}") String directory) {
        this.root = Paths.get(directory).toAbsolutePath().normalize();
    }

    @Override
    public void store(String storageKey, InputStream content) throws IOException {
        Path target = resolve(storageKey);
        Files.createDirectories(target.getParent());
        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public FileSystemResource load(String storageKey) throws IOException {
        Path target = resolve(storageKey);
        if (!Files.isRegularFile(target)) {
            throw new FileNotFoundException("Stored file was not found.");
        }
        return new FileSystemResource(target);
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolve(storageKey));
    }

    private Path resolve(String storageKey) {
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key.");
        }
        return target;
    }
}
