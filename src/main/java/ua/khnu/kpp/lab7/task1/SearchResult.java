package ua.khnu.kpp.lab7.task1;

/**
 * Результат пошуку: одне входження підрядка у файлі.
 *
 * record — скорочений запис незмінного класу: компілятор сам створює
 * конструктор, методи fileName(), index(), equals(), hashCode(), toString().
 *
 * @param fileName ім'я файлу, в якому знайдено підрядок
 * @param index    індекс входження — номер символу, з якого починається підрядок (рахуючи з 0)
 */
public record SearchResult(String fileName, int index) {
}
