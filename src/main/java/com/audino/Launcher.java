package com.audino;

/**
 * Main application launcher class that does not extend javafx.application.Application.
 * Required for JavaFX 11+ modular runtime and Maven Shade Fat JAR execution.
 */
public class Launcher {

    public static void main(String[] args) {
        AudinoApplication.main(args);
    }
}
