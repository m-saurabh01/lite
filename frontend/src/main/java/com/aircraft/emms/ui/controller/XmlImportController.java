package com.aircraft.emms.ui.controller;

import com.aircraft.emms.ui.model.ApiResponse;
import com.aircraft.emms.ui.service.BackendService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;

public class XmlImportController {

    @FXML private Label selectedFileLabel;
    @FXML private Button importButton;
    @FXML private ProgressBar progressBar;
    @FXML private VBox resultBox;
    @FXML private Label resultLabel;
    @FXML private TextArea errorLogArea;
    @FXML private TableView<?> historyTable;
    @FXML private Label importErrorLabel;

    private final BackendService backend = BackendService.getInstance();
    private File selectedFile;

    @FXML
    public void initialize() {
        loadHistory();
    }

    @FXML
    private void handleSelectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select XML ZIP File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("ZIP Files", "*.zip"));

        Stage stage = (Stage) importButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            selectedFile = file;
            selectedFileLabel.setText(file.getName());
            importButton.setDisable(false);
        }
    }

    @FXML
    private void handleImport() {
        if (selectedFile == null) return;

        importButton.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setProgress(-1); // Indeterminate
        resultBox.setVisible(false);
        importErrorLabel.setText("");

        Path filePath = selectedFile.toPath();

        new Thread(() -> {
            try {
                ApiResponse<Object> result = backend.importXmlZip(filePath);
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    resultBox.setVisible(true);

                    if (result.isSuccess()) {
                        resultLabel.setStyle("-fx-text-fill: green;");
                        resultLabel.setText("Import successful: " + result.getMessage());
                    } else {
                        resultLabel.setStyle("-fx-text-fill: red;");
                        resultLabel.setText("Import issues: " + result.getMessage());
                    }

                    importButton.setDisable(false);
                    selectedFile = null;
                    selectedFileLabel.setText("No file selected");
                    loadHistory();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    importButton.setDisable(false);
                    showError(e.getMessage());
                });
            }
        }).start();
    }

    private void loadHistory() {
        // History table would be populated via backend API call
        // This is a placeholder for the typed endpoint
    }

    private void showError(String msg) {
        importErrorLabel.setText(msg);
    }
}
