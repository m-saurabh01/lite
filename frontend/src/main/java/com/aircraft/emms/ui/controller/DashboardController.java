package com.aircraft.emms.ui.controller;

import com.aircraft.emms.ui.model.Role;
import com.aircraft.emms.ui.service.BackendService;
import com.aircraft.emms.ui.service.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class DashboardController {

    @FXML private Label userInfoLabel;
    @FXML private Label roleLabel;
    @FXML private Button logoutButton;
    @FXML private VBox navPanel;
    @FXML private Button navSorties;
    @FXML private Button navFlb;
    @FXML private Button navUsers;
    @FXML private Button navImport;
    @FXML private StackPane contentArea;
    @FXML private Label statusBarLabel;
    @FXML private Label versionLabel;

    private final SessionManager session = SessionManager.getInstance();

    @FXML
    public void initialize() {
        userInfoLabel.setText(session.getName() + " (" + session.getServiceId() + ")");
        roleLabel.setText(session.getRole().name());

        // Role-based navigation visibility
        navUsers.setVisible(session.isAdmin());
        navUsers.setManaged(session.isAdmin());
        navImport.setVisible(session.isAdmin());
        navImport.setManaged(session.isAdmin());

        // Auto-show relevant screen
        if (session.isPilot()) {
            showSorties();
        } else if (session.isCaptain()) {
            showSorties();
        }
    }

    @FXML
    private void showSorties() {
        loadView("/com/aircraft/emms/ui/views/SortieManagement.fxml");
        statusBarLabel.setText("Sortie Management");
    }

    @FXML
    private void showFlb() {
        loadView("/com/aircraft/emms/ui/views/FlightLogBook.fxml");
        statusBarLabel.setText("Flight Log Book");
    }

    @FXML
    private void showUsers() {
        loadView("/com/aircraft/emms/ui/views/UserManagement.fxml");
        statusBarLabel.setText("User Management");
    }

    @FXML
    private void showXmlImport() {
        loadView("/com/aircraft/emms/ui/views/XmlImport.fxml");
        statusBarLabel.setText("XML Import");
    }

    @FXML
    private void handleLogout() {
        new Thread(() -> {
            try {
                BackendService.getInstance().logout();
            } catch (Exception ignored) {}
            session.logout();
            Platform.runLater(this::navigateToLogin);
        }).start();
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (IOException e) {
            contentArea.getChildren().clear();
            Label error = new Label("Error loading view: " + e.getMessage());
            error.getStyleClass().add("error-label");
            contentArea.getChildren().add(error);
        }
    }

    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/aircraft/emms/ui/views/Login.fxml"));
            Parent login = loader.load();
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(login, 450, 500));
            stage.setTitle("EMMS Lite - Login");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
