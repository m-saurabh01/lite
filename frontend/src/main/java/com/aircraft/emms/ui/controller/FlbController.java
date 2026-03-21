package com.aircraft.emms.ui.controller;

import com.aircraft.emms.ui.model.*;
import com.aircraft.emms.ui.service.BackendService;
import com.aircraft.emms.ui.service.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class FlbController {

    @FXML private TableView<FlightLogBookDto> flbTable;
    @FXML private Button createFlbBtn;
    @FXML private Button editFlbBtn;
    @FXML private Button submitFlbBtn;
    @FXML private Label flbErrorLabel;

    // Form
    @FXML private TitledPane flbFormPane;
    @FXML private ComboBox<String> sortieCombo;
    @FXML private TextField flbAircraftType;
    @FXML private TextField flbAircraftNum;
    @FXML private TextField takeoffField;
    @FXML private TextField landingField;
    @FXML private Label durationLabel;
    @FXML private TextArea flbRemarks;
    @FXML private VBox meterEntriesContainer;

    private final BackendService backend = BackendService.getInstance();
    private final SessionManager session = SessionManager.getInstance();
    private final List<MeterEntryRow> meterRows = new ArrayList<>();
    private Long editingFlbId = null;

    @FXML
    public void initialize() {
        flbFormPane.setExpanded(false);

        // Selection listener
        flbTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            editFlbBtn.setDisable(!hasSelection || (newVal != null && newVal.getStatus() != FlbStatus.DRAFT));
            submitFlbBtn.setDisable(!hasSelection || (newVal != null && newVal.getStatus() != FlbStatus.DRAFT));
        });

        // Auto-calculate duration on time field change
        takeoffField.textProperty().addListener((obs, o, n) -> updateDuration());
        landingField.textProperty().addListener((obs, o, n) -> updateDuration());

        loadFlbs();
    }

    @FXML
    private void handleRefresh() {
        loadFlbs();
    }

    @FXML
    private void handleCreateFlb() {
        clearForm();
        editingFlbId = null;
        flbFormPane.setExpanded(true);
        loadSortiesForCombo();
    }

    @FXML
    private void handleEditFlb() {
        FlightLogBookDto selected = flbTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        editingFlbId = selected.getId();
        flbAircraftType.setText(selected.getAircraftType());
        flbAircraftNum.setText(selected.getAircraftNumber());
        if (selected.getActualTakeoffTime() != null)
            takeoffField.setText(selected.getActualTakeoffTime().toString());
        if (selected.getActualLandingTime() != null)
            landingField.setText(selected.getActualLandingTime().toString());
        flbRemarks.setText(selected.getRemarks());

        // Load existing meter entries
        meterRows.clear();
        meterEntriesContainer.getChildren().clear();
        if (selected.getMeterEntries() != null) {
            for (MeterEntryDto me : selected.getMeterEntries()) {
                addMeterRow(me.getMeterName(), me.getMeterValue(), me.isMandatory());
            }
        }

        flbFormPane.setExpanded(true);
    }

    @FXML
    private void handleSubmitFlb() {
        FlightLogBookDto selected = flbTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        new Thread(() -> {
            try {
                backend.submitFlb(selected.getId());
                Platform.runLater(this::loadFlbs);
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleAddMeter() {
        addMeterRow("", null, false);
    }

    @FXML
    private void handleSaveFlb() {
        flbErrorLabel.setText("");

        String aircraftType = flbAircraftType.getText().trim();
        String aircraftNum = flbAircraftNum.getText().trim();

        if (aircraftType.isEmpty() || aircraftNum.isEmpty()) {
            showError("Aircraft type and number are required");
            return;
        }

        FlightLogBookDto dto = new FlightLogBookDto();
        dto.setAircraftType(aircraftType);
        dto.setAircraftNumber(aircraftNum);
        dto.setPilotId(session.getUserId());
        dto.setRemarks(flbRemarks.getText());

        try {
            String t = takeoffField.getText().trim();
            if (!t.isEmpty()) dto.setActualTakeoffTime(LocalTime.parse(t));
            String l = landingField.getText().trim();
            if (!l.isEmpty()) dto.setActualLandingTime(LocalTime.parse(l));
        } catch (DateTimeParseException e) {
            showError("Invalid time format. Use HH:mm");
            return;
        }

        // Handle sortie combo
        String sortieSelection = sortieCombo.getValue();
        if (sortieSelection != null && !sortieSelection.isEmpty()) {
            try {
                Long sortieId = Long.parseLong(sortieSelection.split(" - ")[0]);
                dto.setSortieId(sortieId);
            } catch (NumberFormatException ignored) {}
        }

        // Collect meter entries
        List<MeterEntryDto> meters = new ArrayList<>();
        for (MeterEntryRow row : meterRows) {
            String name = row.nameField.getText().trim();
            String value = row.valueField.getText().trim();
            if (!name.isEmpty() && !value.isEmpty()) {
                MeterEntryDto me = new MeterEntryDto();
                me.setMeterName(name);
                try {
                    me.setMeterValue(new BigDecimal(value));
                } catch (NumberFormatException e) {
                    showError("Invalid meter value for " + name);
                    return;
                }
                meters.add(me);
            } else if (row.mandatory && (name.isEmpty() || value.isEmpty())) {
                showError("Mandatory meter '" + row.nameField.getText() + "' requires a value");
                return;
            }
        }
        dto.setMeterEntries(meters);

        new Thread(() -> {
            try {
                if (editingFlbId != null) {
                    backend.updateFlb(editingFlbId, dto);
                } else {
                    backend.createFlb(dto);
                }
                Platform.runLater(() -> {
                    flbFormPane.setExpanded(false);
                    loadFlbs();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleCancelFlbForm() {
        flbFormPane.setExpanded(false);
        clearForm();
    }

    private void loadFlbs() {
        flbErrorLabel.setText("");
        new Thread(() -> {
            try {
                List<FlightLogBookDto> flbs = session.isPilot() ?
                        backend.getMyFlbs() : backend.getAllFlbs();
                Platform.runLater(() -> flbTable.setItems(FXCollections.observableArrayList(flbs)));
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            }
        }).start();
    }

    private void loadSortiesForCombo() {
        new Thread(() -> {
            try {
                List<SortieDto> sorties = backend.getMySorties();
                Platform.runLater(() -> {
                    sortieCombo.getItems().clear();
                    sorties.forEach(s -> sortieCombo.getItems().add(
                            s.getId() + " - " + s.getSortieNumber() + " (" + s.getAircraftNumber() + ")"));
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private void loadMeterDefinitions(String aircraftType) {
        new Thread(() -> {
            try {
                List<MeterEntryDto> defs = backend.getMeterDefinitions(aircraftType);
                Platform.runLater(() -> {
                    meterRows.clear();
                    meterEntriesContainer.getChildren().clear();
                    for (MeterEntryDto def : defs) {
                        addMeterRow(def.getMeterName(), null, def.isMandatory());
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private void addMeterRow(String name, BigDecimal value, boolean mandatory) {
        MeterEntryRow row = new MeterEntryRow(name, value, mandatory);
        meterRows.add(row);
        meterEntriesContainer.getChildren().add(row.container);
    }

    private void updateDuration() {
        try {
            String t = takeoffField.getText().trim();
            String l = landingField.getText().trim();
            if (!t.isEmpty() && !l.isEmpty()) {
                LocalTime takeoff = LocalTime.parse(t);
                LocalTime landing = LocalTime.parse(l);
                long mins = Duration.between(takeoff, landing).toMinutes();
                if (mins < 0) mins += 1440;
                durationLabel.setText(mins + " min");
            } else {
                durationLabel.setText("-- min");
            }
        } catch (DateTimeParseException e) {
            durationLabel.setText("-- min");
        }
    }

    private void clearForm() {
        sortieCombo.setValue(null);
        flbAircraftType.clear();
        flbAircraftNum.clear();
        takeoffField.clear();
        landingField.clear();
        durationLabel.setText("-- min");
        flbRemarks.clear();
        meterRows.clear();
        meterEntriesContainer.getChildren().clear();
        editingFlbId = null;
    }

    private void showError(String msg) {
        flbErrorLabel.setText(msg);
    }

    /** Inner class representing a meter entry UI row */
    private static class MeterEntryRow {
        final HBox container;
        final TextField nameField;
        final TextField valueField;
        final boolean mandatory;

        MeterEntryRow(String name, BigDecimal value, boolean mandatory) {
            this.mandatory = mandatory;
            container = new HBox(10);
            container.setAlignment(Pos.CENTER_LEFT);
            container.getStyleClass().add("meter-row");

            if (mandatory) {
                Label star = new Label("*");
                star.getStyleClass().add("mandatory-indicator");
                container.getChildren().add(star);
            }

            nameField = new TextField(name);
            nameField.setPromptText("Meter Name");
            nameField.setPrefWidth(180);

            valueField = new TextField(value != null ? value.toPlainString() : "");
            valueField.setPromptText("Value");
            valueField.setPrefWidth(120);

            container.getChildren().addAll(
                    new Label("Meter:"), nameField,
                    new Label("Value:"), valueField
            );
        }
    }
}
