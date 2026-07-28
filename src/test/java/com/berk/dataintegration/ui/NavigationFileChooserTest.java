package com.berk.dataintegration.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.FlowLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NavigationFileChooserTest {
    @Test
    void installsSmallNavigationPanelInsideChooserControls() {
        NavigationFileChooser chooser = new NavigationFileChooser();

        JComponent navigationPanel = chooser.navigationPanelForTesting();

        assertNotNull(navigationPanel);
        JPanel panel = assertInstanceOf(JPanel.class, navigationPanel);
        FlowLayout layout = assertInstanceOf(FlowLayout.class, panel.getLayout());
        assertEquals(FlowLayout.LEFT, layout.getAlignment());
        assertEquals(2, panel.getComponentCount());
        assertEquals("Back", ((javax.swing.JButton) panel.getComponent(0)).getText());
        assertEquals("Forward", ((javax.swing.JButton) panel.getComponent(1)).getText());
    }
}
