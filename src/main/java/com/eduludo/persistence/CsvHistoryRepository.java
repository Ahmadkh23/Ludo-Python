package com.eduludo.persistence;

import com.eduludo.Model.Difficulty;
import com.eduludo.Model.GameRecord;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Stores finished games using the course history CSV schema.
 * Names, colors, and computer-seat flags not represented by the course schema are not reconstructed
 * when reading existing rows.
 */
public final class CsvHistoryRepository implements HistoryRepository {

    static final String HEADER = "gameID,difficulty,timestamp(DD/MM/YYYY; HH:MM:SS),1st_place_name,"
            + "2nd_place_name,3rd_place_name,4rd_place_name,game_duration(HH:MM:SS)";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("dd/MM/uuuu;HH:mm:ss");

    private final Path file;

    public CsvHistoryRepository(Path file) {
        this.file = file;
    }

    /** The lecturer's history CSV, bundled unchanged inside the JAR. */
    public static final String COURSE_TEMPLATE_RESOURCE = "/com/eduludo/history-template.csv";

    /**
     * On first run, starts the history file from the lecturer's CSV (bundled in the JAR) so the game uses that
     * exact file; later games are appended to it. An existing history file is never touched.
     * Returns true if the file was created.
     */
    public static boolean seedFromCourseTemplate(Path file) {
        if (Files.exists(file)) {
            return false;
        }
        try (var in = CsvHistoryRepository.class.getResourceAsStream(COURSE_TEMPLATE_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing bundled resource " + COURSE_TEMPLATE_RESOURCE);
            }
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.copy(in, file);
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create the game history file", e);
        }
    }

    @Override
    public synchronized void save(GameRecord r) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            if (isMissingOrEmpty()) {
                // An interrupted first write can leave a 0-byte file behind; it still needs its header.
                Files.writeString(file, HEADER + System.lineSeparator(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            List<String> lines = readValidatedLines();
            long gameId = nextGameId(lines);
            String line = formatRecord(r, gameId);
            String separator = endsWithLineBreak() ? "" : System.lineSeparator();
            Files.writeString(file, separator + line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save the game history", e);
        }
    }

    @Override
    public synchronized List<GameRecord> findAll() {
        List<GameRecord> result = new ArrayList<>();
        try {
            if (isMissingOrEmpty()) {
                return result;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the game history", e);
        }
        List<String> lines;
        try {
            lines = readValidatedLines();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the game history", e);
        }
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            try {
                result.add(parseRecord(parseCsvLine(line)));
            } catch (RuntimeException ex) {
                // Skip a damaged line instead of losing the whole history.
            }
        }
        result.sort(Comparator.comparing(GameRecord::startedAt).reversed());
        return result;
    }

    /** A byte-order mark, which Excel writes in front of the header when it saves a file as "CSV UTF-8". */
    private static final String BYTE_ORDER_MARK = "\uFEFF";

    private List<String> readValidatedLines() throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (!lines.isEmpty() && lines.get(0).startsWith(BYTE_ORDER_MARK)) {
            // Same header, only marked with the encoding: decode it, do not treat the file as damaged.
            lines.set(0, lines.get(0).substring(BYTE_ORDER_MARK.length()));
        }
        if (lines.isEmpty() || !HEADER.equals(lines.get(0))) {
            throw new IllegalStateException("The history CSV header does not match the course template. "
                    + "The file was left unchanged; expected: " + HEADER);
        }
        return lines;
    }

    /** True when no history file exists yet, or it exists but is still empty (no header row). */
    private boolean isMissingOrEmpty() throws IOException {
        return !Files.exists(file) || Files.size(file) == 0;
    }

    private boolean endsWithLineBreak() throws IOException {
        byte[] contents = Files.readAllBytes(file);
        if (contents.length == 0) {
            return false;
        }
        byte last = contents[contents.length - 1];
        return last == '\n' || last == '\r';
    }

    private static long nextGameId(List<String> lines) {
        long max = 0;
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }
            List<String> fields = parseCsvLine(lines.get(i));
            if (fields.isEmpty()) {
                continue;
            }
            try {
                long id = Long.parseLong(fields.get(0));
                if (id > max) {
                    max = id;
                }
            } catch (NumberFormatException ignored) {
                // A damaged record is skipped by findAll and does not determine the next identifier.
            }
        }
        return Math.addExact(max, 1);
    }

    private static String formatRecord(GameRecord record, long gameId) {
        String[] places = {"", "", "", ""};
        for (GameRecord.RankedPlayer player : record.ranking()) {
            if (player.place() < 1 || player.place() > places.length || !places[player.place() - 1].isEmpty()) {
                throw new IllegalArgumentException("History ranking places must be unique values from 1 to 4");
            }
            validateCsvName(player.name());
            places[player.place() - 1] = player.name();
        }
        if (places[0].isEmpty() || places[1].isEmpty()) {
            throw new IllegalArgumentException("The course history record requires first- and second-place names");
        }
        return String.join(",",
                Long.toString(gameId),
                record.difficulty().name().toLowerCase(Locale.ROOT),
                TIMESTAMP.format(record.startedAt()),
                quote(places[0]),
                quote(places[1]),
                quote(places[2]),
                quote(places[3]),
                formatDuration(record.durationSeconds()));
    }

    private static void validateCsvName(String name) {
        if (name == null || name.isBlank() || name.length() > 20 || name.indexOf(',') >= 0
                || name.indexOf('"') >= 0 || name.indexOf('\n') >= 0 || name.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Player names must be 1–20 characters and cannot contain commas, "
                    + "quotation marks or line breaks");
        }
    }

    private static String formatDuration(long durationSeconds) {
        if (durationSeconds < 0) {
            throw new IllegalArgumentException("Game duration cannot be negative");
        }
        long hours = durationSeconds / 3600;
        long minutes = durationSeconds / 60 % 60;
        long seconds = durationSeconds % 60;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private static GameRecord parseRecord(List<String> fields) {
        if (fields.size() != 8) {
            throw new IllegalArgumentException("History row must contain 8 fields");
        }
        long id = Long.parseLong(fields.get(0).trim());
        if (id < 1) {
            throw new IllegalArgumentException("Game ID must be positive");
        }
        Difficulty difficulty = Difficulty.valueOf(fields.get(1).trim().toUpperCase(Locale.ROOT));
        String timestamp = fields.get(2).trim().replaceFirst(";\\s*", ";");
        LocalDateTime startedAt;
        try {
            startedAt = LocalDateTime.parse(timestamp, TIMESTAMP);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid course history timestamp: " + fields.get(2), e);
        }
        List<GameRecord.RankedPlayer> ranking = new ArrayList<>();
        for (int i = 3; i <= 6; i++) {
            String name = fields.get(i).trim();
            if (!name.isEmpty()) {
                validateCsvName(name);
                ranking.add(new GameRecord.RankedPlayer(i - 2, name, null, false));
            }
        }
        long duration = parseDuration(fields.get(7).trim());
        return new GameRecord(Long.toString(id), startedAt, difficulty, duration, ranking);
    }

    private static long parseDuration(String text) {
        String[] parts = text.split(":", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Game duration must use HH:MM:SS");
        }
        long hours = Long.parseLong(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);
        if (hours < 0 || minutes < 0 || minutes > 59 || seconds < 0 || seconds > 59) {
            throw new IllegalArgumentException("Invalid HH:MM:SS game duration");
        }
        return Math.addExact(Math.multiplyExact(hours, 3600), minutes * 60L + seconds);
    }

    private static String quote(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        fields.add(cur.toString());
        return fields;
    }
}
