package com.berk.dataintegration.ui;

import java.nio.file.Path;
import java.util.Locale;

final class OutputPathExtensions {
    private OutputPathExtensions() {
    }

    static Path withExpectedExtension(Path path, String extension) {
        if (path == null) {
            return null;
        }
        String expectedSuffix = "." + extension;
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
        if (fileName.toLowerCase(Locale.ROOT).endsWith(expectedSuffix.toLowerCase(Locale.ROOT))) {
            return path;
        }
        return path.resolveSibling(fileName + expectedSuffix);
    }
}
