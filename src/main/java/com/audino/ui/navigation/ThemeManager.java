package com.audino.ui.navigation;

import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Singleton theme manager for toggling and persisting Light and Dark clinical color schemes.
 */
public class ThemeManager {

    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);
    private static volatile ThemeManager instance;

    public enum Theme {
        LIGHT("/css/light.css"),
        DARK("/css/dark.css");

        private final String cssPath;

        Theme(String cssPath) {
            this.cssPath = cssPath;
        }

        public String getCssPath() {
            return cssPath;
        }
    }

    private Theme currentTheme = Theme.LIGHT;
    private final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);

    private ThemeManager() {
        String saved = prefs.get("app.theme", "LIGHT");
        try {
            this.currentTheme = Theme.valueOf(saved);
        } catch (IllegalArgumentException e) {
            this.currentTheme = Theme.LIGHT;
        }
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            synchronized (ThemeManager.class) {
                if (instance == null) {
                    instance = new ThemeManager();
                }
            }
        }
        return instance;
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public boolean isDarkMode() {
        return currentTheme == Theme.DARK;
    }

    public void toggleTheme(Scene scene) {
        setTheme(scene, currentTheme == Theme.LIGHT ? Theme.DARK : Theme.LIGHT);
    }

    public void setTheme(Scene scene, Theme theme) {
        this.currentTheme = Objects.requireNonNull(theme);
        prefs.put("app.theme", theme.name());
        logger.info("Theme switched to: {}", theme.name());

        if (scene != null) {
            applyTheme(scene);
        }
    }

    public void applyTheme(Scene scene) {
        if (scene == null) return;

        String lightCss = getClass().getResource(Theme.LIGHT.getCssPath()).toExternalForm();
        String darkCss = getClass().getResource(Theme.DARK.getCssPath()).toExternalForm();
        String appCss = getClass().getResource("/css/application.css").toExternalForm();

        scene.getStylesheets().remove(lightCss);
        scene.getStylesheets().remove(darkCss);
        scene.getStylesheets().remove(appCss);

        // Add theme-specific CSS tokens first, followed by application master stylesheet
        if (currentTheme == Theme.DARK) {
            scene.getStylesheets().add(darkCss);
        } else {
            scene.getStylesheets().add(lightCss);
        }
        scene.getStylesheets().add(appCss);
    }
}
