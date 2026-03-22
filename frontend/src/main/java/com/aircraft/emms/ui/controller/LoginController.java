package com.aircraft.emms.ui.controller;

import com.aircraft.emms.ui.model.LoginResponse;
import com.aircraft.emms.ui.service.ApiClient;
import com.aircraft.emms.ui.service.BackendService;
import com.aircraft.emms.ui.service.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class LoginController {

    @FXML private TextField serviceIdField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Label errorLabel;
    @FXML private Label statusLabel;

    private final BackendService backendService = BackendService.getInstance();

    @FXML
    public void initialize() {
        errorLabel.setText("");
        System.out.println("[LOGIN] initialize called - fields: serviceId=" + serviceIdField + " password=" + passwordField + " status=" + statusLabel);
        checkBackendConnection();
    }

    private void checkBackendConnection() {
        statusLabel.setText("Checking backend...");
        new Thread(() -> {
            boolean connected = false;
            for (int i = 0; i < 30; i++) {
                if (ApiClient.getInstance().isBackendReady()) {
                    connected = true;
                    break;
                }
                try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
            }
            boolean isConnected = connected;
            Platform.runLater(() -> {
                if (isConnected) {
                    statusLabel.setText("Backend connected");
                    statusLabel.setStyle("-fx-text-fill: green;");
                } else {
                    statusLabel.setText("Backend unavailable. Please restart.");
                    statusLabel.setStyle("-fx-text-fill: red;");
                }
            });
        }).start();
    }

    @FXML
    private void handleLogin() {
        String serviceId = serviceIdField.getText().trim();
        String password = passwordField.getText();
        System.out.println("[LOGIN] handleLogin: serviceId='" + serviceId + "' password.length=" + password.length());

        if (serviceId.isEmpty()) {
            showError("Service ID is required");
            return;
        }
        if (password.isEmpty()) {
            showError("Password is required");
            return;
        }

        loginButton.setDisable(true);
        errorLabel.setText("");

        new Thread(() -> {
            try {
                LoginResponse response = backendService.login(serviceId, password);
                SessionManager.getInstance().login(response);

                // Fetch current user details for userId
                var userDetails = backendService.getCurrentUser();
                SessionManager.getInstance().setUserId(userDetails.getId());

                Platform.runLater(this::navigateToDashboard);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError(e.getMessage());
                    loginButton.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleForgotPassword() {
        TextInputDialog serviceIdDialog = new TextInputDialog();
        serviceIdDialog.setTitle("Password Reset");
        serviceIdDialog.setHeaderText("Enter your Service ID");
        serviceIdDialog.setContentText("Service ID:");

        Optional<String> serviceIdResult = serviceIdDialog.showAndWait();
        if (serviceIdResult.isEmpty() || serviceIdResult.get().isBlank()) return;

        String serviceId = serviceIdResult.get().trim();

        new Thread(() -> {
            try {
                String question = backendService.getSecurityQuestion(serviceId);
                Platform.runLater(() -> showSecurityQuestionDialog(serviceId, question));
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            }
        }).start();
    }

    private void showSecurityQuestionDialog(String serviceId, String question) {
        TextInputDialog answerDialog = new TextInputDialog();
        answerDialog.setTitle("Security Question");
        answerDialog.setHeaderText(question);
        answerDialog.setContentText("Answer:");

        Optional<String> answerResult = answerDialog.showAndWait();
        if (answerResult.isEmpty() || answerResult.get().isBlank()) return;

        TextInputDialog newPassDialog = new TextInputDialog();
        newPassDialog.setTitle("New Password");
        newPassDialog.setHeaderText("Enter your new password");
        newPassDialog.setContentText("New Password:");

        Optional<String> newPassResult = newPassDialog.showAndWait();
        if (newPassResult.isEmpty() || newPassResult.get().isBlank()) return;

        new Thread(() -> {
            try {
                backendService.resetPassword(serviceId, answerResult.get(), newPassResult.get());
                Platform.runLater(() -> {
                    errorLabel.setStyle("-fx-text-fill: green;");
                    errorLabel.setText("Password reset successful. Please login.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            }
        }).start();
    }

    private void navigateToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/aircraft/emms/ui/views/Dashboard.fxml"));
            Parent dashboard = loader.load();
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(dashboard, 1024, 700));
            stage.setTitle("EMMS Lite - " + SessionManager.getInstance().getName());
            stage.centerOnScreen();
        } catch (IOException e) {
            showError("Failed to load dashboard: " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setText(message);
    }
}
