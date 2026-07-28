package com.berk.dataintegration.app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataIntegrationToolAppTest {
    private final String originalHeadless = System.getProperty(DataIntegrationToolApp.AWT_HEADLESS_PROPERTY);
    private final String originalLog4jStatus = System.getProperty(DataIntegrationToolApp.LOG4J_STATUS_LOGGER_LEVEL_PROPERTY);

    @AfterEach
    void restoreRuntimeProperties() {
        restore(DataIntegrationToolApp.AWT_HEADLESS_PROPERTY, originalHeadless);
        restore(DataIntegrationToolApp.LOG4J_STATUS_LOGGER_LEVEL_PROPERTY, originalLog4jStatus);
    }

    @Test
    void configuresCliRuntimeDefaultsWhenPropertiesAreMissing() {
        System.clearProperty(DataIntegrationToolApp.AWT_HEADLESS_PROPERTY);
        System.clearProperty(DataIntegrationToolApp.LOG4J_STATUS_LOGGER_LEVEL_PROPERTY);

        DataIntegrationToolApp.configureCliRuntimeProperties();

        assertEquals("true", System.getProperty(DataIntegrationToolApp.AWT_HEADLESS_PROPERTY));
        assertEquals("OFF", System.getProperty(DataIntegrationToolApp.LOG4J_STATUS_LOGGER_LEVEL_PROPERTY));
    }

    @Test
    void keepsUserProvidedRuntimeProperties() {
        System.setProperty(DataIntegrationToolApp.AWT_HEADLESS_PROPERTY, "false");
        System.setProperty(DataIntegrationToolApp.LOG4J_STATUS_LOGGER_LEVEL_PROPERTY, "WARN");

        DataIntegrationToolApp.configureCliRuntimeProperties();

        assertEquals("false", System.getProperty(DataIntegrationToolApp.AWT_HEADLESS_PROPERTY));
        assertEquals("WARN", System.getProperty(DataIntegrationToolApp.LOG4J_STATUS_LOGGER_LEVEL_PROPERTY));
    }

    private void restore(String propertyName, String originalValue) {
        if (originalValue == null) {
            System.clearProperty(propertyName);
            return;
        }
        System.setProperty(propertyName, originalValue);
    }
}
