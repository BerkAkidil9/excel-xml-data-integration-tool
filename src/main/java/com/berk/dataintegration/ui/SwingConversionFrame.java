package com.berk.dataintegration.ui;

import com.berk.dataintegration.service.ConversionRecordCounts;
import com.berk.dataintegration.validation.ValidationError;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SwingConversionFrame extends JFrame {
    private final ConversionViewModel model;
    private final ConversionController controller;
    private final JComboBox<ConversionDirection> directionSelector = new JComboBox<>(ConversionDirection.values());
    private final JTextField inputField = new JTextField();
    private final JTextField outputField = new JTextField();
    private final JButton convertButton = new JButton("Convert");
    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel statusLabel = new JLabel();
    private final JLabel countsLabel = new JLabel();
    private final ErrorTableModel errorTableModel = new ErrorTableModel();

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
        panel.add(new JScrollPane(new JTable(errorTableModel)), BorderLayout.CENTER);
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

        add(panel, constraints, 2, 3, 0, convertButton);

        directionSelector.addActionListener(event -> controller.updateDirection((ConversionDirection) directionSelector.getSelectedItem()));
        convertButton.addActionListener(event -> {
            syncPathsFromFields();
            controller.convert();
        });
        return panel;
    }

    private JPanel statusPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        progressBar.setIndeterminate(true);
        panel.add(progressBar, BorderLayout.NORTH);
        panel.add(statusLabel, BorderLayout.CENTER);
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
            model.setInputPath(path);
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

    private JFileChooser chooser(String extension) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("*." + extension, extension));
        return chooser;
    }

    private void syncPathsFromFields() {
        model.setInputPath(path(inputField.getText()));
        Path outputPath = OutputPathExtensions.withExpectedExtension(
                path(outputField.getText()),
                model.direction().outputExtension()
        );
        outputField.setText(outputPath == null ? "" : outputPath.toString());
        model.setOutputPath(outputPath);
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
        convertButton.setEnabled(!model.running());
        progressBar.setVisible(model.running());
        statusLabel.setText(model.status());
        countsLabel.setText(countsText(model.recordCounts()));
        errorTableModel.setErrors(model.errors());
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

    private static final class ErrorTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {
                "Code", "Category", "Field", "Record", "Source", "Sheet/Path", "Row", "Column", "Value", "Message"
        };
        private List<ValidationError> errors = new ArrayList<>();

        void setErrors(List<ValidationError> errors) {
            this.errors = errors == null ? List.of() : List.copyOf(errors);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return errors.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ValidationError error = errors.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> error.code();
                case 1 -> error.category();
                case 2 -> error.field();
                case 3 -> error.recordId();
                case 4 -> error.sourceFile();
                case 5 -> error.sheet();
                case 6 -> error.row();
                case 7 -> error.column();
                case 8 -> error.value();
                case 9 -> error.message();
                default -> "";
            };
        }
    }
}
