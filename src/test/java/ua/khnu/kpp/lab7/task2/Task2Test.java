package ua.khnu.kpp.lab7.task2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Task2Test {

    @Test
    void findsMatchingFilesInAllSubdirectories(@TempDir Path root) throws Exception {
        Files.createDirectories(root.resolve("a/b"));
        Files.writeString(root.resolve("1.txt"), "");
        Files.writeString(root.resolve("a/2.txt"), "");
        Files.writeString(root.resolve("a/b/3.txt"), "");
        Files.writeString(root.resolve("a/b/skip.md"), "");

        List<Path> found = FileFinderApp.findFiles(root, "*.txt");

        assertEquals(List.of(
                root.resolve("1.txt"),
                root.resolve("a/2.txt"),
                root.resolve("a/b/3.txt")), found);
    }

    @Test
    void defaultMaskFindsEverything(@TempDir Path root) throws Exception {
        Files.writeString(root.resolve("x.txt"), "");
        Files.writeString(root.resolve("y.md"), "");
        assertEquals(2, FileFinderApp.findFiles(root, "*").size());
    }
}
