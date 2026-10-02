package com.mining.minecom.controller;

import com.mining.minecom.common.dto.UserDto;
import com.mining.minecom.service.AuthService;
import com.mining.minecom.service.WebSocketService;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle; // 🔑 CORRECTION : Importation du type correct

public class LoginController implements Initializable {

    // Services (Déclaration unique)
    private AuthService authService;
    private WebSocketService webSocketService;

    @FXML
    private ImageView icon;
    @FXML private VBox signUpFields;
    @FXML private TextField phoneField;
    @FXML private ImageView profileImageView;

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Button actionButton;
    @FXML private Hyperlink bottomLink;

    // Vos champs existants
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    // État du contrôleur
    private boolean isLoginMode = true;
    // ❌ LIGNE SUPPRIMÉE : private final AuthService authService = new AuthService();

    @Override // 🔑 AJOUTÉ : Bonne pratique pour implémenter Initializable
    public void initialize(URL url, ResourceBundle resourceBundle) { // 🔑 CORRECTION : Type ResourceBundle
        authService = new AuthService();
        // Le WebSocketService dépend d'AuthService pour le JWT
        webSocketService = new WebSocketService(authService);
        icon.setImage(
                new Image(getClass().getResourceAsStream("/images/EDV.png"))
        );
        showLoginMode();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleChoosePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(profileImageView.getScene().getWindow());
        if (file != null) {
            try {
                Image image = new Image(file.toURI().toString());
                profileImageView.setImage(image);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleSwitchMode() {
        if (isLoginMode) {
            showSignUpMode();
        } else {
            showLoginMode();
        }
    }

    public void showLoginMode() {
        isLoginMode = true;
        signUpFields.setVisible(false);
        signUpFields.setManaged(false); // N'occupe pas d'espace
        titleLabel.setText("Connexion");
        subtitleLabel.setText("Accès au système");
        actionButton.setText("Login");
        actionButton.setOnAction(this::handleLogin); // 🔑 CORRECTION : Utilisation de this::handleLogin pour passer l'ActionEvent
        bottomLink.setText("Forgot Password?");
        // Note: Vous pouvez réutiliser cette méthode pour le bouton "Login" du menu
    }

    public void showSignUpMode() {
        isLoginMode = false;
        signUpFields.setVisible(true);
        signUpFields.setManaged(true); // Occupe de l'espace
        titleLabel.setText("Inscription");
        subtitleLabel.setText("Créer un nouveau compte");
        actionButton.setText("Sign Up");
        actionButton.setOnAction(e -> handleSignUp()); // Connecte l'action au Sign Up
        bottomLink.setText("Switch to Login"); // Ou "Retour à la Connexion"
        // Note: Vous pouvez réutiliser cette méthode pour le bouton "Sign Up" du menu
    }

    @FXML
    private void handleAction() {
        if (isLoginMode) {
        } else {
            // handleSignUp();
        }
    }

    // Dans com.mining.minecom.controller.LoginController.java

    @FXML
    private void handleLogin( ActionEvent event) { // 🔑 CORRECTION : Conservation de l'argument ActionEvent
        final String username = usernameField.getText();
        final String password = passwordField.getText();

        // 1. Démarrer l'appel réseau dans un nouveau thread
        new Thread(() -> {
            boolean success = false;
            try {
                // LA NOUVELLE LOGIQUE JWT EST APPELÉE ICI
                success = authService.login(username, password);

                // 2. Retourner au thread de l'interface graphique pour la mise à jour
                if (success) {
                    // *** TEST SUPPLÉMENTAIRE DE L'ACCÈS PROTÉGÉ ***
                    String protectedTestResult = authService.accessProtectedEndpoint();
                    System.out.println("TEST POST-LOGIN: " + protectedTestResult);
                    try {
                        // Utiliser UserService pour récupérer le user par username
                        com.mining.minecom.service.UserService userService = new com.mining.minecom.service.UserService();
                        UserDto currentUser = userService.getUserByUsername(username);

                        if (currentUser != null) {
                            AuthService.setCurrentUsername(currentUser.getUsername());
                            AuthService.setCurrentUserId(currentUser.getId());
                            System.out.println("✅ User stocké : " + currentUser.getUsername() + " (ID: " + currentUser.getId() + ")");
                        }
                    } catch (Exception e) {
                        System.err.println("⚠️ Impossible de récupérer l'utilisateur : " + e.getMessage());
                    }
                    Platform.runLater(() -> {
                        if (protectedTestResult != null && protectedTestResult.startsWith("SUCCÈS")) {
                            System.out.println("Authentification API réussie et token fonctionnel!");
                            redirectToDashboard(); // Lancez votre interface principale
                        } else {
                            // Le token a été obtenu mais a échoué juste après
                            showAlert(Alert.AlertType.ERROR, "Erreur Critique", "Token obtenu mais accès protégé refusé. Vérifiez le serveur.");
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Erreur de Connexion", "Nom d'utilisateur ou mot de passe incorrect.");
                    });
                }

            } catch (Exception e) {
                // Gérer les erreurs de réseau/serveur
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur Serveur", "Impossible de contacter le serveur d'authentification (Spring Boot)."));
                e.printStackTrace();
            }
        }).start();
    }

    // Assurez-vous que cette méthode existe et charge votre FXML principal
    private void redirectToDashboard() {
        try {
            Stage currentStage = (Stage) usernameField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent root = loader.load();

            // 🔑 NOUVELLE LOGIQUE : Récupérer le contrôleur après le chargement FXML
            DashboardController dashboardController = loader.getController();

            // 🔑 NOUVELLE LOGIQUE : Passer le service et Lancer la connexion WebSocket
            if (dashboardController != null) {
                dashboardController.setWebSocketService(webSocketService);

            }

            // Reste du code de mise à jour de la scène...
            Scene dashboardScene = new Scene(root);
            String cssPath = "/css/theme-mining.css";
            URL cssUrl = getClass().getResource(cssPath); // Utiliser URL directement
            currentStage.setScene(dashboardScene);
            currentStage.setTitle("MineCom Dashboard");
            currentStage.show();

            // Badge sur l'icône de la barre des tâches + fonctionnement en arrière-plan
            if (dashboardController != null) {
                dashboardController.attachStage(currentStage);
            }

            if (cssUrl != null) {
                dashboardScene.getStylesheets().add(cssUrl.toExternalForm());
                System.out.println("DEBUG UI: CSS 'theme-mining.css' chargé pour le Dashboard.");
            } else {
                System.err.println("ERREUR CSS: Le fichier de style est introuvable à l'emplacement : " + cssPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur UI", "Impossible de charger le tableau de bord." + e.getMessage());
        }
    }

    private void handleSignUp() {
        final String username = usernameField.getText();
        final String password = passwordField.getText();
        final String phone = phoneField.getText();

        // Utiliser l'opérateur ternaire pour rendre la variable 'effectively final'
        final String profilePictureUrl = (profileImageView.getImage() != null)
                ? profileImageView.getImage().getUrl()
                : null;

        // 1. Démarrer l'appel réseau dans un nouveau thread
        new Thread(() -> {
            try {
                // MODIFIÉ : Passage de profilePictureUrl au service
                boolean success = authService.signUp(username, password, phone, profilePictureUrl);

                // 2. Retourner au thread de l'interface graphique
                Platform.runLater(() -> {
                    if (success) {
                        showAlert(Alert.AlertType.INFORMATION, "Succès", "Inscription réussie ! Veuillez vous connecter.");
                        showLoginMode(); // Revenir au mode Login
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Échec de l'Inscription", "Cet utilisateur existe déjà ou les données sont invalides.");
                    }
                });
            } catch (Exception e) {
                // Gérer les erreurs de réseau/serveur
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur Serveur", "Impossible de contacter le serveur d'authentification (Spring Boot)."));
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) icon.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void minimizeWindow() {
        Stage stage = (Stage) icon.getScene().getWindow();
        stage.setIconified(true);
    }

}