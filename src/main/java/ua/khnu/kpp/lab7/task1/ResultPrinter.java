package ua.khnu.kpp.lab7.task1;

/**
 * Окремий потік для виводу результатів у консоль.
 * Забирає результати з буфера по мірі надходження, доки буфер не буде закрито.
 *
 * extends Thread — другий спосіб створити потік:
 * клас сам є потоком і перевизначає метод run().
 */
public class ResultPrinter extends Thread {

    private final ResultBuffer buffer; // спільний буфер, з якого читаємо результати
    private int printed = 0;           // кількість виведених результатів

    public ResultPrinter(ResultBuffer buffer) {
        super("Printer"); // ім'я потоку, виводиться через getName()
        this.buffer = buffer;
    }

    /** Код, який виконує потік після виклику start(). */
    @Override
    public void run() {
        try {
            SearchResult result;
            // take() повертає null лише тоді, коли буфер закрито і порожній — тоді цикл завершується
            while ((result = buffer.take()) != null) {
                printed++;
                System.out.println("[" + getName() + "] файл: " + result.fileName()
                        + ", індекс входження: " + result.index());
            }
        } catch (InterruptedException e) {
            // Потік перервано ззовні — відновлюємо прапорець переривання і завершуємось
            Thread.currentThread().interrupt();
        }
    }

    /** Кількість виведених результатів. Читати після join(), коли потік уже завершився. */
    public int printedCount() {
        return printed;
    }
}
