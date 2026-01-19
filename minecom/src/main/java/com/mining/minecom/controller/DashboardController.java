package com.mining.minecom.controller;

// DTOs du CLIENT (pas du serveur!)
import com.mining.minecom.common.dto.*;

// Enums du CLIENT
import com.mining.minecom.common.enums.MessageType;
import com.mining.minecom.common.enums.MessageStatus;

// Services du CLIENT
import com.mining.minecom.service.AuthService;
import com.mining.minecom.service.WebSocketService;
import com.mining.minecom.service.UserService;
import com.mining.minecom.service.MessageService;
import com.mining.minecom.service.SOSAlertService;

// Interface du CLIENT
import com.mining.minecom.interfaces.ContactSelectionListener;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.Scene;
import javafx.scene.media.AudioClip;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class DashboardController implements Initializable, WebSocketService.DashboardCallback, ContactSelectionListener {

    @FXML private SidebarController sidebarController;
    @FXML private HBox titleBar;
    @FXML private TopbarController topbarController;
    @FXML private TextArea messageInput;
    @FXML private ComboBox<MessageType> messageTypeSelector;
    @FXML private VBox chatContainer;
    @FXML private ImageView icon;
    @FXML private ImageView bgImage;
    @FXML private StackPane mainArea;
    @FXML private Button maximizeBtn;
    @FXML private StackPane maximizeContainer;

    private boolean websocketReady = false;
    private Popup snapPopup;
    private final UserService userService = new UserService();
    private final MessageService messageService = new MessageService();
    private final SOSAlertService sosAlertService = new SOSAlertService();
    private WebSocketService webSocketService;
    private Long currentReceiverId;
    private Long currentUserId;
    private Stage sosAlertStage;
    private SOSAlertPopupController sosPopupController;
    private AudioClip alarmSound;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (icon != null) {
            icon.setImage(new Image(getClass().getResourceAsStream("/images/EDV.png")));
        }

        Image image = new Image(Objects.requireNonNull(
                getClass().getResource("/images/bg1-mine.jpg")).toExternalForm());
        bgImage.setImage(image);
        bgImage.setPreserveRatio(false);
        bgImage.setManaged(false);
        bgImage.fitWidthProperty().bind(mainArea.widthProperty());
        bgImage.fitHeightProperty().bind(mainArea.heightProperty());

        messageTypeSelector.getItems().addAll(MessageType.values());
        messageTypeSelector.setValue(MessageType.TEXT);

        if (messageInput != null) {
            setupMessageInput();
        }

        if (maximizeBtn != null) {
            setupSnapMenu();
        }

        // 🔑 Gérer la touche Échap pour sortir du fullscreen
        messageInput.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        Stage stage = (Stage) titleBar.getScene().getWindow();
                        if (stage.isFullScreen()) {
                            stage.setFullScreen(false);
                        }
                    }
                });
            }
        });

        // 🔑 Charger le son d'alarme
        try {
            alarmSound = new AudioClip(getClass().getResource("/sounds/alarm.mp3").toExternalForm());
            alarmSound.setVolume(0.8);
        } catch (Exception e) {
            System.err.println("⚠️ Impossible de charger le son d'alarme: " + e.getMessage());
        }
    }

    private void setupSnapMenu() {
        maximizeBtn.setOnMouseEntered(event -> showSnapPopup());
    }

    private void showSnapPopup() {
        if (snapPopup == null) {
            snapPopup = createSnapPopup();
        }

        if (!snapPopup.isShowing()) {
            double x = maximizeBtn.localToScreen(maximizeBtn.getBoundsInLocal()).getMinX();
            double y = maximizeBtn.localToScreen(maximizeBtn.getBoundsInLocal()).getMaxY() + 5;
            snapPopup.show(maximizeBtn, x, y);
        }
    }

    private Popup createSnapPopup() {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        VBox container = new VBox(8);
        container.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);"
        );
        container.setOnMouseEntered(event -> {});

        Label title = new Label("Positionner la fenêtre");
        title.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #666;");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);

        Button fullScreenWithTaskbar = createSnapButton("fas-window-maximize", "Maximiser");
        fullScreenWithTaskbar.setOnAction(e -> {
            snapToMaximized();
            popup.hide();
        });

        Button fullScreenNoTaskbar = createSnapButton("fas-expand", "Plein écran");
        fullScreenNoTaskbar.setOnAction(e -> {
            snapToTrueFullScreen();
            popup.hide();
        });

        Button leftHalf = createSnapButton("fas-align-left", "Moitié gauche");
        leftHalf.setOnAction(e -> {
            snapToLeft();
            popup.hide();
        });

        Button rightHalf = createSnapButton("fas-align-right", "Moitié droite");
        rightHalf.setOnAction(e -> {
            snapToRight();
            popup.hide();
        });

        Button topLeft = createSnapButton("fas-th", "Haut gauche");
        topLeft.setOnAction(e -> {
            snapToTopLeft();
            popup.hide();
        });

        Button topRight = createSnapButton("fas-th", "Haut droit");
        topRight.setOnAction(e -> {
            snapToTopRight();
            popup.hide();
        });

        grid.add(fullScreenWithTaskbar, 0, 0);
        grid.add(fullScreenNoTaskbar, 1, 0);
        grid.add(leftHalf, 0, 1);
        grid.add(rightHalf, 1, 1);
        grid.add(topLeft, 0, 2);
        grid.add(topRight, 1, 2);

        container.getChildren().addAll(title, grid);
        popup.getContent().add(container);

        return popup;
    }

    private Button createSnapButton(String iconLiteral, String tooltipText) {
        Button btn = new Button();

        VBox content = new VBox(5);
        content.setAlignment(Pos.CENTER);

        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(20);
        icon.setIconColor(Color.web("#666"));

        Label label = new Label(tooltipText);
        label.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");

        content.getChildren().addAll(icon, label);
        btn.setGraphic(content);

        btn.setStyle(
                "-fx-background-color: #f5f5f5; " +
                        "-fx-background-radius: 6; " +
                        "-fx-padding: 10; " +
                        "-fx-cursor: hand; " +
                        "-fx-min-width: 100; " +
                        "-fx-pref-width: 100;"
        );

        btn.setOnMouseEntered(e ->
                btn.setStyle(
                        "-fx-background-color: #e0e0e0; " +
                                "-fx-background-radius: 6; " +
                                "-fx-padding: 10; " +
                                "-fx-cursor: hand; " +
                                "-fx-min-width: 100; " +
                                "-fx-pref-width: 100;"
                )
        );

        btn.setOnMouseExited(e ->
                btn.setStyle(
                        "-fx-background-color: #f5f5f5; " +
                                "-fx-background-radius: 6; " +
                                "-fx-padding: 10; " +
                                "-fx-cursor: hand; " +
                                "-fx-min-width: 100; " +
                                "-fx-pref-width: 100;"
                )
        );

        return btn;
    }

    private void snapToMaximized() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        stage.setFullScreen(false);
        stage.setX(screenBounds.getMinX());
        stage.setY(screenBounds.getMinY());
        stage.setWidth(screenBounds.getWidth());
        stage.setHeight(screenBounds.getHeight());

        System.out.println("🖥️ Fenêtre maximisée (avec barre des tâches)");
    }

    private void snapToTrueFullScreen() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.setFullScreen(true);
        System.out.println("🖥️ Fenêtre en plein écran (sans barre des tâches)");
    }

    private void snapToLeft() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        stage.setX(screenBounds.getMinX());
        stage.setY(screenBounds.getMinY());
        stage.setWidth(screenBounds.getWidth() / 2);
        stage.setHeight(screenBounds.getHeight());

        System.out.println("⬅️ Fenêtre à gauche");
    }

    private void snapToRight() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        stage.setX(screenBounds.getMinX() + screenBounds.getWidth() / 2);
        stage.setY(screenBounds.getMinY());
        stage.setWidth(screenBounds.getWidth() / 2);
        stage.setHeight(screenBounds.getHeight());

        System.out.println("➡️ Fenêtre à droite");
    }

    private void snapToTopLeft() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        stage.setX(screenBounds.getMinX());
        stage.setY(screenBounds.getMinY());
        stage.setWidth(screenBounds.getWidth() / 2);
        stage.setHeight(screenBounds.getHeight() / 2);

        System.out.println("↖️ Fenêtre en haut à gauche");
    }

    private void snapToTopRight() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        stage.setX(screenBounds.getMinX() + screenBounds.getWidth() / 2);
        stage.setY(screenBounds.getMinY());
        stage.setWidth(screenBounds.getWidth() / 2);
        stage.setHeight(screenBounds.getHeight() / 2);

        System.out.println("↗️ Fenêtre en haut à droite");
    }

    private void setupMessageInput() {
        messageInput.textProperty().addListener((observable, oldValue, newValue) -> {
            int lineCount = newValue.split("\n").length;
            double newHeight = Math.min(lineCount * 20 + 10, 150);
            messageInput.setPrefHeight(newHeight);
        });

        messageInput.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (!event.isShiftDown() && !event.isControlDown()) {
                    event.consume();
                    handleSendMessage();
                }
            }
        });
    }

    // 🔑 MÉTHODES SOS
    private void showSOSDialog() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation d'alerte SOS");
        confirmAlert.setHeaderText("⚠️ ENVOYER UNE ALERTE SOS ?");
        confirmAlert.setContentText(
                "Cette action va envoyer une alerte d'urgence à TOUS les utilisateurs en ligne.\n\n" +
                        "Voulez-vous vraiment continuer ?"
        );

        ButtonType btnSend = new ButtonType("ENVOYER L'ALERTE", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmAlert.getButtonTypes().setAll(btnSend, btnCancel);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField locationField = new TextField();
        locationField.setPromptText("Ex: Niveau -2, Galerie B");
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Décrivez brièvement la situation...");
        descriptionArea.setPrefRowCount(3);

        grid.add(new Label("Localisation:"), 0, 0);
        grid.add(locationField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionArea, 1, 1);

        confirmAlert.getDialogPane().setExpandableContent(grid);
        confirmAlert.getDialogPane().setExpanded(true);

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == btnSend) {
                String location = locationField.getText().trim();
                String description = descriptionArea.getText().trim();
                sendSOSAlert(location, description);
            }
        });
    }

    private void sendSOSAlert(String location, String description) {
        new Thread(() -> {
            SOSAlertDto alert = sosAlertService.sendSOSAlert(location, description);

            Platform.runLater(() -> {
                if (alert != null) {
                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Alerte envoyée");
                    successAlert.setHeaderText("✅ Alerte SOS envoyée avec succès");
                    successAlert.setContentText(
                            "L'alerte a été diffusée à " + alert.getTotalOnlineUsers() +
                                    " utilisateurs en ligne."
                    );
                    successAlert.show();
                } else {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Erreur");
                    errorAlert.setHeaderText("❌ Échec de l'envoi");
                    errorAlert.setContentText("Impossible d'envoyer l'alerte SOS. Vérifiez votre connexion.");
                    errorAlert.show();
                }
            });
        }).start();
    }

    @Override
    public void onSOSAlertReceived(SOSAlertDto alert) {
        Platform.runLater(() -> {
            try {
                if (alarmSound != null) {
                    alarmSound.play();
                }

                if (sosAlertStage == null) {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/fxml/SOSAlertPopup.fxml")
                    );
                    Scene scene = new Scene(loader.load());
                    scene.getStylesheets().add(
                            getClass().getResource("/css/theme-mining.css").toExternalForm()
                    );

                    sosAlertStage = new Stage();
                    sosAlertStage.initModality(Modality.APPLICATION_MODAL);
                    sosAlertStage.initStyle(StageStyle.UNDECORATED);
                    sosAlertStage.setScene(scene);
                    sosAlertStage.setAlwaysOnTop(true);

                    sosPopupController = loader.getController();
                }

                sosPopupController.setAlert(alert);
                sosAlertStage.show();
                sosAlertStage.toFront();

                System.out.println("🚨 ALERTE SOS REÇUE de " + alert.getSenderUsername());

            } catch (Exception e) {
                System.err.println("Erreur lors de l'affichage de l'alerte SOS: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onSOSAcknowledgment(SOSAcknowledgmentDto ack) {
        Platform.runLater(() -> {
            System.out.println("✅ Accusé reçu de : " + ack.getUsername());

            if (sosPopupController != null) {
                sosPopupController.updateAckCount();
            }
        });
    }

    @Override
    public void onConnectedSuccess() {
        Platform.runLater(() -> {
            messageInput.setDisable(false);
            websocketReady = true;
        });
    }

    public void setWebSocketService(WebSocketService service) {
        this.webSocketService = service;
        this.webSocketService.setCallback(this);

        this.currentUserId = AuthService.getCurrentUserId();
        String currentUsername = AuthService.getCurrentUsername();

        if (currentUserId == null || currentUsername == null) {
            System.err.println("❌ Impossible de récupérer l'utilisateur depuis AuthService");
            return;
        }

        System.out.println("✅ Utilisateur connecté : " + currentUsername + " (ID: " + currentUserId + ")");

        if (sidebarController != null) {
            UserListController listController = sidebarController.getUserListController();
            if (listController != null) {
                listController.setSelectionListener(this);
            }

            NavBarController navBarController = sidebarController.getNavBarController();
            if (navBarController != null) {
                navBarController.setCurrentUser(currentUsername);
                System.out.println("✅ Avatar NavBar mis à jour pour : " + currentUsername);

                // 🔑 Configurer le callback SOS
                navBarController.setSOSCallback(v -> showSOSDialog());
            }
        }

        if (topbarController != null) {
            topbarController.setCurrentUser(currentUsername, true);
            System.out.println("✅ Avatar Topbar initialisé pour : " + currentUsername);
            topbarController.setSearchCallback(this::searchInMessages);
        }

        loadInitialUserList();
        Platform.runLater(() -> {
            System.out.println("Dashboard: Tentative de connexion WebSocket...");
            this.webSocketService.connect();
        });
    }

    private void searchInMessages(String searchText) {
        if (chatContainer == null) return;

        if (searchText.isEmpty()) {
            chatContainer.getChildren().forEach(node -> node.setVisible(true));
            chatContainer.getChildren().forEach(node -> node.setManaged(true));
        } else {
            chatContainer.getChildren().forEach(node -> {
                if (node instanceof VBox) {
                    VBox bubbleContainer = (VBox) node;
                    boolean containsSearch = bubbleContainer.getChildren().stream()
                            .anyMatch(child -> {
                                if (child instanceof HBox) {
                                    HBox hbox = (HBox) child;
                                    return hbox.getChildren().stream()
                                            .anyMatch(subChild -> {
                                                if (subChild instanceof VBox) {
                                                    VBox vbox = (VBox) subChild;
                                                    return vbox.getChildren().stream()
                                                            .anyMatch(label -> {
                                                                if (label instanceof Label) {
                                                                    String text = ((Label) label).getText();
                                                                    return text != null &&
                                                                            text.toLowerCase().contains(searchText);
                                                                }
                                                                return false;
                                                            });
                                                }
                                                return false;
                                            });
                                }
                                return false;
                            });

                    node.setVisible(containsSearch);
                    node.setManaged(containsSearch);
                }
            });
        }
    }

    @Override
    public void onContactSelected(UserDto user) {
        System.out.println("Dashboard Hub: Utilisateur sélectionné ID=" + user.getId() + " - " + user.getUsername());
        setCurrentReceiver(user.getId());

        if (topbarController != null) {
            topbarController.updateContact(user);
        }
    }

    private void loadInitialUserList() {
        new Thread(() -> {
            List<ConversationPreviewDto> conversations = userService.getConversations();

            Platform.runLater(() -> {
                UserListController listController = null;
                if (sidebarController != null) {
                    listController = sidebarController.getUserListController();
                }
                if (listController != null) {
                    listController.updateConversations(conversations);
                    System.out.println("UI CHARGEMENT: " + conversations.size() + " conversations chargées.");
                }
            });
        }).start();
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void minimizeWindow() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void maximizeWindow() {
        Stage stage = (Stage) titleBar.getScene().getWindow();

        if (stage.isFullScreen()) {
            stage.setFullScreen(false);
            stage.setWidth(1400);
            stage.setHeight(800);
            stage.centerOnScreen();
            System.out.println("↩️ Fenêtre restaurée (taille normale)");
        } else if (isMaximized(stage)) {
            stage.setWidth(1400);
            stage.setHeight(800);
            stage.centerOnScreen();
            System.out.println("↩️ Fenêtre restaurée (taille normale)");
        } else {
            snapToMaximized();
        }
    }

    private boolean isMaximized(Stage stage) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        return Math.abs(stage.getX() - screenBounds.getMinX()) < 10 &&
                Math.abs(stage.getY() - screenBounds.getMinY()) < 10 &&
                Math.abs(stage.getWidth() - screenBounds.getWidth()) < 10 &&
                Math.abs(stage.getHeight() - screenBounds.getHeight()) < 10;
    }

    public void setCurrentReceiver(Long receiverId) {
        this.currentReceiverId = receiverId;

        if (chatContainer != null) {
            chatContainer.getChildren().clear();
            addSystemMessage("Chargement de la conversation...");

            new Thread(() -> {
                List<MessageResponse> history = messageService.getConversationHistory(receiverId);

                Platform.runLater(() -> {
                    chatContainer.getChildren().clear();

                    if (history.isEmpty()) {
                        addSystemMessage("Aucun message avec cet utilisateur");
                    } else {
                        for (MessageResponse msg : history) {
                            boolean isSent = msg.getSenderId().equals(currentUserId);
                            addMessageBubble(msg, isSent);
                        }
                        System.out.println("✅ " + history.size() + " messages chargés");
                    }
                });
            }).start();
        }
    }

    @FXML
    private void handleSendMessage() {
        String content = messageInput.getText().trim();

        if (content.isEmpty()) {
            return;
        }
        if (currentReceiverId == null) {
            return;
        }

        MessageType selectedType = messageTypeSelector.getValue();
        MessageRequest request = new MessageRequest(currentReceiverId, content, selectedType);

        if (websocketReady) {
            webSocketService.sendMessage(request);

            MessageResponse tempMsg = new MessageResponse();
            tempMsg.setContent(content);
            tempMsg.setMessageType(selectedType);
            tempMsg.setTimestamp(Instant.now());
            tempMsg.setStatus(MessageStatus.SENT);
            tempMsg.setSenderId(currentUserId);

            addMessageBubble(tempMsg, true);
            messageInput.clear();
        }
    }

    private void addMessageBubble(MessageResponse msg, boolean isSent) {
        VBox bubbleContainer = new VBox(3);
        bubbleContainer.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        if (msg.getMessageType() != MessageType.TEXT) {
            Label typeBadge = new Label(msg.getMessageType().toString());
            typeBadge.setStyle(
                    "-fx-background-color: rgba(0,0,0,0.6); " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 9px; " +
                            "-fx-padding: 2 6 2 6; " +
                            "-fx-background-radius: 8;"
            );
            HBox badgeBox = new HBox(typeBadge);
            badgeBox.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            badgeBox.setPadding(new Insets(0, 5, 2, 5));
            bubbleContainer.getChildren().add(badgeBox);
        }

        Label messageLabel = new Label(msg.getContent());
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(500);
        messageLabel.setPrefWidth(Region.USE_COMPUTED_SIZE);
        messageLabel.setPadding(new Insets(10, 15, 10, 15));
        messageLabel.setStyle(getMessageStyle(msg.getMessageType(), isSent));

        HBox metaBox = new HBox(5);
        metaBox.setAlignment(Pos.CENTER_RIGHT);

        String timeStr = TIME_FORMATTER.format(msg.getTimestamp());
        Label timeLabel = new Label(timeStr);
        timeLabel.setStyle("-fx-text-fill: rgba(0,0,0,0.7); -fx-font-size: 10px;");
        timeLabel.setAlignment(Pos.CENTER_RIGHT);
        metaBox.getChildren().add(timeLabel);

        if (isSent) {
            FontIcon statusIcon = new FontIcon();
            if (msg.getStatus() == MessageStatus.READ) {
                statusIcon.setIconLiteral("fas-check-double");
                statusIcon.setIconColor(Color.rgb(0, 200, 255));
            } else {
                statusIcon.setIconLiteral("fas-check");
                statusIcon.setIconColor(Color.rgb(200, 200, 200));
            }
            statusIcon.setIconSize(10);
            metaBox.getChildren().add(statusIcon);
        }

        VBox messageContent = new VBox(1, new Label(msg.getContent()), metaBox);
        messageContent.setPadding(new Insets(10, 15, 8, 15));
        messageContent.setStyle(getMessageStyle(msg.getMessageType(), isSent));
        messageContent.setMaxWidth(500);

        HBox messageBubble = new HBox(messageContent);
        messageBubble.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        bubbleContainer.getChildren().add(messageBubble);
        chatContainer.getChildren().add(bubbleContainer);
    }

    private void addSystemMessage(String content) {
        Label systemLabel = new Label(content);
        systemLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11px; -fx-font-style: italic;");
        HBox systemBox = new HBox(systemLabel);
        systemBox.setAlignment(Pos.CENTER);
        systemBox.setPadding(new Insets(5, 0, 5, 0));
        chatContainer.getChildren().add(systemBox);
    }

    private String getMessageStyle(MessageType type, boolean isSent) {
        String baseStyle = isSent ? "-fx-background-radius: 20 0 20 20; -fx-font-size: 13px; -fx-text-fill: white;" :
                "-fx-background-radius: 0 20 20 20; -fx-font-size: 13px; -fx-text-fill: white;";

        String bgColor = switch (type) {
            case TEXT -> "rgba(255, 255, 255)";
            case INCIDENT -> "rgba(255, 68, 68, 0.8)";
            case URGENT -> "rgba(255, 102, 0, 0.8)";
            case ALERTE -> "rgba(204, 0, 0, 0.8)";
            case MAINTENANCE -> "rgba(255, 170, 0, 0.8)";
            case SECURITE -> "rgba(255, 153, 0, 0.8)";
            case IMAGE -> "rgba(156, 39, 176, 0.8)";
            case FILE -> "rgba(96, 125, 139, 0.8)";
            case DEMANDE_DE_SUPPORT -> "rgba(76, 175, 80, 0.8)";
        };

        return baseStyle + " -fx-background-color: " + bgColor + ";";
    }

    @Override
    public void onMessageReceived(MessageResponse message) {
        Platform.runLater(() -> {
            if (message.getSenderId().equals(currentReceiverId)) {
                addMessageBubble(message, false);
            }
        });
    }

    @Override
    public void onPresenceUpdate(PresenceUpdate update) {
        Platform.runLater(() -> {
            UserListController listController = null;
            if (sidebarController != null) {
                listController = sidebarController.getUserListController();
            }
            if (listController != null) {
                UserDto userUpdate = new UserDto(update.getUserId(), update.getUsername(), update.isOnline());
                listController.updateOrCreateUser(userUpdate);
            }
        });
    }
}