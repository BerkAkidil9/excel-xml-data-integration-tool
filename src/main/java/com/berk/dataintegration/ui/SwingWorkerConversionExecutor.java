package com.berk.dataintegration.ui;

import com.berk.dataintegration.service.ConversionResult;

import javax.swing.SwingWorker;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public final class SwingWorkerConversionExecutor implements ConversionExecutor {
    private final ConversionUseCase useCase;

    public SwingWorkerConversionExecutor() {
        this(new DefaultConversionUseCase());
    }

    SwingWorkerConversionExecutor(ConversionUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public void execute(
            ConversionRequest request,
            Consumer<ConversionResult> onComplete,
            Consumer<Exception> onError
    ) {
        new SwingWorker<ConversionResult, Void>() {
            @Override
            protected ConversionResult doInBackground() {
                return useCase.convert(request);
            }

            @Override
            protected void done() {
                try {
                    onComplete.accept(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    onError.accept(exception);
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    if (cause instanceof Exception converted) {
                        onError.accept(converted);
                    } else {
                        onError.accept(new RuntimeException(cause));
                    }
                }
            }
        }.execute();
    }
}
