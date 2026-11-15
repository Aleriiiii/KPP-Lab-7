package ua.khnu.kpp.lab7.task2;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;

/**
 * Пошук файлів за маскою в одній директорії.
 * Кожен об'єкт виконується в окремому потоці (один потік — одна директорія).
 * Підкаталоги не переглядаються: вони є в загальному списку і обробляються іншими потоками.
 */
public class DirectoryScanner implements Runnable {

    private final Path directory;        // директорія, яку переглядає цей потік
    private final PathMatcher matcher;   // перевірка імені файлу за маскою (напр. *.txt)
    private final List<Path> foundFiles; // спільний потокобезпечний список результатів

    public DirectoryScanner(Path directory, PathMatcher matcher, List<Path> foundFiles) {
        this.directory = directory;
        this.matcher = matcher;
        this.foundFiles = foundFiles;
    }

    /** Код, який виконує потік після виклику start(). */
    @Override
    public void run() {
        String threadName = Thread.currentThread().getName(); // ім'я поточного потоку, напр. Scanner-3
        int count = 0;
        // newDirectoryStream повертає вміст лише цієї директорії (без вкладених)
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                // Враховуємо лише звичайні файли, ім'я яких відповідає масці
                if (Files.isRegularFile(entry) && matcher.matches(entry.getFileName())) {
                    foundFiles.add(entry); // список синхронізований — одночасний add() безпечний
                    count++;
                }
            }
        } catch (IOException e) {
            System.err.println("[" + threadName + "] не вдалося прочитати " + directory + ": " + e.getMessage());
            return;
        }
        System.out.println("[" + threadName + "] " + directory + " → знайдено файлів: " + count);
    }
}
