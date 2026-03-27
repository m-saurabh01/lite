package com.aircraft.emms.ui.controller;

import com.aircraft.emms.ui.model.Role;
import com.aircraft.emms.ui.model.UserDto;
import com.aircraft.emms.ui.service.BackendService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.*;

public class UserManagementController {

    @FXML private TableView<UserDto> userTable;
    @FXML private Button editUserBtn;
    @FXML private Button deactivateBtn;
    @FXML private Label userErrorLabel;
    @FXML private Label successLabel;
    @FXML private TextField userSearchField;

    // Form
    @FXML private TitledPane userFormPane;
    @FXML private TextField serviceIdField;
    @FXML private TextField nameField;
    @FXML private MenuButton rolesMenuButton;
    @FXML private PasswordField passwordField;
    @FXML private TextField securityQuestionField;
    @FXML private PasswordField securityAnswerField;

    private final BackendService backend = BackendService.getInstance();
    private Long editingUserId = null;
    private FilteredList<UserDto> filteredUsers;
    private final Map<String, CheckMenuItem> roleMenuItems = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        userFormPane.setExpanded(false);

        // Build role menu items dynamically from Role enum (excluding ADMIN)
        for (Role role : Role.values()) {
            if (role == Role.ADMIN) continue;
            CheckMenuItem item = new CheckMenuItem(role.name());
            item.setOnAction(e -> updateRolesButtonText());
            roleMenuItems.put(role.name(), item);
            rolesMenuButton.getItems().add(item);
        }

        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            editUserBtn.setDisable(newVal == null);
            deactivateBtn.setDisable(newVal == null);
        });

        if (userSearchField != null) {
            userSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (filteredUsers != null) {
                    filteredUsers.setPredicate(u -> {
                        if (newVal == null || newVal.isBlank()) return true;
                        String lower = newVal.toLowerCase();
                        return (u.getServiceId() != null && u.getServiceId().toLowerCase().contains(lower))
                            || (u.getName() != null && u.getName().toLowerCase().contains(lower))
                            || (u.getRolesDisplay() != null && u.getRolesDisplay().toLowerCase().contains(lower));
                    });
                }
            });
        }

        loadUsers();
    }

    @FXML
    private void handleRefresh() {
        loadUsers();
    }

    @FXML
    private void handleEditUser() {
        UserDto selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        clearMessages();
        editingUserId = selected.getId();
        serviceIdField.setText(selected.getServiceId());
        nameField.setText(selected.getName());

        List<String> roles = selected.getRoles() != null ? selected.getRoles() : List.of();
        roleMenuItems.forEach((name, item) -> item.setSelected(roles.contains(name)));
        updateRolesButtonText();

        securityQuestionField.setText(selected.getSecurityQuestion());
        passwordField.clear();
        securityAnswerField.clear();

        userFormPane.setExpanded(true);
    }

    @FXML
    private void handleDeactivateUser() {
        UserDto selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        showInlineConfirm("Deactivate user " + selected.getServiceId() + "?",
                "The user will no longer be able to log in.", () -> {
            new Thread(() -> {
                try {
                    backend.deactivateUser(selected.getId());
                    Platform.runLater(() -> {
                        showSuccess("User " + selected.getServiceId() + " deactivated.");
                        loadUsers();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showError(e.getMessage()));
                }
            }).start();
        });
    }

    @FXML
    private void handleSaveUser() {
        clearMessages();

        String name = nameField.getText().trim();
        List<String> selectedRoles = getSelectedRoles();

        if (name.isEmpty()) {
            showError("Name is required");
            return;
        }
        if (selectedRoles.isEmpty()) {
            showError("At least one role must be selected");
            return;
        }
        if (editingUserId == null) {
            showError("No user selected for editing");
            return;
        }

        UserDto dto = new UserDto();
        dto.setServiceId(serviceIdField.getText().trim());
        dto.setName(name);
        dto.setRole(Role.valueOf(selectedRoles.get(0)));
        dto.setRoles(selectedRoles);
        dto.setActive(true);

        String password = passwordField.getText();
        if (!password.isEmpty()) dto.setPassword(password);
        String sq = securityQuestionField.getText().trim();
        if (!sq.isEmpty()) dto.setSecurityQuestion(sq);
        String sa = securityAnswerField.getText();
        if (!sa.isEmpty()) dto.setSecurityAnswer(sa);

        new Thread(() -> {
            try {
                backend.updateUser(editingUserId, dto);
                Platform.runLater(() -> {
                    userFormPane.setExpanded(false);
                    showSuccess("User updated successfully.");
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

    private List<String> getSelectedRoles() {
        List<String> roles = new ArrayList<>();
        roleMenuItems.forEach((name, item) -> {
            if (item.isSelected()) roles.add(name);
        });
        return roles;
    }

    private void loadUsers() {
        new Thread(() -> {
            try {
                List<UserDto> users = backend.getAllUsers();
                Platform.runLater(() -> {
                    filteredUsers = new FilteredList<>(FXCollections.observableArrayList(users));
                    userTable.setItems(filteredUsers);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            }
        }).start();
    }

    private void clearForm() {
        serviceIdField.clear();
        nameField.clear();
        roleMenuItems.values().forEach(item -> item.setSelected(false));
        updateRolesButtonText();
        passwordField.clear();
        securityQuestionField.clear();
        securityAnswerField.clear();
        editingUserId = null;
    }

    private void clearMessages() {
        userErrorLabel.setText("");
        successLabel.setText("");
    }

    private void showError(String msg) {
        successLabel.setText("");
        userErrorLabel.setText(msg);
    }

    private void showSuccess(String msg) {
        userErrorLabel.setText("");
        successLabel.setText(msg);
    }

    private void updateRolesButtonText() {
        List<String> selected = getSelectedRoles();
        rolesMenuButton.setText(selected.isEmpty() ? "Select Roles..." : String.join(", ", selected));
    }

    private void showInlineConfirm(String title, String message, Runnable onConfirm) {
        StackPane parent = findParentStackPane();
        if (parent == null) {
            onConfirm.run();
            return;
        }

        VBox overlay = new VBox(12);
        overlay.setAlignment(Pos.CENTER);
        overlay.getStyleClass().add("confirm-overlay");

        VBox dialog = new VBox(12);
        dialog.getStyleClass().add("confirm-dialog");
        dialog.setAlignment(Pos.CENTER);
        dialog.setMaxWidth(400);
        dialog.setPadding(new Insets(24));

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #1a202c;");
        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-text-fill: #718096; -fx-font-size: 13px;");
        msgLabel.setWrapText(true);

        Button confirmBtn = new Button("Confirm");
        confirmBtn.getStyleClass().add("btn-danger");
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");

        HBox buttons = new HBox(10, cancelBtn, confirmBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        dialog.getChildren().addAll(titleLabel, msgLabel, buttons);
        overlay.getChildren().add(dialog);
        parent.getChildren().add(overlay);

        cancelBtn.setOnAction(e -> parent.getChildren().remove(overlay));
        confirmBtn.setOnAction(e -> {
            parent.getChildren().remove(overlay);
            onConfirm.run();
        });
    }

    private StackPane findParentStackPane() {
        javafx.scene.Node node = userTable;
        while (node != null) {
            if (node.getParent() instanceof StackPane sp) return sp;
            node = node.getParent();
        }
        return null;
    }
}
