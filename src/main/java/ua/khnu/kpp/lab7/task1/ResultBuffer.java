package ua.khnu.kpp.lab7.task1;

import java.util.LinkedList;

/**
 * Спільний список результатів, з яким одночасно працюють кілька потоків:
 *  - потоки пошуку (FileSearcher) додають результати методом put();
 *  - потік виводу (ResultPrinter) забирає їх методом take() по мірі надходження.
 *
 * Синхронізація: synchronized + wait() / notifyAll().
 */
public class ResultBuffer {

    private final LinkedList<SearchResult> results = new LinkedList<>(); // черга результатів
    private boolean closed = false; // true — нових результатів більше не буде

    /**
     * Додає результат у кінець черги.
     * synchronized — у кожен момент часу з об'єктом працює лише один потік,
     * тому одночасні виклики з різних потоків не пошкодять список.
     */
    public synchronized void put(SearchResult result) {
        if (closed) {
            throw new IllegalStateException("Buffer is closed");
        }
        results.addLast(result);
        notifyAll(); // будить потоки, які чекають у take()
    }

    /**
     * Повертає найстаріший результат.
     * Якщо черга порожня — потік чекає (wait), доки не буде викликано put() або close().
     *
     * @return наступний результат або null, якщо буфер закрито і черга порожня
     */
    public synchronized SearchResult take() throws InterruptedException {
        // while, а не if: після пробудження умову потрібно перевірити повторно
        while (results.isEmpty() && !closed) {
            wait(); // звільняє монітор об'єкта і призупиняє потік до notifyAll()
        }
        if (results.isEmpty()) {
            return null; // буфер закрито і всі результати вже забрано — сигнал завершення
        }
        return results.removeFirst();
    }

    /**
     * Позначає, що нових результатів не буде (усі потоки пошуку завершились).
     * notifyAll() потрібен, щоб потік, який чекає в take(), прокинувся і завершився.
     */
    public synchronized void close() {
        closed = true;
        notifyAll();
    }
}
