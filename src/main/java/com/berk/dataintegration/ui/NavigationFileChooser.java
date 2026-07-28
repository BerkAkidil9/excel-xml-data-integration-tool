package com.berk.dataintegration.ui;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.file.Path;

final class NavigationFileChooser extends JFileChooser {
    static final String NAVIGATION_PANEL_NAME = "fileChooserNavigationPanel";

    private final DirectoryHistory history = new DirectoryHistory();
    private final JButton backButton = new JButton("Back");
    private final JButton forwardButton = new JButton("Forward");
    private boolean applyingHistory;

    NavigationFileChooser() {
        File currentDirectory = getCurrentDirectory();
        if (currentDirectory != null) {
            history.record(currentDirectory.toPath());
        }
        configureNavigationButtons();
        addPropertyChangeListener(DIRECTORY_CHANGED_PROPERTY, event -> recordDirectoryChange((File) event.getNewValue()));
        updateNavigationButtons();
    }

    private void configureNavigationButtons() {
        backButton.addActionListener(event -> moveTo(history.goBack()));
        forwardButton.addActionListener(event -> moveTo(history.goForward()));
        backButton.setMargin(new Insets(2, 8, 2, 8));
        forwardButton.setMargin(new Insets(2, 8, 2, 8));
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        panel.setName(NAVIGATION_PANEL_NAME);
        panel.add(backButton);
        panel.add(forwardButton);
        installNavigationPanel(panel);
    }

    private void installNavigationPanel(JPanel navigationPanel) {
        JPanel controlPanel = findControlButtonPanel(this);
        if (controlPanel != null && controlPanel.getParent() instanceof JPanel parentPanel) {
            addNavigationPanelToControlRow(parentPanel, controlPanel, navigationPanel);
            return;
        }
        setAccessory(navigationPanel);
    }

    private void addNavigationPanelToControlRow(JPanel parentPanel, JPanel controlPanel, JPanel navigationPanel) {
        parentPanel.remove(controlPanel);
        parentPanel.setLayout(new BorderLayout(8, 0));
        parentPanel.add(navigationPanel, BorderLayout.LINE_START);
        parentPanel.add(controlPanel, BorderLayout.LINE_END);
        parentPanel.revalidate();
        parentPanel.repaint();
    }

    private JPanel findControlButtonPanel(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JPanel panel && containsApproveAndCancelButtons(panel)) {
                return panel;
            }
            if (component instanceof Container child) {
                JPanel result = findControlButtonPanel(child);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private boolean containsApproveAndCancelButtons(JPanel panel) {
        boolean approveFound = false;
        boolean cancelFound = false;
        for (Component component : panel.getComponents()) {
            if (component instanceof JButton button) {
                approveFound = approveFound || isApproveButton(button);
                cancelFound = cancelFound || isCancelButton(button);
            }
        }
        return approveFound && cancelFound;
    }

    private boolean isApproveButton(JButton button) {
        Object actionCommand = button.getActionCommand();
        return APPROVE_SELECTION.equals(actionCommand);
    }

    private boolean isCancelButton(JButton button) {
        Object actionCommand = button.getActionCommand();
        return CANCEL_SELECTION.equals(actionCommand);
    }

    private void recordDirectoryChange(File directory) {
        if (applyingHistory || directory == null) {
            updateNavigationButtons();
            return;
        }
        history.record(directory.toPath());
        updateNavigationButtons();
    }

    private void moveTo(Path directory) {
        if (directory == null) {
            updateNavigationButtons();
            return;
        }
        applyingHistory = true;
        try {
            setCurrentDirectory(directory.toFile());
        } finally {
            applyingHistory = false;
            updateNavigationButtons();
        }
    }

    private void updateNavigationButtons() {
        backButton.setEnabled(history.canGoBack());
        forwardButton.setEnabled(history.canGoForward());
    }

    JComponent navigationPanelForTesting() {
        return findComponentByName(this, NAVIGATION_PANEL_NAME);
    }

    private JComponent findComponentByName(Container container, String name) {
        for (Component component : container.getComponents()) {
            if (component instanceof JComponent child && name.equals(child.getName())) {
                return child;
            }
            if (component instanceof Container childContainer) {
                JComponent result = findComponentByName(childContainer, name);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
