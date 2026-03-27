package com.aircraft.emms.ui.controller;

import com.aircraft.emms.ui.model.Role;
import com.aircraft.emms.ui.model.SortieDto;
import com.aircraft.emms.ui.model.SortieStatus;
import com.aircraft.emms.ui.model.UserDto;
import com.aircraft.emms.ui.service.BackendService;
import com.aircraft.emms.ui.service.SessionManager;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public class SortieController {

    @FXML private TableView<SortieDto> sortieTable;
    @FXML private Button createSortieBtn;
    @FXML private Button assignPilotBtn;
    @FXML private Button acceptBtn;
    @FXML private Button rejectBtn;
    @FXML private Button cancelBtn;
    @FXML private Button closeBtn;
    @FXML private Label clashLabel;
    @FXML private Label errorLabel;
    @FXML private TextField searchField;

    // Form fields
    @FXML private TitledPane formPane;
    @FXML private TextField sortieNumField;
    @FXML private TextField aircraftTypeField;
    @FXML private TextField aircraftNumField;
    @FXML private DatePicker datePicker;
    @FXML private TextField startTimeField;
    @FXML private TextField endTimeField;
    @FXML private TextArea remarksField;

    private final BackendService backend = BackendService.getInstance();
    private final SessionManager session = SessionManager.getInstance();
    private FilteredList<SortieDto> filteredSorties;

    @FXML
    public void initialize() {
        // Captain-only creation
        boolean canCreate = session.isCaptain();
        createSortieBtn.setVisible(canCreate);
        createSortieBtn.setManaged(canCreate);

        boolean canManage = session.isAdmin() || session.isCaptain();
        assignPilotBtn.setVisible(canManage);
        cancelBtn.setVisible(canManage);
        closeBtn.setVisible(canManage);

        boolean canRespond = session.isPilot();
        acceptBtn.setVisible(canRespond);
        acceptBtn.setManaged(canRespond);
        rejectBtn.setVisible(canRespond);
        rejectBtn.setManaged(canRespond);

        // Selection listener for action buttons
        sortieTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateActionButtons(newVal);
        });

        // Search filter
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (filteredSorties != null) {
                    filteredSorties.setPredicate(s -> {
                        if (newVal == null || newVal.isBlank()) return true;
                        String lower = newVal.toLowerCase();
                        return (s.getSortieNumber() != null && s.getSortieNumber().toLowerCase().contains(lower))
                            || (s.getAircraftNumber() != null && s.getAircraftNumber().toLowerCase().contains(lower))
                            || (s.getPilotName() != null && s.getPilotName().toLowerCase().contains(lower))
                            || (s.getStatus() != null && s.getStatus().name().toLowerCase().contains(lower));
                    });
                }
            });
        }

        formPane.setExpanded(false);
        formPane.setVisible(false);
        formPane.setManaged(false);
        loadSorties();
    }

    @FXML
    private void handleRefresh() {
        loadSorties();
    }

    @FXML
    private void handleCreateSortie() {
        clearForm();
        formPane.setVisible(true);
        formPane.setManaged(true);
        formPane.setExpanded(true);
        // Auto-populate aircraft info from active aircraft
        new Thread(() -> {
            try {
                var ac = backend.getActiveAircraft();
                Platform.runLater(() -> {
                    sortieNumField.setText("(Auto-generated)");
                    aircraftTypeField.setText(ac.getAircraftName());
                    aircraftNumField.setText(ac.getAssetNum());
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to load aircraft info: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleSaveSortie() {
        errorLabel.setText("");

        LocalDate date = datePicker.getValue();

        if (date == null) {
            showError("Please select a date");
            return;
        }

        SortieDto dto = new SortieDto();
        dto.setScheduledDate(date);
        dto.setRemarks(remarksField.getText());

        try {
            String start = startTimeField.getText().trim();
            String end = endTimeField.getText().trim();
            if (!start.isEmpty()) dto.setScheduledStart(LocalTime.parse(start));
            if (!end.isEmpty()) dto.setScheduledEnd(LocalTime.parse(end));

            if (!start.isEmpty() && !end.isEmpty()) {
                LocalTime st = LocalTime.parse(start);
                LocalTime et = LocalTime.parse(end);
                if (!et.isAfter(st)) {
                    showError("End time must be after start time");
                    return;
                }
            }
        } catch (DateTimeParseException e) {
            showError("Invalid time format. Use HH:mm");
            return;
        }

        new Thread(() -> {
            try {
                backend.createSortie(dto);
                Platform.runLater(() -> {
                    formPane.setExpanded(false);
                    formPane.setVisible(false);
                    formPane.setManaged(false);
                    loadSorties();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleAssignPilot() {
        SortieDto selected = sortieTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        new Thread(() -> {
            try {
                List<UserDto> pilots = backend.getUsersByRole(Role.PILOT);
                Platform.runLater(() -> showPilotSelectionDialog(selected, pilots));
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            }
        }).start();
    }

    private void showPilotSelectionDialog(SortieDto sortie, List<UserDto> pilots) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>();
        dialog.setTitle("Assign Pilot");
        dialog.setHeaderText("Select a pilot for sortie " + sortie.getSortieNumber());

        List<String> pilotOptions = pilots.stream()
                .map(p -> p.getId() + " - " + p.getName() + " (" + p.getServiceId() + ")")
                .toList();
        dialog.getItems().addAll(pilotOptions);

        dialog.showAndWait().ifPresent(selection -> {
            Long pilotId = Long.parseLong(selection.split(" - ")[0]);
            new Thread(() -> {
                try {
                    backend.assignPilot(sortie.getId(), pilotId);
                    Platform.runLater(this::loadSorties);
                } catch (Exception e) {
                    Platform.runLater(() -> showError(e.getMessage()));
                }
            }).start();
        });
    }

    @FXML
    private void handleAccept() {
        SortieDto selected = sortieTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        new Thread(() -> {
            try {
                backend.acceptSortie(selected.getId());
                Platform.runLater(this::loadSorties);
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleReject() {
        SortieDto selected = sortieTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog remarksDialog = new TextInputDialog();
        remarksDialog.setTitle("Reject Sortie");
        remarksDialog.setHeaderText("Reject sortie " + selected.getSortieNumber());
        remarksDialog.setContentText("Remarks (mandatory):");

        remarksDialog.showAndWait().ifPresent(remarks -> {
            if (remarks.trim().isEmpty()) {
                showError("Remarks are mandatory for rejection");
                return;
            }
            new Thread(() -> {
                try {
                    backend.rejectSortie(selected.getId(), remarks.trim());
                    Platform.runLater(this::loadSorties);
                } catch (Exception e) {
                    Platform.runLater(() -> showError(e.getMessage()));
                }
            }).start();
        });
    }

    @FXML
    private void handleCancel() {
        SortieDto selected = sortieTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        showInlineConfirm("Cancel sortie " + selected.getSortieNumber() + "?",
                "This action will cancel the sortie.", () -> {
            new Thread(() -> {
                try {
                    backend.cancelSortie(selected.getId());
                    Platform.runLater(this::loadSorties);
                } catch (Exception e) {
                    Platform.runLater(() -> showError(e.getMessage()));
                }
            }).start();
        });
    }

    @FXML
    private void handleClose() {
        SortieDto selected = sortieTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        showInlineConfirm("Close sortie " + selected.getSortieNumber() + "?",
                "All linked FLBs must be closed. This cannot be undone.", () -> {
            new Thread(() -> {
                try {
                    backend.closeSortie(selected.getId());
                    Platform.runLater(this::loadSorties);
                } catch (Exception e) {
                    Platform.runLater(() -> showError(e.getMessage()));
                }
            }).start();
        });
    }

    @FXML
    private void handleCancelForm() {
        formPane.setExpanded(false);
        formPane.setVisible(false);
        formPane.setManaged(false);
        clearForm();
    }

    private void loadSorties() {
        errorLabel.setText("");
        new Thread(() -> {
            try {
                List<SortieDto> sorties = backend.getMySorties();
                Platform.runLater(() -> {
                    filteredSorties = new FilteredList<>(FXCollections.observableArrayList(sorties));
                    sortieTable.setItems(filteredSorties);
                    // Re-apply search filter
                    if (searchField != null && searchField.getText() != null && !searchField.getText().isBlank()) {
                        String text = searchField.getText();
                        searchField.setText("");
                        searchField.setText(text);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            }
        }).start();
    }

    private void updateActionButtons(SortieDto selected) {
        boolean hasSelection = selected != null;
        assignPilotBtn.setDisable(!hasSelection || (selected != null && selected.getStatus() != SortieStatus.CREATED));
        acceptBtn.setDisable(!hasSelection || (selected != null && selected.getStatus() != SortieStatus.ASSIGNED));
        rejectBtn.setDisable(!hasSelection || (selected != null && selected.getStatus() != SortieStatus.ASSIGNED));
        cancelBtn.setDisable(!hasSelection);
        closeBtn.setDisable(!hasSelection || selected == null ||
                (selected.getStatus() != SortieStatus.ACCEPTED &&
                 selected.getStatus() != SortieStatus.IN_PROGRESS &&
                 selected.getStatus() != SortieStatus.COMPLETED &&
                 selected.getStatus() != SortieStatus.REJECTED));

        if (selected != null && selected.isHasClash()) {
            clashLabel.setText("CLASH: " + selected.getClashDetails());
        } else {
            clashLabel.setText("");
        }
    }

    private void showInlineConfirm(String title, String message, Runnable onConfirm) {
        // Find the parent StackPane (contentArea)
        StackPane parent = findParentStackPane();
        if (parent == null) {
            // Fallback to Alert
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
        Button cancelBtnDialog = new Button("Cancel");
        cancelBtnDialog.getStyleClass().add("btn-secondary");

        HBox buttons = new HBox(10, cancelBtnDialog, confirmBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        dialog.getChildren().addAll(titleLabel, msgLabel, buttons);
        overlay.getChildren().add(dialog);

        parent.getChildren().add(overlay);

        cancelBtnDialog.setOnAction(e -> parent.getChildren().remove(overlay));
        confirmBtn.setOnAction(e -> {
            parent.getChildren().remove(overlay);
            onConfirm.run();
        });
    }

    private StackPane findParentStackPane() {
        javafx.scene.Node node = sortieTable;
        while (node != null) {
            if (node.getParent() instanceof StackPane sp) return sp;
            node = node.getParent();
        }
        return null;
    }

    private void clearForm() {
        sortieNumField.clear();
        aircraftTypeField.clear();
        aircraftNumField.clear();
        datePicker.setValue(null);
        startTimeField.clear();
        endTimeField.clear();
        remarksField.clear();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
    }
}
