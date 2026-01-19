package com.mining.minecom.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;


import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class NavBarController implements Initializable {

    @FXML private StackPane profileAvatar;
    @FXML private Button sosBtn;
    @FXML private Button messagesBtn;
    @FXML private Button contactsBtn;
    @FXML private Button callsBtn;
    @FXML private Button notificationsBtn;
    @FXML private Button tasksBtn;
    @FXML private Button settingsBtn;
    @FXML private Button profileBtn;

    private String currentUsername;
    private Consumer<Void> sosCallback;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        messagesBtn.getStyleClass().add("nav-button-active");

    }
    public void setSOSCallback(Consumer<Void> callback) {
        this.sosCallback = callback;
    }
    @FXML
    private void handleSOS() {
        System.out.println("🚨 Bouton SOS cliqué !");
        if (sosCallback != null) {
            sosCallback.accept(null);
        }
    }

    // 🔑 MÉTHODE PUBLIQUE POUR DÉFINIR LE NOM D'UTILISATEUR
    public void setCurrentUser(String username) {
        System.out.println("🔵 NavBar reçoit le username : " + username);
        this.currentUsername = username;
        updateProfileAvatar(username);
    }

    // 🔑 CRÉER L'AVATAR AVEC INITIALES
    private void updateProfileAvatar(String username) {
        if (profileAvatar == null) return;

        profileAvatar.getChildren().clear();

        // Cercle de fond
        Circle background = new Circle(18);
        background.setFill(getColorForUser(username));

        // Initiales
        String initials = getInitials(username);
        Label initialsLabel = new Label(initials);
        initialsLabel.setStyle(
                "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px;"
        );

        profileAvatar.getChildren().addAll(background, initialsLabel);
        profileAvatar.setAlignment(Pos.CENTER);
    }

    // Extraire les initiales
    private String getInitials(String username) {
        if (username == null || username.isEmpty()) {
            return "??";
        }

        String[] parts = username.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        } else {
            return username.substring(0, Math.min(2, username.length())).toUpperCase();
        }
    }

    // Couleur basée sur le nom
    private Color getColorForUser(String username) {
        Color[] colors = {
                Color.rgb(255, 107, 107), Color.rgb(78, 205, 196),
                Color.rgb(69, 183, 209), Color.rgb(255, 159, 64),
                Color.rgb(153, 102, 255), Color.rgb(255, 99, 132),
                Color.rgb(54, 162, 235), Color.rgb(255, 206, 86),
        };

        int hash = username.hashCode();
        int index = Math.abs(hash) % colors.length;
        return colors[index];
    }

    @FXML
    private void showMessages() {
        setActiveButton(messagesBtn);
        System.out.println("📨 Afficher les messages");
    }

    @FXML
    private void showContacts() {
        setActiveButton(contactsBtn);
        System.out.println("👥 Afficher les contacts");
    }

    @FXML
    private void showCalls() {
        setActiveButton(callsBtn);
        System.out.println("📞 Afficher les appels");
    }

    @FXML
    private void showNotifications() {
        setActiveButton(notificationsBtn);
        System.out.println("🔔 Afficher les notifications");
    }

    @FXML
    private void showTasks() {
        setActiveButton(tasksBtn);
        System.out.println("✓ Afficher les tâches");
    }

    @FXML
    private void showSettings() {
        setActiveButton(settingsBtn);
        System.out.println("⚙️ Afficher les paramètres");
    }

    @FXML
    private void showProfile() {
        setActiveButton(profileBtn);
        System.out.println("👤 Afficher le profil");
    }

    private void setActiveButton(Button activeBtn) {
        messagesBtn.getStyleClass().remove("nav-button-active");
        contactsBtn.getStyleClass().remove("nav-button-active");
        callsBtn.getStyleClass().remove("nav-button-active");
        notificationsBtn.getStyleClass().remove("nav-button-active");
        tasksBtn.getStyleClass().remove("nav-button-active");
        settingsBtn.getStyleClass().remove("nav-button-active");
        profileBtn.getStyleClass().remove("nav-button-active");

        activeBtn.getStyleClass().add("nav-button-active");
    }
}