package br.com.weg.workshop.file.storage;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.Resource;

public interface FileStorage {
    void store(String storageKey, InputStream content) throws IOException;
    Resource load(String storageKey) throws IOException;
    void delete(String storageKey) throws IOException;
}
