package com.berk.dataintegration.app;

import com.berk.dataintegration.cli.CommandLineApp;
import com.berk.dataintegration.ui.ConversionController;
import com.berk.dataintegration.ui.ConversionViewModel;
import com.berk.dataintegration.ui.SwingConversionFrame;
import com.berk.dataintegration.ui.SwingWorkerConversionExecutor;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class DataIntegrationToolApp {
    static final String AWT_HEADLESS_PROPERTY = "java.awt.headless";
    static final String LOG4J_STATUS_LOGGER_LEVEL_PROPERTY = "log4j2.StatusLogger.level";

    private DataIntegrationToolApp() {
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            configureCliRuntimeProperties();
            int exitCode = new CommandLineApp().run(args, System.out, System.err);
            System.exit(exitCode);
        }
        SwingUtilities.invokeLater(() -> {
            setSystemLookAndFeel();
            ConversionViewModel model = new ConversionViewModel();
            ConversionController controller = new ConversionController(model, new SwingWorkerConversionExecutor());
            new SwingConversionFrame(model, controller).setVisible(true);
        });
    }

    static void configureCliRuntimeProperties() {
        System.setProperty(AWT_HEADLESS_PROPERTY, System.getProperty(AWT_HEADLESS_PROPERTY, "true"));
        System.setProperty(LOG4J_STATUS_LOGGER_LEVEL_PROPERTY, System.getProperty(LOG4J_STATUS_LOGGER_LEVEL_PROPERTY, "OFF"));
    }

    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ignored) {
            // Keep the default look and feel when the platform one is unavailable.
        }
    }
}
