package com.imtidad.util;

import javafx.beans.value.ChangeListener;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Lightweight visual theme switcher for authenticated IMTIDAD screens.
 * It changes only the CSS layer; application workflows and data logic are untouched.
 */
public final class ThemeManager {
    private static final String DARK_THEME = stylesheetUrl("/dark-theme.css");
    private static final Map<Scene, ChangeListener<Parent>> INSTALLED_SCENES = new WeakHashMap<>();
    private static boolean darkMode = true;

    private ThemeManager() {
        // Utility class
    }

    public static synchronized void install(Scene scene) {
        if (scene == null) {
            return;
        }

        if (!INSTALLED_SCENES.containsKey(scene)) {
            ChangeListener<Parent> listener = (observable, oldRoot, newRoot) -> applyToRoot(newRoot);
            scene.rootProperty().addListener(listener);
            INSTALLED_SCENES.put(scene, listener);
        }

        applyToRoot(scene.getRoot());
    }

    public static synchronized void uninstall(Scene scene) {
        if (scene == null) {
            return;
        }

        ChangeListener<Parent> listener = INSTALLED_SCENES.remove(scene);
        if (listener != null) {
            scene.rootProperty().removeListener(listener);
        }
        removeTheme(scene.getRoot());
    }

    public static synchronized void toggle() {
        darkMode = !darkMode;
        for (Scene scene : new ArrayList<>(INSTALLED_SCENES.keySet())) {
            if (scene != null) {
                applyToRoot(scene.getRoot());
            }
        }
    }

    public static synchronized boolean isDarkMode() {
        return darkMode;
    }

    private static void applyToRoot(Parent root) {
        if (root == null) {
            return;
        }

        removeTheme(root);
        if (darkMode && DARK_THEME != null && !root.getStylesheets().contains(DARK_THEME)) {
            root.getStylesheets().add(DARK_THEME);
        }
    }

    private static void removeTheme(Parent root) {
        if (root != null && DARK_THEME != null) {
            root.getStylesheets().remove(DARK_THEME);
        }
    }

    private static String stylesheetUrl(String path) {
        URL resource = ThemeManager.class.getResource(path);
        return resource == null ? null : resource.toExternalForm();
    }
}
