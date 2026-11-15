package ua.khnu.kpp.lab7.task2;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Завдання 2 (початковий рівень): багатопотоковий пошук файлів.
 *
 * Запуск: java -cp target/classes ua.khnu.kpp.lab7.task2.FileFinderApp <коренева_папка> [маска]
 * Маска — glob-шаблон імені файлу, напр. "*.txt". За замовчуванням "*" (усі файли).
 *
 * Потоки програми:
 *   main      — формує список директорій, запускає потоки, чекає на їх завершення, виводить результат;
 *   Scanner-N — по одному на кожну директорію, шукає в ній файли за маскою.
 */
public final class FileFinderApp {

    private FileFinderApp() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // args[0] — коренева папка, args[1] — маска (необов'язкова)
        if (args.length < 1) {
            System.out.println("Використання: FileFinderApp <коренева_папка> [маска, напр. *.txt]");
            return;
        }
        Path root = Path.of(args[0]);
        String mask = args.length > 1 ? args[1] : "*";
        if (!Files.isDirectory(root)) {
            System.out.println("Не є директорією: " + root.toAbsolutePath());
            return;
        }

        List<Path> found = findFiles(root, mask);

        System.out.println();
        System.out.println("Знайдено файлів за маскою \"" + mask + "\": " + found.size());
        found.forEach(p -> System.out.println("  " + p));
    }

    /** Основна логіка пошуку (винесена в окремий метод для тестування). */
    public static List<Path> findFiles(Path root, String mask) throws IOException, InterruptedException {
        // Перетворення маски (напр. *.txt) на об'єкт для перевірки імен файлів
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + mask);

        // 1. Потік main формує список директорій.
        //    Files.walk обходить усе дерево; filter залишає лише директорії (разом із коренем).
        List<Path> directories;
        try (Stream<Path> walk = Files.walk(root)) {
            directories = walk.filter(Files::isDirectory).toList();
        }
        System.out.println("[main] директорій для пошуку: " + directories.size());

        // 2. Спільний список результатів.
        //    synchronizedList синхронізує кожен метод, тому одночасний add() з кількох потоків безпечний.
        List<Path> found = Collections.synchronizedList(new ArrayList<>());

        // 3. Для кожної директорії створюється і запускається окремий потік.
        List<Thread> threads = new ArrayList<>();
        int n = 1;
        for (Path dir : directories) {
            Thread t = new Thread(new DirectoryScanner(dir, matcher, found), "Scanner-" + n++);
            threads.add(t);
            t.start();
        }

        // 4. join() — потік main чекає завершення всіх потоків пошуку.
        for (Thread t : threads) {
            t.join();
        }

        // 5. Після join() у список ніхто не пише — його можна безпечно скопіювати і відсортувати.
        //    Сортування потрібне, бо потоки додають файли в довільному порядку.
        List<Path> result = new ArrayList<>(found);
        result.sort(Comparator.naturalOrder());
        return result;
    }
}
