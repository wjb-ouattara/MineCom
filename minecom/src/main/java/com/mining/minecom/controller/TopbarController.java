package com.mining.minecom.controller;

import javafx.fxml.FXML;
import com.mining.minecom.common.dto.UserDto;
import com.mining.minecom_server.common.dto.TeamDto;
import com.mining.minecom_server.common.dto.TeamMemberDto;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class TopbarController implements Initializable {
    @FXML private HBox topbar;
    @FXML private StackPane avatarContainer; // 🔑 CHANGEMENT : StackPane au lieu de ImageView
    @FXML private Label userName;
    @FXML private Label lastSeen;
    @FXML private TextField searchField;
    @FXML private MenuItem infoMenuItem;

    private double xOffset = 0;
    private double yOffset = 0;
    private Consumer<String> searchCallback;
    private Runnable infoCallback;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize(URL url, ResourceBundle resourceBundle) {
        topbar.setOnMousePressed(this::mousePressed);
        topbar.setOnMouseDragged(this::mouseDragged);

        userName.setText("Bienvenue");
        String currentTime = LocalTime.now().format(TIME_FORMATTER);
        lastSeen.setText("Dernière connexion : " + currentTime);
        setupSearchField();
    }
    private void setupSearchField() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (searchCallback != null) {
                searchCallback.accept(newValue.toLowerCase().trim());
            }
        });
    }
    public void setSearchCallback(Consumer<String> callback) {
        this.searchCallback = callback;
    }
    /** Action du menu « Infos du contact / de l'équipe ». */
    public void setInfoCallback(Runnable callback) {
        this.infoCallback = callback;
    }

    @FXML
    private void handleInfo() {
        if (infoCallback != null) infoCallback.run();
    }

    public void clearSearch() {
        searchField.clear();
    }

    private void mousePressed(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    private void mouseDragged(MouseEvent event) {
        Stage stage = (Stage) topbar.getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

    // 🔑 NOUVELLE MÉTHODE : Initialiser avec l'utilisateur connecté
    public void setCurrentUser(String username, boolean isOnline) {
        if (username != null) {
            userName.setText(username);
            lastSeen.setText(isOnline ? "En ligne" : "Hors ligne");
            updateAvatar(username);
        }
    }

    // 🔑 MÉTHODE MISE À JOUR : Afficher le contact sélectionné
    public void updateContact(UserDto user) {
        if (user != null) {
            userName.setText(user.getUsername());
            lastSeen.setText(user.getIsOnline() ? "En ligne" : "Hors ligne");
            updateAvatar(user.getUsername());
            if (infoMenuItem != null) infoMenuItem.setText("Infos du contact");
        }
    }

    /** Afficher l'équipe sélectionnée : nom + liste des membres. */
    public void updateTeam(TeamDto team) {
        if (team == null) return;
        userName.setText(team.getName());
        String members = team.getMembers().stream()
                .map(TeamMemberDto::getUsername)
                .limit(5)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        int total = team.getMembers().size();
        lastSeen.setText(total + " membres" + (members.isEmpty() ? "" : " : " + members + (total > 5 ? "…" : "")));
        if (infoMenuItem != null) infoMenuItem.setText("Infos de l'équipe");

        if (avatarContainer != null) {
            avatarContainer.getChildren().clear();
            Circle background = new Circle(20);
            background.setFill(getColorForUser(team.getName()));
            FontIcon groupIcon = new FontIcon("fas-users");
            groupIcon.setIconSize(16);
            groupIcon.setIconColor(Color.WHITE);
            avatarContainer.getChildren().addAll(background, groupIcon);
            avatarContainer.setAlignment(Pos.CENTER);
        }
    }

    // 🔑 NOUVELLE MÉTHODE : Créer l'avatar dynamique
    private void updateAvatar(String username) {
        if (avatarContainer == null) {
            System.err.println("⚠️ avatarContainer est null");
            return;
        }

        avatarContainer.getChildren().clear();

        // Cercle de fond
        Circle background = new Circle(20); // Rayon 20px (40px de diamètre)
        background.setFill(getColorForUser(username));

        // Initiales
        String initials = getInitials(username);
        Label initialsLabel = new Label(initials);
        initialsLabel.setStyle(
                "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px;"
        );

        avatarContainer.getChildren().addAll(background, initialsLabel);
        avatarContainer.setAlignment(Pos.CENTER);
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

    // Couleur basée sur le nom (même logique que NavBarController)
    private Color getColorForUser(String username) {
        if (username == null || username.isEmpty()) {
            username = "User";
        }

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
    private void handleCall() {
        System.out.println("📞 Appel en cours...");
    }
}