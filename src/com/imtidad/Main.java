package com.imtidad;

import com.imtidad.util.SchemaInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("🚀 بدء تشغيل واجهة نظام امتداد...");

        Parent root = FXMLLoader.load(getClass().getResource("/com/imtidad/view/Login.fxml"));

        // The login screen is a two-column workspace, not a narrow mobile view.
        Scene scene = new Scene(root, 1180, 720);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        primaryStage.setTitle("نظام امتداد - تسجيل الدخول");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(620);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // Initialize database schema before starting the application
        SchemaInitializer.initializeSchema();
        launch(args);
    }
}
