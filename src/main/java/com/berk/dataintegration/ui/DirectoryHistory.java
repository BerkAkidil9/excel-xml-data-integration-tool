package com.berk.dataintegration.ui;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class DirectoryHistory {
    private final List<Path> directories = new ArrayList<>();
    private int currentIndex = -1;

    void record(Path directory) {
        if (directory == null) {
            return;
        }
        Path normalized = directory.toAbsolutePath().normalize();
        if (currentIndex >= 0 && Objects.equals(directories.get(currentIndex), normalized)) {
            return;
        }
        while (directories.size() > currentIndex + 1) {
            directories.remove(directories.size() - 1);
        }
        directories.add(normalized);
        currentIndex = directories.size() - 1;
    }

    boolean canGoBack() {
        return currentIndex > 0;
    }

    boolean canGoForward() {
        return currentIndex >= 0 && currentIndex < directories.size() - 1;
    }

    Path goBack() {
        if (!canGoBack()) {
            return null;
        }
        currentIndex--;
        return directories.get(currentIndex);
    }

    Path goForward() {
        if (!canGoForward()) {
            return null;
        }
        currentIndex++;
        return directories.get(currentIndex);
    }
}
