package com.berk.dataintegration.app;

import com.berk.dataintegration.cli.CommandLineApp;
import com.berk.dataintegration.ui.ConversionController;
import com.berk.dataintegration.ui.ConversionViewModel;
import com.berk.dataintegration.ui.SwingConversionFrame;
import com.berk.dataintegration.ui.SwingWorkerConversionExecutor;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class DataIntegrationToolApp {
    private DataIntegrationToolApp() {
    }

    public static void main(String[] args) {
        if (args.length > 0) {
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

    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ignored) {
            // Keep the default look and feel when the platform one is unavailable.
        }
    }
}
