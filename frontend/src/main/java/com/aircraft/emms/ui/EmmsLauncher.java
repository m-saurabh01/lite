package com.aircraft.emms.ui;

import javafx.application.Application;

/**
 * Launcher class required for jpackage and non-modular setups.
 * This class does NOT extend Application, allowing it to be the
 * main entry point when building a native installer.
 */
public class EmmsLauncher {

    public static void main(String[] args) {
        Application.launch(EmmsApplication.class, args);
    }
}
