package com.aircraft.emms.ui.controller;

import com.aircraft.emms.ui.model.Role;
import com.aircraft.emms.ui.model.UserDto;
import com.aircraft.emms.ui.service.BackendService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.Optional;

public class UserManagementController {

    @FXML private TableView<UserDto> userTable;
    @FXML private Button editUserBtn;
    @FXML private Button deactivateBtn;
    @FXML private Label userErrorLabel;

    // Form
    @FXML private TitledPane userFormPane;
    @FXML private TextField serviceIdField;
    @FXML private TextField nameField;
    @FXML private ComboBox<Role> roleCombo;
    @FXML private PasswordField passwordField;
    @FXML private TextField securityQuestionField;
    @FXML private PasswordField securityAnswerField;

    private final BackendService backend = BackendService.getInstance();
    private Long editingUserId = null;

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList(Role.values()));
        userFormPane.setExpanded(false);

        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            editUserBtn.setDisable(newVal == null);
            deactivateBtn.setDisable(newVal == null);
        });

        loadUsers();
    }

    @FXML
    private void handleRefresh() {
        loadUsers();
    }

    @FXML
    private void handleAddUser() {
        clearForm();
        editingUserId = null;
        serviceIdField.setDisable(false);
        userFormPane.setExpanded(true);
    }

    @FXML
    private void handleEditUser() {
        UserDto selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        editingUserId = selected.getId();
        serviceIdField.setText(selected.getServiceId());
        serviceIdField.setDisable(true); // Cannot change service ID
        nameField.setText(selected.getName());
        roleCombo.setValue(selected.getRole());
        securityQuestionField.setText(selected.getSecurityQuestion());
        passwordField.clear(); // Don't show existing password

        userFormPane.setExpanded(true);
    }

    @FXML
    private void handleDeactivateUser() {
        UserDto selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Deactivate user " + selected.getServiceId() + "?",
                ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.YES) {
            new Thread(() -> {
                try {
                    backend.deactivateUser(selected.getId());
                    Platform.runLater(this::loadUsers);
                } catch (Exception e) {
                    Platform.runLater(() -> showError(e.getMessage()));
                }
            }).start();
        }
    }

    @FXML
    private void handleSaveUser() {
        userErrorLabel.setText("");

        String serviceId = serviceIdField.getText().trim();
        String name = nameField.getText().trim();
        Role role = roleCombo.getValue();
        String password = passwordField.getText();

        if (serviceId.isEmpty() || name.isEmpty() || role == null) {
            showError("Service ID, Name, and Role are required");
            return;
        }

        if (editingUserId == null && password.isEmpty()) {
            showError("Password is required for new users");
            return;
        }

        UserDto dto = new UserDto();
        dto.setServiceId(serviceId);
        dto.setName(name);
        dto.setRole(role);
        dto.setActive(true);
        if (!password.isEmpty()) dto.setPassword(password);
        String sq = securityQuestionField.getText().trim();
        if (!sq.isEmpty()) dto.setSecurityQuestion(sq);
        String sa = securityAnswerField.getText();
        if (!sa.isEmpty()) dto.setSecurityAnswer(sa);

        new Thread(() -> {
            try {
                if (editingUserId != null) {
                    backend.updateUser(editingUserId, dto);
                } else {
                    backend.createUser(dto);
                }
                Platform.runLater(() -> {
                    userFormPane.setExpanded(false);
                    loadUsers();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleCancelForm() {
        userFormPane.setExpanded(false);
        clearForm();
    }

    private void loadUsers() {
        userErrorLabel.setText("");
        new Thread(() -> {
            try {
                List<UserDto> users = backend.getAllUsers();
                Platform.runLater(() -> userTable.setItems(FXCollections.observableArrayList(users)));
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            }
        }).start();
    }

    private void clearForm() {
        serviceIdField.clear();
        serviceIdField.setDisable(false);
        nameField.clear();
        roleCombo.setValue(null);
        passwordField.clear();
        securityQuestionField.clear();
        securityAnswerField.clear();
        editingUserId = null;
    }

    private void showError(String msg) {
        userErrorLabel.setText(msg);
    }
}
