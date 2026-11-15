# Лабораторна робота № 7 — Багатопотокові додатки

**Рівень завдання:** початковий (обидві задачі)

## Завдання
1. **Пошук підрядка у файлах.** Список файлів передається через командний рядок. Для кожного файлу виділяється окремий потік. Результати виводить в консоль окремий потік, який зчитує об'єкти `SearchResult` («ім'я файлу», «індекс входження») по мірі їх надходження.
2. **Пошук файлів у директоріях.** Головний потік формує список директорій. Кожен потік шукає файли у своїй директорії.

## Структура проєкту
```
KPP-Lab-7/
├── .mvn/wrapper/maven-wrapper.properties
├── mvnw, mvnw.cmd
├── pom.xml
├── submission.properties
├── samples/                         # демо-дані для запуску
│   ├── texts/a.txt, b.txt, c.txt    # для задачі 1
│   └── tree/...                     # дерево папок для задачі 2
├── src/main/java/ua/khnu/kpp/lab7/
│   ├── task1/
│   │   ├── SubstringSearchApp.java  # main: запускає потоки, чекає на них
│   │   ├── FileSearcher.java        # Runnable: пошук у ОДНОМУ файлі
│   │   ├── ResultPrinter.java       # extends Thread: вивід результатів
│   │   ├── ResultBuffer.java        # спільний список (synchronized + wait/notifyAll)
│   │   └── SearchResult.java        # record: fileName, index
│   └── task2/
│       ├── FileFinderApp.java       # main: формує список директорій, запускає потоки
│       └── DirectoryScanner.java    # Runnable: пошук в ОДНІЙ директорії
├── src/test/java/ua/khnu/kpp/lab7/
│   ├── task1/Task1Test.java
│   └── task2/Task2Test.java
├── .gitignore
└── README.md
```

## Архітектура

### Задача 1: схема «виробники → буфер → споживач»
```
                     ┌──────────────┐
 Searcher-1 (a.txt) ─┤              │
 Searcher-2 (b.txt) ─┤ ResultBuffer ├──► Printer ──► консоль
 Searcher-3 (c.txt) ─┤  (LinkedList)│
                     └──────────────┘
        put()          synchronized        take()
```
* **`FileSearcher`** (виробник) читає свій файл і знаходить усі входження підрядка через `indexOf`. Кожне входження він кладе в буфер як `SearchResult`.
* **`ResultBuffer`** — спільний список. Методи `put`, `take` і `close` позначені `synchronized`, тож одночасно зі списком працює лише один потік.
  * Якщо список порожній, `take()` викликає `wait()`: потік виводу засинає і звільняє блокування.
  * `put()` після додавання викликає `notifyAll()` і будить потік виводу.
  * `close()` повідомляє, що нових результатів не буде. Тоді `take()` повертає `null`, і потік виводу завершується.
* **`ResultPrinter`** (споживач) у циклі викликає `take()` і друкує кожен результат одразу, як тільки той з'явився.

Порядок роботи `main`:
1. Створити буфер і запустити `Printer`. Він одразу засинає, бо результатів ще немає.
2. Для кожного файлу створити `new Thread(new FileSearcher(...))` і викликати `start()`.
3. Викликати `join()` для кожного потоку пошуку, тобто дочекатися завершення всіх.
4. Викликати `buffer.close()`, а потім `printer.join()`: дочекатися, поки `Printer` виведе все, що залишилось.
5. Надрукувати загальну кількість знайдених входжень.

### Задача 2: «головний потік роздає директорії»
```
 main: Files.walk(root) → [root, root/docs, root/docs/notes, root/src, ...]
          │
          ├─► Scanner-1  (root)        ─┐
          ├─► Scanner-2  (root/docs)    ├─► synchronizedList<Path> found
          └─► Scanner-N  (...)         ─┘
 main: join() усіх → сортування → вивід
```
* **`FileFinderApp`** (головний потік) обходить дерево папок і складає список усіх директорій. Для кожної директорії він створює окремий потік.
* **`DirectoryScanner`** переглядає лише свою директорію, без підпапок, бо підпапки дістались іншим потокам. Файли, які відповідають масці, він додає до спільного списку.
* Спільний список створено через `Collections.synchronizedList(...)`, тож одночасний `add()` з кількох потоків безпечний.
* Після `join()` усіх потоків `main` сортує результат і друкує його.

### Які поняття з методички використано
| Поняття | Де |
|---|---|
| Наслідування `Thread` | `ResultPrinter` |
| Реалізація `Runnable` | `FileSearcher`, `DirectoryScanner` |
| `start()` (не `run()`!) | `SubstringSearchApp`, `FileFinderApp` |
| `join()` — чекати завершення потоку | обидва `main` |
| `synchronized`-методи | `ResultBuffer` |
| `wait()` / `notifyAll()` | `ResultBuffer.take()` / `put()` / `close()` |
| Імена потоків (`setName` / конструктор) | `Searcher-N`, `Printer`, `Scanner-N` |

## Запуск
Windows (PowerShell), команди виконуються з кореня проєкту:
```powershell
chcp 65001                  # щоб коректно відображалась кирилиця
.\mvnw.cmd clean verify     # компіляція + тести

# Задача 1: <підрядок> <файли...>
java -cp target/classes ua.khnu.kpp.lab7.task1.SubstringSearchApp Java samples/texts/a.txt samples/texts/b.txt samples/texts/c.txt

# Задача 2: <папка> [маска]   (маску беріть у лапки!)
java -cp target/classes ua.khnu.kpp.lab7.task2.FileFinderApp samples/tree "*.txt"
```
Linux / macOS: те саме, але `./mvnw clean verify`.

**У NetBeans / IntelliJ:** запускайте потрібний клас (`SubstringSearchApp` або `FileFinderApp`), а аргументи вкажіть у Run → Edit Configurations → Program arguments (наприклад, `Java samples/texts/a.txt samples/texts/b.txt`).

### Приклад виводу
Задача 1:
```
[main] запущено потоків пошуку: 3
[Printer] файл: samples/texts/b.txt, індекс входження: 60
[Printer] файл: samples/texts/a.txt, індекс входження: 0
[Printer] файл: samples/texts/a.txt, індекс входження: 36
[Printer] файл: samples/texts/a.txt, індекс входження: 77
[main] пошук завершено. Знайдено входжень "Java": 4
```
Задача 2:
```
[main] директорій для пошуку: 6
[Scanner-2] samples/tree/src → знайдено файлів: 0
[Scanner-1] samples/tree → знайдено файлів: 1
[Scanner-6] samples/tree/docs/notes → знайдено файлів: 2
...
Знайдено файлів за маскою "*.txt": 4
```
Порядок рядків від потоків щоразу інший, бо потоки працюють паралельно, і операційна система сама вирішує, який із них виконується першим.
