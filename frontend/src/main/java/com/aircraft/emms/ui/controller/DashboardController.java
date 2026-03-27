package com.aircraft.emms.ui.controller;

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
    @FXML private Button navImport;
    @FXML private Button navUsers;
    @FXML private Button navAircraft;
    @FXML private Button navFlb;
    @FXML private Button navWorkOrder;
    @FXML private Button navSnag;
    @FXML private Button navReports;
    @FXML private StackPane contentArea;
    @FXML private Label activeAircraftNav;

    private final SessionManager session = SessionManager.getInstance();
    private Button activeNavButton = null;

    @FXML
    public void initialize() {
        userInfoLabel.setText(session.getName() + " (" + session.getServiceId() + ")");
        roleLabel.setText(session.getRolesDisplay());

        boolean isAdmin = session.isAdmin();

        // Admin nav items
        setNavVisible(navImport, isAdmin);
        setNavVisible(navUsers, isAdmin);
        setNavVisible(navAircraft, isAdmin);

        // User nav items (visible for non-admin roles)
        setNavVisible(navFlb, !isAdmin);
        setNavVisible(navWorkOrder, !isAdmin);
        setNavVisible(navSnag, !isAdmin);
        setNavVisible(navReports, !isAdmin);

        // Load active aircraft info
        loadActiveAircraftInfo();

        // Auto-show first relevant screen
        if (isAdmin) {
            showXmlImport();
        } else {
            showSortieFlb();
        }
    }

    @FXML
    private void showXmlImport() {
        loadView("/com/aircraft/emms/ui/views/XmlImport.fxml", "Import / Export", navImport);
    }

    @FXML
    private void showUsers() {
        loadView("/com/aircraft/emms/ui/views/UserManagement.fxml", "User Management", navUsers);
    }

    @FXML
    private void showAircraft() {
        loadView("/com/aircraft/emms/ui/views/AircraftManagement.fxml", "Aircraft Management", navAircraft);
    }

    @FXML
    private void showSortieFlb() {
        loadView("/com/aircraft/emms/ui/views/SortieFlbView.fxml", "Sorties & Flight Log Book", navFlb);
    }

    @FXML
    private void showWorkOrders() {
        loadView("/com/aircraft/emms/ui/views/PlaceholderWorkOrder.fxml", "Work Orders", navWorkOrder);
    }

    @FXML
    private void showSnagReporting() {
        loadView("/com/aircraft/emms/ui/views/PlaceholderSnag.fxml", "Snag Reporting", navSnag);
    }

    @FXML
    private void showReports() {
        loadView("/com/aircraft/emms/ui/views/PlaceholderReports.fxml", "Reports", navReports);
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

    private void loadView(String fxmlPath, String title, Button navButton) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            setActiveNav(navButton);
        } catch (IOException e) {
            contentArea.getChildren().clear();
            Label error = new Label("Error loading view: " + e.getMessage());
            error.getStyleClass().add("error-label");
            contentArea.getChildren().add(error);
        }
    }

    private void setNavVisible(Button btn, boolean visible) {
        btn.setVisible(visible);
        btn.setManaged(visible);
    }

    private void setActiveNav(Button btn) {
        if (activeNavButton != null) {
            activeNavButton.getStyleClass().remove("nav-button-active");
        }
        activeNavButton = btn;
        if (btn != null && !btn.getStyleClass().contains("nav-button-active")) {
            btn.getStyleClass().add("nav-button-active");
        }
    }

    private void loadActiveAircraftInfo() {
        new Thread(() -> {
            try {
                var ac = BackendService.getInstance().getActiveAircraft();
                Platform.runLater(() -> activeAircraftNav.setText("Aircraft: " + ac.getAircraftName()));
            } catch (Exception e) {
                Platform.runLater(() -> activeAircraftNav.setText("No active aircraft"));
            }
        }).start();
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
