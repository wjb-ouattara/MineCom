package com.mining.minecom.controller;

import com.mining.minecom.common.enums.MessageType;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;

public class NavBarController implements Initializable {

    // SOS
    @FXML private StackPane sosBadgeContainer;
    @FXML private Circle sosCircle;
    @FXML private Button sosBtn;

    // Navigation
    @FXML private Button messagesBtn;
    @FXML private Button contactsBtn;
    @FXML private Button callsBtn;
    @FXML private Button notificationsBtn;
    @FXML private Button tasksBtn;
    @FXML private Button settingsBtn;
    @FXML private Button profileBtn;

    // Avatar
    @FXML private StackPane profileAvatar;

    // 🔑 Badge global (défini en FXML)
    @FXML private StackPane globalBadgePane;
    @FXML private Circle globalBadgeCircle;
    @FXML private Label globalBadgeText;

    private String currentUsername;
    private Consumer<Void> sosCallback;
    private Consumer<Void> sosBadgeClickCallback;
    private Timeline pulseAnimation;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        messagesBtn.getStyleClass().add("nav-button-active");

        // Tooltip SOS badge
        if (sosBadgeContainer != null) {
            Tooltip.install(sosBadgeContainer, new Tooltip("⚠️ ALERTE SOS ACTIVE - Cliquez pour voir"));
        }

        System.out.println("✅ NavBarController initialisé - badge: " + (globalBadgePane != null ? "OK" : "NULL"));
    }

    // ================================================================
    // 🔑 BADGE GLOBAL DE NOTIFICATIONS
    // ================================================================

    public void updateGlobalNotifBadge(int totalCount, List<MessageType> allTypes) {
        if (globalBadgePane == null) {
            System.err.println("❌ globalBadgePane est null !");
            return;
        }

        if (totalCount <= 0) {
            globalBadgePane.setVisible(false);
            globalBadgePane.setManaged(false);
            System.out.println("🔕 Badge global masqué");
            return;
        }

        Set<MessageType> uniqueTypes = new HashSet<>(allTypes);
        Color badgeColor = UserListController.calculateBadgeColor(uniqueTypes);

        globalBadgeCircle.setFill(badgeColor);
        globalBadgeText.setText(totalCount > 99 ? "99+" : String.valueOf(totalCount));
        // managed=true : sinon le StackPane parent ne le positionne pas (taille 0, coin haut-gauche)
        globalBadgePane.setManaged(true);
        globalBadgePane.setVisible(true);
        globalBadgePane.toFront();

        System.out.println("🔔 Badge global: " + totalCount + " messages non lus");
    }

    public void hideGlobalNotifBadge() {
        if (globalBadgePane != null) {
            globalBadgePane.setVisible(false);
            globalBadgePane.setManaged(false);
        }
    }

    // ================================================================
    // SOS BADGE
    // ================================================================

    public void showSOSBadge() {
        if (sosBadgeContainer == null) return;
        sosBadgeContainer.setVisible(true);
        sosBadgeContainer.setManaged(true);
        startPulseAnimation();
        System.out.println("🔴 Badge SOS affiché");
    }

    public void hideSOSBadge() {
        if (sosBadgeContainer == null) return;
        sosBadgeContainer.setVisible(false);
        sosBadgeContainer.setManaged(false);
        stopPulseAnimation();
        System.out.println("✅ Badge SOS masqué");
    }

    private void startPulseAnimation() {
        if (sosCircle == null) return;
        stopPulseAnimation();
        pulseAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(sosCircle.scaleXProperty(), 1.0),
                        new KeyValue(sosCircle.scaleYProperty(), 1.0),
                        new KeyValue(sosCircle.opacityProperty(), 1.0)
                ),
                new KeyFrame(Duration.millis(600),
                        new KeyValue(sosCircle.scaleXProperty(), 1.2),
                        new KeyValue(sosCircle.scaleYProperty(), 1.2),
                        new KeyValue(sosCircle.opacityProperty(), 0.7)
                ),
                new KeyFrame(Duration.millis(1200),
                        new KeyValue(sosCircle.scaleXProperty(), 1.0),
                        new KeyValue(sosCircle.scaleYProperty(), 1.0),
                        new KeyValue(sosCircle.opacityProperty(), 1.0)
                )
        );
        pulseAnimation.setCycleCount(Timeline.INDEFINITE);
        pulseAnimation.play();
    }

    private void stopPulseAnimation() {
        if (pulseAnimation != null) {
            pulseAnimation.stop();
            pulseAnimation = null;
            if (sosCircle != null) {
                sosCircle.setScaleX(1.0);
                sosCircle.setScaleY(1.0);
                sosCircle.setOpacity(1.0);
            }
        }
    }

    public void setSOSBadgeClickCallback(Consumer<Void> callback) {
        this.sosBadgeClickCallback = callback;
    }

    @FXML
    private void handleSOSBadgeClick() {
        if (sosBadgeClickCallback != null) sosBadgeClickCallback.accept(null);
    }

    @FXML
    private void handleSOS() {
        if (sosCallback != null) sosCallback.accept(null);
    }

    public void setSOSCallback(Consumer<Void> callback) {
        this.sosCallback = callback;
    }

    // ================================================================
    // AVATAR
    // ================================================================

    public void setCurrentUser(String username) {
        System.out.println("🔵 NavBar reçoit le username : " + username);
        this.currentUsername = username;
        updateProfileAvatar(username);
    }

    private void updateProfileAvatar(String username) {
        if (profileAvatar == null) return;
        if (username == null || username.isEmpty()) username = "User";

        profileAvatar.getChildren().clear();

        Circle background = new Circle(18);
        background.setFill(getColorForUser(username));

        Label initialsLabel = new Label(getInitials(username));
        initialsLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        profileAvatar.getChildren().addAll(background, initialsLabel);
        profileAvatar.setAlignment(Pos.CENTER);
    }

    private String getInitials(String username) {
        if (username == null || username.isEmpty()) return "??";
        String[] parts = username.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return username.substring(0, Math.min(2, username.length())).toUpperCase();
    }

    private Color getColorForUser(String username) {
        if (username == null || username.isEmpty()) username = "User";
        Color[] colors = {
                Color.rgb(255, 107, 107), Color.rgb(78, 205, 196),
                Color.rgb(69, 183, 209), Color.rgb(255, 159, 64),
                Color.rgb(153, 102, 255), Color.rgb(255, 99, 132),
                Color.rgb(54, 162, 235), Color.rgb(255, 206, 86),
        };
        return colors[Math.abs(username.hashCode()) % colors.length];
    }

    // ================================================================
    // NAVIGATION
    // ================================================================

    @FXML private void showMessages()     { setActiveButton(messagesBtn); }
    @FXML private void showContacts()     { setActiveButton(contactsBtn); }
    @FXML private void showCalls()        { setActiveButton(callsBtn); }
    @FXML private void showNotifications(){ setActiveButton(notificationsBtn); }
    @FXML private void showTasks()        { setActiveButton(tasksBtn); }
    @FXML private void showSettings()     { setActiveButton(settingsBtn); }
    @FXML private void showProfile()      { setActiveButton(profileBtn); }

    private void setActiveButton(Button activeBtn) {
        for (Button btn : new Button[]{messagesBtn, contactsBtn, callsBtn,
                notificationsBtn, tasksBtn, settingsBtn, profileBtn}) {
            btn.getStyleClass().remove("nav-button-active");
        }
        activeBtn.getStyleClass().add("nav-button-active");
    }
}