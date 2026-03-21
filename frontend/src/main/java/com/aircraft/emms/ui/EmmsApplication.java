package com.aircraft.emms.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class EmmsApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/aircraft/emms/ui/views/Login.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 450, 500);
        primaryStage.setTitle("EMMS Lite - Login");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(450);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    @Override
    public void stop() {
        // Cleanup on app close
        try {
            com.aircraft.emms.ui.service.BackendService.getInstance().logout();
        } catch (Exception ignored) {}
    }
}
