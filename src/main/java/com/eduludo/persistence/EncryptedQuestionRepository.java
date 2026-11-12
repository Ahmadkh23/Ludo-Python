package com.eduludo.persistence;

import com.eduludo.Model.Question;
import com.eduludo.security.CryptoBox;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Supplier;

/**
 * Stores the question bank encrypted with AES-GCM ({@link CryptoBox}), so the answers can't be read
 * or edited in a text editor. On first start the file is created from the built-in default questions.
 * Saves are atomic (write a temp file, then move it), so a crash can't leave a half-written file.
 */
public final class EncryptedQuestionRepository implements QuestionRepository {

    /** Resource with the questions shipped with the game. */
    public static final String DEFAULT_RESOURCE = "/com/eduludo/questions-default.json";

    private final Path file;
    private final CryptoBox crypto;
    private final Supplier<String> defaultJson;

    public EncryptedQuestionRepository(Path file, CryptoBox crypto) {
        this(file, crypto, EncryptedQuestionRepository::readDefaultResource);
    }

    public EncryptedQuestionRepository(Path file, CryptoBox crypto, Supplier<String> defaultJson) {
        this.file = file;
        this.crypto = crypto;
        this.defaultJson = defaultJson;
    }

    @Override
    public synchronized List<Question> loadAll() {
        if (!Files.exists(file)) {
            List<Question> defaults = QuestionJson.fromJson(defaultJson.get());
            saveAll(defaults);
            return defaults;
        }
        try {
            String encrypted = Files.readString(file, StandardCharsets.UTF_8);
            return QuestionJson.fromJson(crypto.decrypt(encrypted));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the question bank", e);
        }
    }

    @Override
    public synchronized void saveAll(List<Question> questions) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, crypto.encrypt(QuestionJson.toJson(questions)), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save the question bank", e);
        }
    }

    static String readDefaultResource() {
        try (InputStream in = EncryptedQuestionRepository.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + DEFAULT_RESOURCE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

