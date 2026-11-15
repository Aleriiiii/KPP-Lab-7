package ua.khnu.kpp.lab7.task1;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Завдання 1 (початковий рівень): багатопотоковий пошук підрядка у файлах.
 *
 * Запуск: java -cp target/classes ua.khnu.kpp.lab7.task1.SubstringSearchApp <підрядок> <файл1> [файл2 ...]
 *
 * Потоки програми:
 *   main       — розбирає аргументи, запускає інші потоки, чекає на їх завершення;
 *   Searcher-N — по одному на кожен файл, шукає підрядок і записує результати в буфер;
 *   Printer    — один, виводить результати з буфера по мірі надходження.
 */
public final class SubstringSearchApp {

    private SubstringSearchApp() {
    }

    public static void main(String[] args) throws InterruptedException {
        // args[0] — підрядок, args[1..] — імена файлів
        if (args.length < 2 || args[0].isEmpty()) {
            System.out.println("Використання: SubstringSearchApp <підрядок> <файл1> [файл2 ...]");
            return;
        }
        String substring = args[0];

        // Спільний буфер для всіх потоків
        ResultBuffer buffer = new ResultBuffer();

        // 1. Запуск потоку виводу. Поки результатів немає, він чекає в buffer.take().
        ResultPrinter printer = new ResultPrinter(buffer);
        printer.start();

        // 2. Для кожного файлу створюється і запускається окремий потік пошуку.
        List<Thread> searchers = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            Path file = Path.of(args[i]);
            if (!Files.isRegularFile(file)) {
                System.err.println("Пропущено (не файл): " + file);
                continue;
            }
            // Thread(Runnable, ім'я) — потік, який виконає FileSearcher.run()
            Thread t = new Thread(new FileSearcher(file, substring, buffer), "Searcher-" + i);
            searchers.add(t);
            // start() створює новий потік; виклик run() напряму виконав би пошук у потоці main
            t.start();
        }
        System.out.println("[main] запущено потоків пошуку: " + searchers.size());

        // 3. join() — потік main чекає, доки завершиться кожен потік пошуку.
        for (Thread t : searchers) {
            t.join();
        }

        // 4. Усі потоки пошуку завершились — нових результатів не буде.
        //    close() дозволяє потоку виводу вийти з циклу після виводу залишку.
        buffer.close();
        printer.join(); // чекаємо, доки потік виводу виведе всі результати і завершиться

        // 5. Підсумок
        System.out.println("[main] пошук завершено. Знайдено входжень \"" + substring + "\": "
                + printer.printedCount());
    }
}
