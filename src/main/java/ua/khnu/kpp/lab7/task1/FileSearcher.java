package ua.khnu.kpp.lab7.task1;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Пошук усіх входжень підрядка в одному файлі.
 * Кожен об'єкт цього класу виконується в окремому потоці (один потік — один файл).
 *
 * implements Runnable — один із двох способів створити потік:
 * клас описує роботу в методі run(), а запускає її об'єкт Thread.
 */
public class FileSearcher implements Runnable {

    private final Path file;           // файл, у якому шукаємо
    private final String substring;    // підрядок, який шукаємо
    private final ResultBuffer buffer; // спільний буфер, куди записуються результати

    public FileSearcher(Path file, String substring, ResultBuffer buffer) {
        // Порожній підрядок заборонено: indexOf("") знаходить його на кожній позиції
        if (substring == null || substring.isEmpty()) {
            throw new IllegalArgumentException("substring must not be empty");
        }
        this.file = file;
        this.substring = substring;
        this.buffer = buffer;
    }

    /** Код, який виконує потік після виклику start(). */
    @Override
    public void run() {
        String text;
        try {
            // Читаємо весь вміст файлу в один рядок
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // Файл не вдалося прочитати — виводимо помилку і завершуємо лише цей потік
            System.err.println("[" + Thread.currentThread().getName() + "] не вдалося прочитати "
                    + file + ": " + e.getMessage());
            return;
        }

        // indexOf повертає індекс першого входження або -1, якщо підрядка немає
        int index = text.indexOf(substring);
        while (index >= 0) {
            // Кожне входження передаємо у спільний буфер як об'єкт SearchResult
            buffer.put(new SearchResult(file.toString(), index));
            // Шукаємо наступне входження, починаючи з позиції після поточного
            index = text.indexOf(substring, index + 1);
        }
    }
}
