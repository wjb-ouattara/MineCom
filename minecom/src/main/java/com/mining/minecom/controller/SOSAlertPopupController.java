package com.mining.minecom.controller;

import com.mining.minecom.common.dto.SOSAlertDto;
import com.mining.minecom.service.SOSAlertService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class SOSAlertPopupController {

    @FXML private StackPane overlay;
    @FXML private Label senderLabel;
    @FXML private Label locationLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label timeLabel;
    @FXML private Label ackCountLabel;

    private SOSAlertDto currentAlert;
    private final SOSAlertService sosAlertService = new SOSAlertService();
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    public void setAlert(SOSAlertDto alert) {
        this.currentAlert = alert;
        updateUI();
    }

    private void updateUI() {
        if (currentAlert != null) {
            Platform.runLater(() -> {
                senderLabel.setText(currentAlert.getSenderUsername());
                locationLabel.setText(currentAlert.getLocation());
                descriptionLabel.setText(
                        currentAlert.getDescription() != null && !currentAlert.getDescription().isEmpty()
                                ? currentAlert.getDescription()
                                : "Aucune description"
                );
                timeLabel.setText(TIME_FORMATTER.format(currentAlert.getTimestamp()));

                updateAckCount();
            });
        }
    }

    public void updateAckCount() {
        if (currentAlert != null) {
            Platform.runLater(() -> {
                int acked = currentAlert.getAcknowledgedCount() != null
                        ? currentAlert.getAcknowledgedCount() : 0;
                int total = currentAlert.getTotalOnlineUsers() != null
                        ? currentAlert.getTotalOnlineUsers() : 0;

                ackCountLabel.setText(acked + " / " + total + " accusés de réception");
            });
        }
    }

    @FXML
    private void handleAcknowledge() {
        if (currentAlert != null) {
            // Envoyer l'accusé de réception
            new Thread(() -> {
                sosAlertService.acknowledgeAlert(currentAlert.getId());

                Platform.runLater(() -> {
                    System.out.println("✅ Accusé de réception envoyé pour alerte ID=" +
                            currentAlert.getId());
                    handleClose();
                });
            }).start();
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) overlay.getScene().getWindow();
        stage.close();
    }
}