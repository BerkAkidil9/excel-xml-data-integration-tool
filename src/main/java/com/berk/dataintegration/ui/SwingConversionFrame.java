package com.berk.dataintegration.ui;

import com.berk.dataintegration.service.ConversionRecordCounts;
import com.berk.dataintegration.validation.ValidationError;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SwingConversionFrame extends JFrame {
    private final ConversionViewModel model;
    private final ConversionController controller;
    private final JComboBox<ConversionDirection> directionSelector = new JComboBox<>(ConversionDirection.values());
    private final JTextField inputField = new JTextField();
    private final JTextField outputField = new JTextField();
    private final JButton convertButton = new JButton("Convert");
    private final JButton resetButton = new JButton("Reset");
    private final JButton saveErrorReportButton = new JButton("Save Error Report");
    private final JButton saveTemplateButton = new JButton("Save Excel Template");
    private final JButton copySelectedErrorButton = new JButton("Copy Selected Error");
    private final JComboBox<ErrorCategoryFilter> errorFilterSelector = new JComboBox<>(ErrorCategoryFilter.values());
    private final JProgressBar progressBar = new JProgressBar();
    private final JPanel statusBanner = new JPanel(new BorderLayout());
    private final JLabel statusLabel = new JLabel();
    private final JLabel countsLabel = new JLabel();
    private final JLabel errorSummaryLabel = new JLabel();
    private final ErrorTableModel errorTableModel = new ErrorTableModel();
    private final JTable errorTable = new JTable(errorTableModel);
    private final TableRowSorter<ErrorTableModel> errorSorter = new TableRowSorter<>(errorTableModel);
    private final JTextArea errorDetailArea = new JTextArea(6, 80);

    public SwingConversionFrame(ConversionViewModel model, ConversionController controller) {
        super("Excel/XML Data Integration Tool");
        this.model = model;
        this.controller = controller;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));
        setContentPane(content());
        bindModel();
        refresh();
        setSize(980, 560);
        setLocationRelativeTo(null);
    }

    private JPanel content() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(formPanel(), BorderLayout.NORTH);
        panel.add(errorPanel(), BorderLayout.CENTER);
        panel.add(statusPanel(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel formPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        add(panel, constraints, 0, 0, 0, new JLabel("Direction"));
        constraints.weightx = 1;
        add(panel, constraints, 1, 0, 2, directionSelector);
        constraints.weightx = 0;

        add(panel, constraints, 0, 1, 0, new JLabel("Input"));
        constraints.weightx = 1;
        add(panel, constraints, 1, 1, 1, inputField);
        constraints.weightx = 0;
        add(panel, constraints, 2, 1, 0, button("Browse", this::chooseInput));

        add(panel, constraints, 0, 2, 0, new JLabel("Output"));
        constraints.weightx = 1;
        add(panel, constraints, 1, 2, 1, outputField);
        constraints.weightx = 0;
        add(panel, constraints, 2, 2, 0, button("Browse", this::chooseOutput));

        add(panel, constraints, 0, 3, 0, saveTemplateButton);
        add(panel, constraints, 1, 3, 0, saveErrorReportButton);
        add(panel, constraints, 2, 3, 0, resetButton);
        add(panel, constraints, 3, 3, 0, convertButton);

        directionSelector.addActionListener(event -> controller.updateDirection((ConversionDirection) directionSelector.getSelectedItem()));
        saveTemplateButton.addActionListener(event -> saveTemplate());
        saveErrorReportButton.addActionListener(event -> saveErrorReport());
        resetButton.addActionListener(event -> controller.reset());
        convertButton.addActionListener(event -> {
            syncPathsFromFields();
            if (!confirmOverwrite(model.outputPath())) {
                return;
            }
            controller.convert();
        });
        return panel;
    }

    private JPanel errorPanel() {
        configureErrorTable();
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel topPanel = new JPanel(new BorderLayout(8, 6));
        JPanel toolbar = new JPanel(new BorderLayout(8, 8));
        errorSummaryLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        toolbar.add(new JLabel("Error Filter"), BorderLayout.WEST);
        toolbar.add(errorFilterSelector, BorderLayout.CENTER);
        toolbar.add(copySelectedErrorButton, BorderLayout.EAST);
        topPanel.add(errorSummaryLabel, BorderLayout.NORTH);
        topPanel.add(toolbar, BorderLayout.SOUTH);
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(errorTable), BorderLayout.CENTER);

        errorDetailArea.setEditable(false);
        errorDetailArea.setLineWrap(true);
        errorDetailArea.setWrapStyleWord(true);
        JScrollPane detailScrollPane = new JScrollPane(errorDetailArea);
        detailScrollPane.setBorder(BorderFactory.createTitledBorder("Error Details"));
        panel.add(detailScrollPane, BorderLayout.SOUTH);

        errorFilterSelector.addActionListener(event -> applyErrorFilter());
        copySelectedErrorButton.addActionListener(event -> copySelectedError());
        errorTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateSelectedErrorDetail();
            }
        });
        return panel;
    }

    private void configureErrorTable() {
        errorTable.setRowSorter(errorSorter);
        errorTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        errorTable.setFillsViewportHeight(true);
        TableColumnModel columns = errorTable.getColumnModel();
        int[] widths = {150, 120, 120, 140, 150, 220, 70, 80, 180, 360};
        for (int i = 0; i < widths.length; i++) {
            columns.getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    private JPanel statusPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        progressBar.setIndeterminate(true);
        statusBanner.setOpaque(true);
        statusBanner.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        statusBanner.add(statusLabel, BorderLayout.CENTER);
        panel.add(progressBar, BorderLayout.NORTH);
        panel.add(statusBanner, BorderLayout.CENTER);
        panel.add(countsLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JButton button(String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(event -> action.run());
        return button;
    }

    private void add(
            JPanel panel,
            GridBagConstraints constraints,
            int x,
            int y,
            int width,
            java.awt.Component component
    ) {
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.gridwidth = width == 0 ? 1 : width;
        panel.add(component, constraints);
        constraints.gridwidth = 1;
    }

    private void chooseInput() {
        JFileChooser chooser = chooser(model.direction().inputExtension());
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path path = chooser.getSelectedFile().toPath();
            inputField.setText(path.toString());
            controller.updateInputPath(path);
            outputField.setText(model.outputPath() == null ? "" : model.outputPath().toString());
        }
    }

    private void chooseOutput() {
        JFileChooser chooser = chooser(model.direction().outputExtension());
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path path = OutputPathExtensions.withExpectedExtension(
                    chooser.getSelectedFile().toPath(),
                    model.direction().outputExtension()
            );
            outputField.setText(path.toString());
            model.setOutputPath(path);
        }
    }

    private void saveErrorReport() {
        JFileChooser chooser = chooser("csv");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path path = OutputPathExtensions.withExpectedExtension(chooser.getSelectedFile().toPath(), "csv");
            if (confirmOverwrite(path)) {
                controller.saveErrorReport(path);
            }
        }
    }

    private void saveTemplate() {
        JFileChooser chooser = chooser("xlsx");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path path = OutputPathExtensions.withExpectedExtension(chooser.getSelectedFile().toPath(), "xlsx");
            if (confirmOverwrite(path)) {
                controller.saveTemplate(path);
            }
        }
    }

    private JFileChooser chooser(String extension) {
        JFileChooser chooser = new NavigationFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("*." + extension, extension));
        return chooser;
    }

    private void syncPathsFromFields() {
        Path previousInputPath = model.inputPath();
        Path previousOutputPath = model.outputPath();
        Path inputPath = path(inputField.getText());
        Path typedOutputPath = path(outputField.getText());
        controller.updateInputPath(inputPath);
        Path outputPath = outputPathAfterInputSync(previousInputPath, previousOutputPath, typedOutputPath);
        outputField.setText(outputPath == null ? "" : outputPath.toString());
        model.setOutputPath(outputPath);
    }

    private Path outputPathAfterInputSync(Path previousInputPath, Path previousOutputPath, Path typedOutputPath) {
        if (typedOutputPath == null) {
            return model.outputPath();
        }
        if (isStaleSuggestedOutput(previousInputPath, previousOutputPath, typedOutputPath)) {
            return model.outputPath();
        }
        return OutputPathExtensions.withExpectedExtension(typedOutputPath, model.direction().outputExtension());
    }

    private boolean isStaleSuggestedOutput(Path previousInputPath, Path previousOutputPath, Path typedOutputPath) {
        return previousInputPath != null
                && previousOutputPath != null
                && previousOutputPath.equals(typedOutputPath)
                && OutputPathExtensions.replaceExtension(previousInputPath, model.direction().outputExtension())
                .equals(previousOutputPath);
    }

    private boolean confirmOverwrite(Path path) {
        if (path == null || !Files.exists(path)) {
            return true;
        }
        int answer = JOptionPane.showConfirmDialog(
                this,
                "The output file already exists. Overwrite it?",
                "Confirm Overwrite",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return answer == JOptionPane.YES_OPTION;
    }

    private Path path(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return Path.of(text.trim());
    }

    private void bindModel() {
        model.addPropertyChangeListener(event -> runOnEdt(this::refresh));
    }

    private void refresh() {
        directionSelector.setSelectedItem(model.direction());
        inputField.setText(model.inputPath() == null ? "" : model.inputPath().toString());
        outputField.setText(model.outputPath() == null ? "" : model.outputPath().toString());
        convertButton.setEnabled(!model.running());
        resetButton.setEnabled(!model.running());
        saveTemplateButton.setEnabled(!model.running());
        saveErrorReportButton.setEnabled(!model.running() && !model.errors().isEmpty());
        progressBar.setVisible(model.running());
        applyStatusPresentation();
        countsLabel.setText(countsText(model.recordCounts()));
        errorSummaryLabel.setText(ErrorSummaryFormatter.format(model.errors()));
        errorTableModel.setErrors(model.errors());
        applyErrorFilter();
        updateSelectedErrorDetail();
    }

    private void applyStatusPresentation() {
        ConversionStatusSeverity severity = model.statusSeverity();
        statusLabel.setText(statusText(severity, model.status()));
        if (severity == ConversionStatusSeverity.ERROR) {
            statusBanner.setBackground(new Color(255, 235, 238));
            statusBanner.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(198, 40, 40)),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
            statusLabel.setForeground(new Color(120, 20, 20));
        } else if (severity == ConversionStatusSeverity.SUCCESS) {
            statusBanner.setBackground(new Color(232, 245, 233));
            statusBanner.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(46, 125, 50)),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
            statusLabel.setForeground(new Color(27, 94, 32));
        } else if (severity == ConversionStatusSeverity.RUNNING) {
            statusBanner.setBackground(new Color(232, 240, 254));
            statusBanner.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(66, 133, 244)),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
            statusLabel.setForeground(new Color(23, 78, 166));
        } else {
            statusBanner.setBackground(getBackground());
            statusBanner.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            statusLabel.setForeground(Color.BLACK);
        }
    }

    private String statusText(ConversionStatusSeverity severity, String status) {
        if (severity == ConversionStatusSeverity.ERROR) {
            return "ERROR: " + status;
        }
        if (severity == ConversionStatusSeverity.SUCCESS) {
            return "SUCCESS: " + status;
        }
        if (severity == ConversionStatusSeverity.RUNNING) {
            return "WORKING: " + status;
        }
        return status;
    }

    private String countsText(ConversionRecordCounts counts) {
        return "Parties: " + counts.partyCount()
                + "   Payment accounts: " + counts.paymentAccountCount()
                + "   Invoices: " + counts.invoiceCount()
                + "   Lines: " + counts.invoiceLineCount();
    }

    private void runOnEdt(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    private void applyErrorFilter() {
        ErrorCategoryFilter selected = (ErrorCategoryFilter) errorFilterSelector.getSelectedItem();
        ErrorCategoryFilter filter = selected == null ? ErrorCategoryFilter.ALL : selected;
        errorSorter.setRowFilter(new javax.swing.RowFilter<>() {
            @Override
            public boolean include(Entry<? extends ErrorTableModel, ? extends Integer> entry) {
                return filter.accepts(entry.getModel().errorAt(entry.getIdentifier()));
            }
        });
        if (errorTable.getRowCount() > 0 && errorTable.getSelectedRow() < 0) {
            errorTable.setRowSelectionInterval(0, 0);
        }
    }

    private void updateSelectedErrorDetail() {
        ValidationError selectedError = selectedError();
        errorDetailArea.setText(ErrorDetailFormatter.format(selectedError));
        errorDetailArea.setCaretPosition(0);
        copySelectedErrorButton.setEnabled(!model.running() && selectedError != null);
    }

    private ValidationError selectedError() {
        int selectedRow = errorTable.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }
        return errorTableModel.errorAt(errorTable.convertRowIndexToModel(selectedRow));
    }

    private void copySelectedError() {
        String text = ErrorDetailFormatter.format(selectedError());
        if (text.isBlank()) {
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }
}
