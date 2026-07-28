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

    static boolean hasExtension(Path path, String extension) {
        if (path == null) {
            return false;
        }
        String expectedSuffix = "." + extension;
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
        return fileName.toLowerCase(Locale.ROOT).endsWith(expectedSuffix.toLowerCase(Locale.ROOT));
    }

    static Path replaceExtension(Path path, String extension) {
        if (path == null) {
            return null;
        }
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        return path.resolveSibling(baseName + "." + extension);
    }
}
