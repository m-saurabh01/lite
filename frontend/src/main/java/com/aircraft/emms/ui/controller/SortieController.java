package com.aircraft.emms.ui.controller;

import com.aircraft.emms.ui.model.Role;
import com.aircraft.emms.ui.model.SortieDto;
import com.aircraft.emms.ui.model.SortieStatus;
import com.aircraft.emms.ui.model.UserDto;
import com.aircraft.emms.ui.service.BackendService;
import com.aircraft.emms.ui.service.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public class SortieController {

    @FXML private TableView<SortieDto> sortieTable;
    @FXML private Button createSortieBtn;
    @FXML private Button assignPilotBtn;
    @FXML private Button acceptBtn;
    @FXML private Button rejectBtn;
    @FXML private Button cancelBtn;
    @FXML private Label clashLabel;
    @FXML private Label errorLabel;

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

    @FXML
    public void initialize() {
        // Role-based visibility
        boolean canCreate = session.isAdmin() || session.isCaptain();
        createSortieBtn.setVisible(canCreate);
        createSortieBtn.setManaged(canCreate);
        assignPilotBtn.setVisible(canCreate);
        cancelBtn.setVisible(canCreate);

        acceptBtn.setVisible(session.isPilot());
        acceptBtn.setManaged(session.isPilot());
        rejectBtn.setVisible(session.isPilot());
        rejectBtn.setManaged(session.isPilot());

        // Selection listener for action buttons
        sortieTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateActionButtons(newVal);
        });

        formPane.setExpanded(false);
        loadSorties();
    }

    @FXML
    private void handleRefresh() {
        loadSorties();
    }

    @FXML
    private void handleCreateSortie() {
        clearForm();
        formPane.setExpanded(true);
    }

    @FXML
    private void handleSaveSortie() {
        errorLabel.setText("");

        String sortieNum = sortieNumField.getText().trim();
        String aircraftType = aircraftTypeField.getText().trim();
        String aircraftNum = aircraftNumField.getText().trim();
        LocalDate date = datePicker.getValue();

        if (sortieNum.isEmpty() || aircraftType.isEmpty() || aircraftNum.isEmpty() || date == null) {
            showError("Please fill all required fields");
            return;
        }

        SortieDto dto = new SortieDto();
        dto.setSortieNumber(sortieNum);
        dto.setAircraftType(aircraftType);
        dto.setAircraftNumber(aircraftNum);
        dto.setScheduledDate(date);
        dto.setRemarks(remarksField.getText());

        try {
            String start = startTimeField.getText().trim();
            if (!start.isEmpty()) dto.setScheduledStart(LocalTime.parse(start));
            String end = endTimeField.getText().trim();
            if (!end.isEmpty()) dto.setScheduledEnd(LocalTime.parse(end));
        } catch (DateTimeParseException e) {
            showError("Invalid time format. Use HH:mm");
            return;
        }

        new Thread(() -> {
            try {
                SortieDto created = backend.createSortie(dto);
                Platform.runLater(() -> {
                    formPane.setExpanded(false);
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

        // Show pilot selection dialog
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

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(selection -> {
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

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Reject this sortie?", ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            new Thread(() -> {
                try {
                    backend.rejectSortie(selected.getId());
                    Platform.runLater(this::loadSorties);
                } catch (Exception e) {
                    Platform.runLater(() -> showError(e.getMessage()));
                }
            }).start();
        }
    }

    @FXML
    private void handleCancel() {
        SortieDto selected = sortieTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Cancel this sortie?", ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            new Thread(() -> {
                try {
                    // Post to cancel endpoint would need to be added; using status update
                    // For now, this demonstrates the pattern
                    Platform.runLater(this::loadSorties);
                } catch (Exception e) {
                    Platform.runLater(() -> showError(e.getMessage()));
                }
            }).start();
        }
    }

    @FXML
    private void handleCancelForm() {
        formPane.setExpanded(false);
        clearForm();
    }

    private void loadSorties() {
        errorLabel.setText("");
        new Thread(() -> {
            try {
                List<SortieDto> sorties = backend.getMySorties();
                Platform.runLater(() -> {
                    sortieTable.setItems(FXCollections.observableArrayList(sorties));
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

        if (selected != null && selected.isHasClash()) {
            clashLabel.setText("CLASH: " + selected.getClashDetails());
        } else {
            clashLabel.setText("");
        }
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
