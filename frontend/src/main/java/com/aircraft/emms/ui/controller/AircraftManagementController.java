package com.aircraft.emms.ui.controller;

import com.aircraft.emms.ui.model.AircraftDataSetDto;
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

import java.util.List;

public class AircraftManagementController {

    @FXML private Label activeAircraftLabel;
    @FXML private TableView<AircraftDataSetDto> aircraftTable;
    @FXML private Button activateBtn;
    @FXML private Button truncateBtn;
    @FXML private Label errorLabel;
    @FXML private TextField aircraftSearchField;

    private final BackendService backend = BackendService.getInstance();
    private FilteredList<AircraftDataSetDto> filteredAircraft;

    @FXML
    public void initialize() {
        aircraftTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            activateBtn.setDisable(!hasSelection);
            truncateBtn.setDisable(!hasSelection);
        });

        if (aircraftSearchField != null) {
            aircraftSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (filteredAircraft != null) {
                    filteredAircraft.setPredicate(a -> {
                        if (newVal == null || newVal.isBlank()) return true;
                        String lower = newVal.toLowerCase();
                        return (a.getAssetNum() != null && a.getAssetNum().toLowerCase().contains(lower))
                            || (a.getAircraftName() != null && a.getAircraftName().toLowerCase().contains(lower))
                            || (a.getAircraftType() != null && a.getAircraftType().toLowerCase().contains(lower));
                    });
                }
            });
        }

        loadData();
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    @FXML
    private void handleActivate() {
        AircraftDataSetDto selected = aircraftTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        showInlineConfirm("Activate aircraft '" + selected.getAircraftName() + "'?",
                "This will deactivate the current active aircraft.", () -> {
            new Thread(() -> {
                try {
                    backend.activateAircraft(selected.getId());
                    Platform.runLater(this::loadData);
                } catch (Exception e) {
                    Platform.runLater(() -> showError(e.getMessage()));
                }
            }).start();
        });
    }

    @FXML
    private void handleTruncate() {
        AircraftDataSetDto selected = aircraftTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        showInlineConfirm("Truncate aircraft '" + selected.getAircraftName() + "'?",
                "WARNING: This will permanently delete ALL data including users, meters, sorties, FLBs, and snags. This action cannot be undone!", () -> {
            new Thread(() -> {
                try {
                    backend.truncateAircraft(selected.getId());
                    Platform.runLater(this::loadData);
                } catch (Exception e) {
                    Platform.runLater(() -> showError(e.getMessage()));
                }
            }).start();
        });
    }

    private void loadData() {
        errorLabel.setText("");
        loadAircraft();
    }

    private void loadAircraft() {
        new Thread(() -> {
            try {
                List<AircraftDataSetDto> datasets = backend.listAircraft();
                Platform.runLater(() -> {
                    filteredAircraft = new FilteredList<>(FXCollections.observableArrayList(datasets));
                    aircraftTable.setItems(filteredAircraft);
                    datasets.stream().filter(AircraftDataSetDto::isActive).findFirst()
                            .ifPresentOrElse(
                                    a -> activeAircraftLabel.setText("Active: " + a.getAircraftName()),
                                    () -> activeAircraftLabel.setText("No active aircraft"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            }
        }).start();
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
        dialog.setMaxWidth(420);
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
        javafx.scene.Node node = aircraftTable;
        while (node != null) {
            if (node.getParent() instanceof StackPane sp) return sp;
            node = node.getParent();
        }
        return null;
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
    }
}
