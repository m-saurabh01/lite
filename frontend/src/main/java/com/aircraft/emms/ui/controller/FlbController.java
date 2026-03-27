package com.aircraft.emms.ui.controller;

import com.aircraft.emms.ui.model.*;
import com.aircraft.emms.ui.service.BackendService;
import com.aircraft.emms.ui.service.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
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
    @FXML private Button closeFlbBtn;
    @FXML private Button abortFlbBtn;
    @FXML private Label flbErrorLabel;
    @FXML private TextField flbSearchField;

    // Form
    @FXML private TitledPane flbFormPane;
    @FXML private ComboBox<String> sortieCombo;
    @FXML private TextField flbAircraftType;
    @FXML private TextField flbAircraftNum;
    @FXML private TextField takeoffField;
    @FXML private TextField landingField;
    @FXML private Label durationLabel;
    @FXML private TextArea flbRemarks;
    @FXML private TableView<MeterEntryDto> meterTable;
    @FXML private TableColumn<MeterEntryDto, String> meterNameCol;
    @FXML private TableColumn<MeterEntryDto, String> meterUomCol;
    @FXML private TableColumn<MeterEntryDto, String> meterValueCol;
    @FXML private TableColumn<MeterEntryDto, String> meterValidationCol;
    @FXML private Button fetchMetersBtn;
    @FXML private TextField meterSearchField;

    private final BackendService backend = BackendService.getInstance();
    private final SessionManager session = SessionManager.getInstance();
    private final ObservableList<MeterEntryDto> meterData = FXCollections.observableArrayList();
    private FilteredList<MeterEntryDto> filteredMeterData;
    private Long editingFlbId = null;
    private FilteredList<FlightLogBookDto> filteredFlbs;

    @FXML
    public void initialize() {
        flbFormPane.setExpanded(false);
        flbFormPane.setVisible(false);
        flbFormPane.setManaged(false);

        // Setup meter table columns — show ★ prefix for mandatory meters
        meterNameCol.setCellValueFactory(cd -> {
            String name = cd.getValue().getMeterName();
            String prefix = cd.getValue().isMandatory() ? "★ " : "";
            return new SimpleStringProperty(prefix + name);
        });
        meterUomCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getUnitOfMeasure() != null ? cd.getValue().getUnitOfMeasure() : ""));
        meterValueCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getMeterValue() != null ? cd.getValue().getMeterValue().toPlainString() : ""));
        meterValueCol.setCellFactory(col -> {
            TableCell<MeterEntryDto, String> cell = new TableCell<>() {
                private final TextField textField = new TextField();
                {
                    textField.setStyle("-fx-padding: 2 4 2 4; -fx-font-size: 13px;");
                    textField.setOnAction(e -> commitEdit(textField.getText()));
                    textField.focusedProperty().addListener((obs, wasFocused, isNow) -> {
                        if (!isNow) commitEdit(textField.getText());
                    });
                }
                @Override
                public void startEdit() {
                    super.startEdit();
                    textField.setText(getItem());
                    setGraphic(textField);
                    setText(null);
                    textField.requestFocus();
                    textField.selectAll();
                }
                @Override
                public void cancelEdit() {
                    super.cancelEdit();
                    setText(getItem());
                    setGraphic(null);
                }
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                        setGraphic(null);
                    } else if (isEditing()) {
                        textField.setText(item);
                        setGraphic(textField);
                        setText(null);
                    } else {
                        setText(item);
                        setGraphic(null);
                    }
                }
            };
            return cell;
        });
        meterValueCol.setOnEditCommit(event -> {
            MeterEntryDto row = event.getRowValue();
            String newVal = event.getNewValue() != null ? event.getNewValue().trim() : "";
            if (newVal.isEmpty()) {
                row.setMeterValue(null);
                row.setValidationMsg(row.isMandatory() ? "Required" : "");
            } else {
                try {
                    BigDecimal val = new BigDecimal(newVal);
                    if (val.compareTo(BigDecimal.ZERO) < 0) {
                        row.setValidationMsg("Must be >= 0");
                    } else {
                        row.setMeterValue(val);
                        row.setValidationMsg("Validated");
                    }
                } catch (NumberFormatException e) {
                    row.setValidationMsg("Not a valid number");
                }
            }
            meterTable.refresh();
        });

        // Validation column — shows inline errors in red
        meterValidationCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getValidationMsg() != null ? cd.getValue().getValidationMsg() : ""));
        meterValidationCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setText(null);
                    setStyle("");
                } else if ("Validated".equals(item)) {
                    setText(item);
                    setStyle("-fx-text-fill: #38a169; -fx-font-size: 11px; -fx-font-weight: bold;");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #c53030; -fx-font-size: 11px;");
                }
            }
        });

        // Mandatory meter name: only the ★ is red, rest is normal
        meterNameCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (item.startsWith("★")) {
                    setText(null);
                    javafx.scene.text.TextFlow flow = new javafx.scene.text.TextFlow();
                    javafx.scene.text.Text star = new javafx.scene.text.Text("★ ");
                    star.setStyle("-fx-fill: #c53030; -fx-font-weight: bold;");
                    javafx.scene.text.Text name = new javafx.scene.text.Text(item.substring(2));
                    name.setStyle("-fx-fill: #1a202c;");
                    flow.getChildren().addAll(star, name);
                    setGraphic(flow);
                } else {
                    setText(item);
                    setGraphic(null);
                    setStyle("");
                }
            }
        });
        meterTable.setEditable(true);
        filteredMeterData = new FilteredList<>(meterData, p -> true);
        meterTable.setItems(filteredMeterData);

        // Meter search filter
        if (meterSearchField != null) {
            meterSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
                filteredMeterData.setPredicate(m -> {
                    if (newVal == null || newVal.isBlank()) return true;
                    String lower = newVal.toLowerCase();
                    return (m.getMeterName() != null && m.getMeterName().toLowerCase().contains(lower))
                        || (m.getUnitOfMeasure() != null && m.getUnitOfMeasure().toLowerCase().contains(lower));
                });
            });
        }

        // Selection listener — button states based on FLB status flow
        flbTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            FlbStatus st = hasSelection ? newVal.getStatus() : null;
            // Edit only in DRAFT or OPEN
            editFlbBtn.setDisable(!hasSelection || (st != FlbStatus.DRAFT && st != FlbStatus.OPEN));
            // Close only from OPEN (all meters must be filled — validated server-side)
            closeFlbBtn.setDisable(!hasSelection || st != FlbStatus.OPEN);
            // Abort from DRAFT or OPEN
            abortFlbBtn.setDisable(!hasSelection || (st != FlbStatus.DRAFT && st != FlbStatus.OPEN));
        });

        // Auto-calculate duration on time field change
        takeoffField.textProperty().addListener((obs, o, n) -> updateDuration());
        landingField.textProperty().addListener((obs, o, n) -> updateDuration());

        // Search filter
        if (flbSearchField != null) {
            flbSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (filteredFlbs != null) {
                    filteredFlbs.setPredicate(f -> {
                        if (newVal == null || newVal.isBlank()) return true;
                        String lower = newVal.toLowerCase();
                        return (f.getSortieNumber() != null && f.getSortieNumber().toLowerCase().contains(lower))
                            || (f.getAircraftNumber() != null && f.getAircraftNumber().toLowerCase().contains(lower))
                            || (f.getPilotName() != null && f.getPilotName().toLowerCase().contains(lower))
                            || (f.getStatus() != null && f.getStatus().name().toLowerCase().contains(lower));
                    });
                }
            });
        }

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
        flbFormPane.setVisible(true);
        flbFormPane.setManaged(true);
        flbFormPane.setExpanded(true);
        loadSortiesForCombo();
        if (fetchMetersBtn != null) fetchMetersBtn.setDisable(false);
        // Auto-populate aircraft info from active aircraft
        new Thread(() -> {
            try {
                var ac = backend.getActiveAircraft();
                Platform.runLater(() -> {
                    flbAircraftType.setText(ac.getAircraftName());
                    flbAircraftNum.setText(ac.getAssetNum());
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to load aircraft info: " + e.getMessage()));
            }
        }).start();
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
        meterData.clear();
        if (selected.getMeterEntries() != null) {
            meterData.addAll(selected.getMeterEntries());
        }

        flbFormPane.setVisible(true);
        flbFormPane.setManaged(true);
        flbFormPane.setExpanded(true);
        if (fetchMetersBtn != null) fetchMetersBtn.setDisable(false);
    }

    @FXML
    private void handleCloseFlb() {
        FlightLogBookDto selected = flbTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        showInlineConfirm("Close FLB #" + selected.getId() + "?",
                "All mandatory meters must have values. This cannot be undone.", () -> {
            new Thread(() -> {
                try {
                    backend.closeFlb(selected.getId());
                    Platform.runLater(this::loadFlbs);
                } catch (Exception e) {
                    Platform.runLater(() -> showError(e.getMessage()));
                }
            }).start();
        });
    }

    @FXML
    private void handleAbortFlb() {
        FlightLogBookDto selected = flbTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        showInlineConfirm("Abort FLB #" + selected.getId() + "?",
                "This will abort the FLB. This cannot be undone.", () -> {
            new Thread(() -> {
                try {
                    backend.abortFlb(selected.getId());
                    Platform.runLater(this::loadFlbs);
                } catch (Exception e) {
                    Platform.runLater(() -> showError(e.getMessage()));
                }
            }).start();
        });
    }

    @FXML
    private void handleFetchMeters() {
        flbErrorLabel.setText("");
        new Thread(() -> {
            try {
                List<MeterEntryDto> defs = backend.getActiveAircraftMeterDefs();
                Platform.runLater(() -> {
                    meterData.clear();
                    meterData.addAll(defs);
                    if (defs.isEmpty()) {
                        showError("No meter definitions found for active aircraft");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Failed to fetch meters: " + e.getMessage()));
            }
        }).start();
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

            if (!t.isEmpty() && !l.isEmpty()) {
                LocalTime takeoff = LocalTime.parse(t);
                LocalTime landing = LocalTime.parse(l);
                if (!landing.isAfter(takeoff)) {
                    showError("Landing time must be after takeoff time");
                    return;
                }
            }
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
        for (MeterEntryDto row : meterData) {
            String name = row.getMeterName();
            BigDecimal value = row.getMeterValue();
            if (name != null && !name.isEmpty() && value != null) {
                MeterEntryDto me = new MeterEntryDto();
                me.setMeterName(name);
                me.setMeterValue(value);
                me.setMeterDefinitionId(row.getMeterDefinitionId());
                meters.add(me);
            } else if (row.isMandatory() && (name == null || name.isEmpty() || value == null)) {
                showError("Mandatory meter '" + name + "' requires a value");
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
                    flbFormPane.setVisible(false);
                    flbFormPane.setManaged(false);
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
        flbFormPane.setVisible(false);
        flbFormPane.setManaged(false);
        clearForm();
    }

    private void loadFlbs() {
        flbErrorLabel.setText("");
        new Thread(() -> {
            try {
                // All roles can see FLBs
                List<FlightLogBookDto> flbs = backend.getAllFlbs();
                Platform.runLater(() -> {
                    filteredFlbs = new FilteredList<>(FXCollections.observableArrayList(flbs));
                    flbTable.setItems(filteredFlbs);
                });
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
        meterData.clear();
        if (meterSearchField != null) meterSearchField.clear();
        editingFlbId = null;
    }

    private void showError(String msg) {
        flbErrorLabel.setText(msg);
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
        Button cancelBtnDialog = new Button("Cancel");
        cancelBtnDialog.getStyleClass().add("btn-secondary");

        HBox buttons = new HBox(10, cancelBtnDialog, confirmBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        dialog.getChildren().addAll(titleLabel, msgLabel, buttons);
        overlay.getChildren().add(dialog);
        parent.getChildren().add(overlay);
         // to prevent dropdown from showing over the dialog

        cancelBtnDialog.setOnAction(e -> parent.getChildren().remove(overlay));
        confirmBtn.setOnAction(e -> {
            parent.getChildren().remove(overlay);
            onConfirm.run();
        });
    }

    private StackPane findParentStackPane() {
        javafx.scene.Node node = flbTable;
        while (node != null) {
            if (node.getParent() instanceof StackPane sp) return sp;
            node = node.getParent();
        }
        return null;
    }
}
