package ua.khnu.kpp.lab7.task1;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Task1Test {

    @Test
    void searcherFindsAllOccurrences(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("a.txt");
        Files.writeString(file, "abc abc xabc");

        ResultBuffer buffer = new ResultBuffer();
        new FileSearcher(file, "abc", buffer).run(); // викликаємо run() напряму — без потоку
        buffer.close();

        List<Integer> indexes = new ArrayList<>();
        SearchResult r;
        while ((r = buffer.take()) != null) {
            assertEquals(file.toString(), r.fileName());
            indexes.add(r.index());
        }
        assertEquals(List.of(0, 4, 9), indexes);
    }

    @Test
    void takeReturnsNullAfterCloseWhenEmpty() throws Exception {
        ResultBuffer buffer = new ResultBuffer();
        buffer.close();
        assertNull(buffer.take());
    }

    @Test
    void consumerWaitsUntilProducerPuts() throws Exception {
        ResultBuffer buffer = new ResultBuffer();
        List<SearchResult> received = new ArrayList<>();

        Thread consumer = new Thread(() -> {
            try {
                SearchResult r;
                while ((r = buffer.take()) != null) {
                    received.add(r);
                }
            } catch (InterruptedException ignored) {
            }
        });
        consumer.start();

        Thread.sleep(50);              // споживач уже чекає (wait) на порожньому буфері
        buffer.put(new SearchResult("f", 1));
        buffer.put(new SearchResult("f", 2));
        buffer.close();
        consumer.join(2000);

        assertFalse(consumer.isAlive());
        assertEquals(2, received.size());
    }

    @Test
    void emptySubstringIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new FileSearcher(Path.of("x"), "", new ResultBuffer()));
    }
}
