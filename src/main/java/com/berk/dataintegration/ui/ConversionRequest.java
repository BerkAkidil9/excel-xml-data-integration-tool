package com.berk.dataintegration.ui;

import java.nio.file.Path;

public record ConversionRequest(
        ConversionDirection direction,
        Path inputPath,
        Path outputPath
) {
}
