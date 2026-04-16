package com.aircraft.emms.ui;

import javafx.application.Application;

/**
 * Launcher class required for jpackage and non-modular setups.
 * This class does NOT extend Application, allowing it to be the
 * main entry point when building a native installer.
 */
public class EmmsLauncher {

    public static void main(String[] args) {
        // Fix Windows COM initialization for native dialogs (FileChooser etc.)
        System.setProperty("glass.accessible.force", "false");
        Application.launch(EmmsApplication.class, args);
    }
}
