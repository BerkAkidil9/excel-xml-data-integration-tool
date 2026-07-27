package com.berk.dataintegration.ui;

import com.berk.dataintegration.service.ConversionRecordCounts;
import com.berk.dataintegration.service.ConversionResult;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwingWorkerConversionExecutorTest {
    @Test
    void runsConversionOffTheEdtAndCompletesOnTheEdt() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicBoolean conversionRanOnEdt = new AtomicBoolean(true);
        AtomicBoolean callbackRanOnEdt = new AtomicBoolean(false);
        AtomicReference<Exception> callbackError = new AtomicReference<>();
        ConversionResult result = new ConversionResult(
                Path.of("input.xlsx"),
                Path.of("output.xml"),
                ConversionRecordCounts.empty(),
                List.of()
        );
        SwingWorkerConversionExecutor executor = new SwingWorkerConversionExecutor(request -> {
            conversionRanOnEdt.set(SwingUtilities.isEventDispatchThread());
            return result;
        });

        SwingUtilities.invokeAndWait(() -> executor.execute(
                new ConversionRequest(ConversionDirection.EXCEL_TO_XML, Path.of("input.xlsx"), Path.of("output.xml")),
                completed -> {
                    callbackRanOnEdt.set(SwingUtilities.isEventDispatchThread());
                    done.countDown();
                },
                exception -> {
                    callbackError.set(exception);
                    done.countDown();
                }
        ));

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertNull(callbackError.get());
        assertFalse(conversionRanOnEdt.get());
        assertTrue(callbackRanOnEdt.get());
    }
}
