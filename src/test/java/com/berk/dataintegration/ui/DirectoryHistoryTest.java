package com.berk.dataintegration.ui;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectoryHistoryTest {
    @Test
    void startsWithoutNavigationTargets() {
        DirectoryHistory history = new DirectoryHistory();

        assertFalse(history.canGoBack());
        assertFalse(history.canGoForward());
        assertNull(history.goBack());
        assertNull(history.goForward());
    }

    @Test
    void navigatesBackAndForwardBetweenRecordedDirectories() {
        DirectoryHistory history = new DirectoryHistory();
        Path first = Path.of("first");
        Path second = Path.of("second");

        history.record(first);
        history.record(second);

        assertTrue(history.canGoBack());
        assertFalse(history.canGoForward());
        assertEquals(normalized(first), history.goBack());
        assertFalse(history.canGoBack());
        assertTrue(history.canGoForward());
        assertEquals(normalized(second), history.goForward());
    }

    @Test
    void recordingAfterBackClearsForwardHistory() {
        DirectoryHistory history = new DirectoryHistory();
        Path first = Path.of("first");
        Path second = Path.of("second");
        Path third = Path.of("third");

        history.record(first);
        history.record(second);
        history.goBack();
        history.record(third);

        assertFalse(history.canGoForward());
        assertEquals(normalized(first), history.goBack());
        assertEquals(normalized(third), history.goForward());
    }

    @Test
    void ignoresRepeatedCurrentDirectory() {
        DirectoryHistory history = new DirectoryHistory();
        Path directory = Path.of("same");

        history.record(directory);
        history.record(directory);

        assertFalse(history.canGoBack());
        assertFalse(history.canGoForward());
    }

    private Path normalized(Path path) {
        return path.toAbsolutePath().normalize();
    }
}
