package br.com.weg.workshop.file.service;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileUploadValidatorTest {

    private final FileUploadValidator validator = new FileUploadValidator();

    @Test
    void acceptsPngAndProducesItsIntegrityChecksum() {
        ValidatedUpload upload = validator.validate(file("banner.PNG", "image/png", png()));

        assertThat(upload.filename()).isEqualTo("banner.PNG");
        assertThat(upload.extension()).isEqualTo("png");
        assertThat(upload.contentType()).isEqualTo("image/png");
        assertThat(upload.checksumSha256()).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    void rejectsAFileWhoseContentDoesNotMatchItsDeclaredType() {
        assertThatThrownBy(() -> validator.validate(file("banner.png", "image/png", jpeg())))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsUnsupportedExtensionsAndPathLikeNames() {
        assertThatThrownBy(() -> validator.validate(file("banner.gif", "image/gif", png())))
                .isInstanceOf(InvalidFileException.class);
        assertThatThrownBy(() -> validator.validate(file("folder/banner.png", "image/png", png())))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsFilesLargerThanTenMegabytes() {
        byte[] oversized = Arrays.copyOf(png(), (int) FileUploadValidator.MAX_FILE_SIZE + 1);

        assertThatThrownBy(() -> validator.validate(file("banner.png", "image/png", oversized)))
                .isInstanceOf(InvalidFileException.class);
    }

    private MockMultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }

    private byte[] png() {
        return new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 0x00};
    }

    private byte[] jpeg() {
        return new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00};
    }
}
