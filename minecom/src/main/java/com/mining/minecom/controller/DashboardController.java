package com.mining.minecom.controller;

import com.mining.minecom.common.dto.*;
import com.mining.minecom.common.enums.MessageType;
import com.mining.minecom.common.enums.MessageStatus;
import com.mining.minecom.service.*;
import com.mining.minecom.interfaces.ContactSelectionListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mining.minecom_server.common.dto.MessageReadNotification;
import com.mining.minecom_server.common.dto.TeamDto;
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
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.sound.sampled.*;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;
import java.io.File;
import java.nio.file.Files;
import java.io.ByteArrayInputStream;

public class DashboardController implements Initializable, WebSocketService.DashboardCallback, ContactSelectionListener {

    // ================================================================
    // FXML
    // ================================================================
    @FXML
    private SidebarController sidebarController;
    @FXML
    private HBox titleBar;
    @FXML
    private TopbarController topbarController;
    @FXML
    private TextArea messageInput;
    @FXML
    private ComboBox<MessageType> messageTypeSelector;
    @FXML
    private VBox chatContainer;
    @FXML
    private ScrollPane chatScrollPane; // 🔑 AJOUT
    @FXML
    private ImageView icon;
    @FXML
    private ImageView bgImage;
    @FXML
    private StackPane mainArea;
    @FXML
    private Button maximizeBtn;
    @FXML
    private StackPane maximizeContainer;
    @FXML
    private HBox replyPreviewBar;
    @FXML
    private Label replyToLabel;
    @FXML
    private Label replyPreviewText;

    @FXML
    private void handleSendImage() {
        if (currentReceiverId == null && currentTeam == null) {
            showAlert("⚠️ Sélectionnez d'abord un contact.");
            return;
        }
        File file = fileService.openImageChooser(chatContainer.getScene().getWindow());
        if (file != null) {
            uploadAndSendFile(file, MessageType.IMAGE);
        }
    }

    @FXML
    private void handleSendPdf() {
        if (currentReceiverId == null && currentTeam == null) {
            showAlert("⚠️ Sélectionnez d'abord un contact.");
            return;
        }
        File file = fileService.openPdfChooser(chatContainer.getScene().getWindow());
        if (file != null) {
            uploadAndSendFile(file, MessageType.FILE);
        }
    }

    // ================================================================
    // CHAMPS
    // ================================================================
    private boolean websocketReady = false;
    private Popup snapPopup;
    private final UserService userService = new UserService();
    private final MessageService messageService = new MessageService();
    private final SOSAlertService sosAlertService = new SOSAlertService();
    private WebSocketService webSocketService;
    private Long currentReceiverId;
    // Équipe ouverte (exclusif avec currentReceiverId)
    private TeamDto currentTeam;
    private final TeamService teamService = new TeamService();
    private Long currentUserId;
    private Stage sosAlertStage;
    private SOSAlertPopupController sosPopupController;
    private SOSAlertDto currentActiveAlert;
    private final Set<Long> acknowledgedAlertIds = new HashSet<>();
    // Message en cours de réponse / édition
    private MessageResponse currentReplyTo = null;
    private Long currentEditMessageId = null;
    private Popup currentActionPopup = null;
    private final FileService fileService = new FileService();
    // Badge barre des tâches + zone de notification + notifications Windows
    private DesktopNotificationService desktopNotifier;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    // ================================================================
    // INITIALIZE
    // ================================================================
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

        if (messageInput != null) setupMessageInput();
        if (maximizeBtn != null) setupSnapMenu();

        messageInput.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        Stage stage = (Stage) titleBar.getScene().getWindow();
                        if (stage.isFullScreen()) stage.setFullScreen(false);
                    }
                });
            }
        });
    }

    // ================================================================
    // WEBSOCKET CONNEXION
    // ================================================================
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
            if (listController != null) listController.setSelectionListener(this);

            NavBarController navBarController = sidebarController.getNavBarController();
            if (navBarController != null) {
                navBarController.setCurrentUser(currentUsername);
                System.out.println("✅ Avatar NavBar mis à jour pour : " + currentUsername);
                navBarController.setSOSCallback(v -> showSOSDialog());
                navBarController.setSOSBadgeClickCallback(v -> showSOSDetails());
            }
        }

        if (topbarController != null) {
            topbarController.setCurrentUser(currentUsername, true);
            System.out.println("✅ Avatar Topbar initialisé pour : " + currentUsername);
            topbarController.setSearchCallback(this::searchInMessages);
            topbarController.setInfoCallback(this::showCurrentTeamInfo);
        }

        loadInitialUserList();

        Platform.runLater(() -> {
            System.out.println("Dashboard: Tentative de connexion WebSocket...");
            this.webSocketService.connect();
        });

        new Thread(() -> {
            try {
                Thread.sleep(2000);
                checkActiveSOSAlert();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ================================================================
    //  MESSAGE REÇU — FIX PRINCIPAL
    // ================================================================
    @Override
    public void onMessageReceived(MessageResponse message) {
        Platform.runLater(() -> {
            System.out.println("📩 Message reçu de senderId=" + message.getSenderId()
                    + " | currentReceiver=" + currentReceiverId
                    + " | type=" + message.getMessageType()
                    + (message.getFileUrl() != null ? " | fichier=" + message.getFileName() : ""));

            if (message.getSenderId() == null) return;

            // Aperçu du dernier message dans la liste des contacts
            if (sidebarController != null && sidebarController.getUserListController() != null) {
                sidebarController.getUserListController().updateLastMessage(
                        message.getSenderId(), previewText(message), message.getTimestamp(), false);
            }

            MessageType type = message.getMessageType() != null ? message.getMessageType() : MessageType.TEXT;
            boolean conversationOpen = currentReceiverId != null && message.getSenderId().equals(currentReceiverId);
            // Fenêtre fermée (zone de notification), réduite ou en arrière-plan → l'utilisateur ne voit rien
            boolean windowActive = desktopNotifier == null || desktopNotifier.isWindowActive();

            if (conversationOpen) {
                // Conversation ouverte → afficher immédiatement
                addMessageBubble(message, false);
                scrollToBottom();
            }

            if (conversationOpen && windowActive) {
                markMessagesAsRead(currentReceiverId);
            } else {
                // Non vu → badges (contact, NavBar, barre des tâches). Si la conversation est
                // ouverte mais la fenêtre inactive, onWindowActivated() les effacera au retour.
                System.out.println("Notification pour senderId=" + message.getSenderId());
                trackUnreadMessage(message.getSenderId(), type);
            }
            moveContactToTop(message.getSenderId());

            if (!windowActive) notifyDesktop(message, type);
        });
    }

    /** Notification Windows (toast) pour un message reçu hors de la vue de l'utilisateur. */
    private void notifyDesktop(MessageResponse message, MessageType type) {
        if (desktopNotifier == null) return;
        String sender = null;
        if (sidebarController != null && sidebarController.getUserListController() != null) {
            sender = sidebarController.getUserListController().getUsername(message.getSenderId());
        }
        String title = (type != MessageType.TEXT ? "[" + type + "] " : "")
                + (sender != null ? sender : "Nouveau message");
        desktopNotifier.notify(title, truncateText(previewText(message), 120), type);
    }

    /** Texte court affiché dans la liste des contacts pour un message. */
    private String previewText(MessageResponse msg) {
        if (isImageAttachment(msg)) return "📷 Photo";
        if (isPdfAttachment(msg)) return "📄 " + (msg.getFileName() != null ? msg.getFileName() : "Document PDF");
        return msg.getContent() != null ? msg.getContent() : "";
    }

    // ================================================================
    //  OUVRIR UNE CONVERSATION
    // ================================================================
    public void setCurrentReceiver(Long receiverId) {
        this.currentReceiverId = receiverId;
        this.currentTeam = null;

        // Effacer les non lus de ce contact
        if (sidebarController != null) {
            UserListController lc = sidebarController.getUserListController();
            if (lc != null) {
                lc.clearUnread(receiverId);
                updateGlobalBadge();
            }
        }

        // Marquer comme lus sur le serveur
        markMessagesAsRead(receiverId);

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
                            addMessageBubble(msg, msg.getSenderId().equals(currentUserId));
                        }
                        System.out.println("✅ " + history.size() + " messages chargés");
                    }

                    // 🔑 Scroll vers le bas APRÈS chargement
                    scrollToBottom();
                });
            }).start();
        }
    }

    // ================================================================
    //  MÉTHODES DE NOTIFICATION
    // ================================================================

    private void trackUnreadMessage(Long senderId, MessageType type) {
        if (sidebarController == null) return;
        UserListController lc = sidebarController.getUserListController();
        if (lc == null) return;
        lc.addUnreadMessage(senderId, type);
        updateGlobalBadge();
    }

    private void updateGlobalBadge() {
        if (sidebarController == null) return;
        UserListController lc = sidebarController.getUserListController();
        if (lc == null) return;

        int total = lc.getTotalUnreadCount();
        List<MessageType> allTypes = lc.getAllUnreadTypes();
        System.out.println("📊 Badge global: " + total + " messages non lus");

        NavBarController nav = sidebarController.getNavBarController();
        if (nav != null) nav.updateGlobalNotifBadge(total, allTypes);

        // Icône de la barre des tâches / zone de notification : couleur du message le plus grave
        if (desktopNotifier != null) {
            MessageType top = UserListController.mostUrgent(allTypes);
            String tooltip = total > 0
                    ? "MineCom – " + total + " message(s) non lu(s)"
                            + (top != null && top != MessageType.TEXT ? " • " + top : "")
                    : "MineCom";
            desktopNotifier.updateBadge(total, top != null ? UserListController.getColorForType(top) : null, tooltip);
        }
    }

    /**
     * Appelé par LoginController une fois la scène du dashboard affichée.
     * Active le badge sur l'icône et le fonctionnement en arrière-plan (comme Telegram/WhatsApp).
     */
    public void attachStage(Stage stage) {
        desktopNotifier = new DesktopNotificationService(stage, this::quitApplication);

        if (desktopNotifier.isTraySupported()) {
            // Fermer la fenêtre ne quitte plus l'application : elle reste dans la zone de notification
            Platform.setImplicitExit(false);
            stage.setOnCloseRequest(e -> {   // Alt+F4 / fermeture par le système
                e.consume();
                desktopNotifier.hideToTray();
            });
        }

        stage.focusedProperty().addListener((obs, was, focused) -> {
            if (focused) onWindowActivated();
        });
        updateGlobalBadge();
    }

    /** Retour sur la fenêtre : les messages arrivés dans la conversation ouverte sont maintenant vus. */
    private void onWindowActivated() {
        if (sidebarController == null) return;
        UserListController lc = sidebarController.getUserListController();
        if (currentTeam != null) {
            if (lc != null && lc.hasTeamUnread(currentTeam.getId())) {
                lc.clearTeamUnread(currentTeam.getId());
                updateGlobalBadge();
            }
            return;
        }
        if (currentReceiverId == null) return;
        if (lc != null && lc.hasUnread(currentReceiverId)) {
            lc.clearUnread(currentReceiverId);
            updateGlobalBadge();
            markMessagesAsRead(currentReceiverId);
        }
    }

    /** Quitter vraiment (menu « Quitter » de l'icône de notification). */
    private void quitApplication() {
        System.out.println("👋 Fermeture de MineCom...");
        if (desktopNotifier != null) desktopNotifier.dispose();
        new Thread(() -> {
            try {
                if (webSocketService != null) webSocketService.disconnect();
                new AuthService().logout();
            } finally {
                Platform.exit();
                System.exit(0);
            }
        }, "minecom-quit").start();
    }

    private void moveContactToTop(Long userId) {
        if (sidebarController == null) return;
        UserListController lc = sidebarController.getUserListController();
        if (lc != null) lc.moveToTop(userId);
    }

    private void markMessagesAsRead(Long senderId) {
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:8080/api/messages/read/" + senderId))
                        .header("Authorization", "Bearer " + AuthService.getCurrentJwtToken())
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("✅ Messages de " + senderId + " marqués comme lus - Status: " + response.statusCode());
            } catch (Exception e) {
                System.err.println("Erreur markAsRead: " + e.getMessage());
            }
        }).start();
    }

    // ================================================================
    // 🔑 SCROLL VERS LE BAS
    // ================================================================
    private void scrollToBottom() {
        Platform.runLater(() -> {
            if (chatScrollPane != null) {
                chatScrollPane.applyCss();
                chatScrollPane.layout();
                chatScrollPane.setVvalue(1.0);
            }
        });
    }

    // ================================================================
    // READ RECEIPTS
    // ================================================================
    @Override
    public void onMessagesRead(MessageReadNotification notif) {
        Platform.runLater(() -> {
            if (currentReceiverId == null || !currentReceiverId.equals(notif.getReaderId())) return;
            System.out.println("👁️ " + notif.getReaderUsername() + " a lu vos messages");
            // Mettre à jour toutes les icônes de statut → bleu
            chatContainer.getChildren().forEach(node -> {
                if (node instanceof VBox bubbleContainer) {
                    bubbleContainer.getChildren().forEach(child -> {
                        if (child instanceof HBox hbox) {
                            hbox.getChildren().forEach(sub -> {
                                if (sub instanceof VBox vbox) {
                                    updateAllStatusIcons(vbox);
                                }
                            });
                        }
                    });
                }
            });
        });
    }

    private void updateAllStatusIcons(VBox messageContent) {
        messageContent.getChildren().forEach(child -> {
            if (child instanceof HBox metaBox) {
                metaBox.getChildren().forEach(item -> {
                    if (item instanceof FontIcon icon) {
                        icon.setIconLiteral("fas-check-double");
                        icon.setIconColor(Color.rgb(0, 200, 255));
                    }
                });
            }
        });
    }

    // ================================================================
    // 👥 ÉQUIPES
    // ================================================================
    @Override
    public void onTeamSelected(TeamDto team) {
        System.out.println("Dashboard Hub: Équipe sélectionnée ID=" + team.getId() + " - " + team.getName());
        currentReceiverId = null;
        currentTeam = team;
        cancelReply();

        UserListController lc = sidebarController != null ? sidebarController.getUserListController() : null;
        if (lc != null) {
            lc.clearTeamUnread(team.getId());
            updateGlobalBadge();
        }
        if (topbarController != null) topbarController.updateTeam(team);

        chatContainer.getChildren().clear();
        addSystemMessage("Chargement de l'équipe...");
        final Long teamId = team.getId();

        new Thread(() -> {
            List<MessageResponse> history = messageService.getTeamHistory(teamId);
            Platform.runLater(() -> {
                // L'utilisateur a pu ouvrir une autre conversation entre-temps
                if (currentTeam == null || !currentTeam.getId().equals(teamId)) return;
                chatContainer.getChildren().clear();
                addSystemMessage("👥 Équipe « " + team.getName() + " » – messages visibles uniquement par ses "
                        + team.getMembers().size() + " membres");
                for (MessageResponse msg : history) {
                    addMessageBubble(msg, msg.getSenderId().equals(currentUserId));
                }
                scrollToBottom();
            });
        }, "team-history").start();
    }

    @Override
    public void onTeamLeft(Long teamId) {
        closeTeamIfOpen(teamId, "Vous avez quitté cette équipe.");
    }

    @Override
    public void onTeamMessageReceived(MessageResponse message) {
        Platform.runLater(() -> {
            Long teamId = message.getTeamId();
            if (teamId == null || message.getSenderId() == null) return;
            UserListController lc = sidebarController != null ? sidebarController.getUserListController() : null;

            if (lc != null) {
                lc.updateTeamLastMessage(teamId, message.getSenderUsername() + ": " + previewText(message),
                        message.getTimestamp(), false);
                lc.moveTeamToTop(teamId);
            }

            MessageType type = message.getMessageType() != null ? message.getMessageType() : MessageType.TEXT;
            boolean teamOpen = currentTeam != null && teamId.equals(currentTeam.getId());
            boolean windowActive = desktopNotifier == null || desktopNotifier.isWindowActive();

            if (teamOpen) {
                addMessageBubble(message, false);
                scrollToBottom();
            }
            if (!(teamOpen && windowActive) && lc != null) {
                lc.addUnreadTeamMessage(teamId, type);
                updateGlobalBadge();
            }

            if (!windowActive && desktopNotifier != null) {
                TeamDto team = lc != null ? lc.getTeam(teamId) : null;
                String title = (type != MessageType.TEXT ? "[" + type + "] " : "")
                        + (team != null ? team.getName() + " – " : "") + message.getSenderUsername();
                desktopNotifier.notify(title, truncateText(previewText(message), 120), type);
            }
        });
    }

    @Override
    public void onTeamUpdated(TeamDto team) {
        Platform.runLater(() -> {
            UserListController lc = sidebarController != null ? sidebarController.getUserListController() : null;
            if (lc != null) lc.upsertTeam(team);
            if (currentTeam != null && currentTeam.getId().equals(team.getId())) {
                currentTeam = team;
                if (topbarController != null) topbarController.updateTeam(team);
            }
        });
    }

    @Override
    public void onTeamRemoved(TeamDto team) {
        Platform.runLater(() -> {
            UserListController lc = sidebarController != null ? sidebarController.getUserListController() : null;
            if (lc != null) {
                lc.removeTeam(team.getId());
                updateGlobalBadge();
            }
            closeTeamIfOpen(team.getId(), "Vous ne faites plus partie de l'équipe « " + team.getName() + " ».");
        });
    }

    private void closeTeamIfOpen(Long teamId, String reason) {
        if (currentTeam == null || !currentTeam.getId().equals(teamId)) return;
        currentTeam = null;
        cancelReply();
        chatContainer.getChildren().clear();
        addSystemMessage(reason);
        if (topbarController != null) topbarController.setCurrentUser(AuthService.getCurrentUsername(), true);
    }

    /** Menu « Infos » de la barre du haut : gestion de l'équipe ouverte. */
    private void showCurrentTeamInfo() {
        if (currentTeam == null || sidebarController == null) return;
        UserListController lc = sidebarController.getUserListController();
        if (lc != null) lc.openTeamInfo(currentTeam);
    }

    /** Aperçu de son propre message dans la liste (l'envoi n'est pas renvoyé à l'expéditeur). */
    private void showOwnTeamMessageInList(Long teamId, MessageResponse msg) {
        UserListController lc = sidebarController != null ? sidebarController.getUserListController() : null;
        if (lc == null) return;
        lc.updateTeamLastMessage(teamId, previewText(msg), msg.getTimestamp(), true);
        lc.moveTeamToTop(teamId);
    }

    // ================================================================
    // CONTACTS
    // ================================================================
    @Override
    public void onContactSelected(UserDto user) {
        System.out.println("Dashboard Hub: Utilisateur sélectionné ID=" + user.getId() + " - " + user.getUsername());
        setCurrentReceiver(user.getId());
        if (topbarController != null) topbarController.updateContact(user);
    }

    private void loadInitialUserList() {
        new Thread(() -> {
            List<ConversationPreviewDto> conversations = userService.getConversations();
            Platform.runLater(() -> {
                if (sidebarController != null) {
                    UserListController lc = sidebarController.getUserListController();
                    if (lc != null) {
                        lc.updateConversations(conversations);
                        System.out.println("UI CHARGEMENT: " + conversations.size() + " conversations chargées.");
                    }
                }
            });
        }).start();

        new Thread(() -> {
            List<TeamDto> teams = teamService.getMyTeams();
            Platform.runLater(() -> {
                UserListController lc = sidebarController != null ? sidebarController.getUserListController() : null;
                if (lc != null) {
                    lc.setTeams(teams);
                    System.out.println("UI CHARGEMENT: " + teams.size() + " équipes chargées.");
                }
            });
        }, "load-teams").start();
    }

    private void searchInMessages(String searchText) {
        if (chatContainer == null) return;
        chatContainer.getChildren().forEach(node -> {
            boolean visible = true;
            if (!searchText.isEmpty() && node instanceof VBox) {
                visible = ((VBox) node).getChildren().stream().anyMatch(child -> {
                    if (child instanceof HBox) {
                        return ((HBox) child).getChildren().stream().anyMatch(sub -> {
                            if (sub instanceof VBox) {
                                return ((VBox) sub).getChildren().stream().anyMatch(lbl -> {
                                    if (lbl instanceof Label) {
                                        String t = ((Label) lbl).getText();
                                        return t != null && t.toLowerCase().contains(searchText);
                                    }
                                    return false;
                                });
                            }
                            return false;
                        });
                    }
                    return false;
                });
            }
            node.setVisible(visible);
            node.setManaged(visible);
        });
    }

    @Override
    public void onPresenceUpdate(PresenceUpdate update) {
        Platform.runLater(() -> {
            if (sidebarController != null) {
                UserListController lc = sidebarController.getUserListController();
                if (lc != null) {
                    lc.updateOrCreateUser(new UserDto(update.getUserId(), update.getUsername(), update.isOnline()));
                }
            }
        });
    }

    // ================================================================
    // ENVOI DE MESSAGE
    // ================================================================
    @FXML
    private void handleSendMessage() {
        String content = messageInput.getText().trim();
        if (content.isEmpty() || (currentReceiverId == null && currentTeam == null)) return;

        // Mode édition
        if (currentEditMessageId != null) {
            sendEditRequest(currentEditMessageId, content);
            return;
        }

        MessageType selectedType = messageTypeSelector.getValue();
        MessageRequest request = new MessageRequest(currentReceiverId, content, selectedType);
        if (currentTeam != null) request.setTeamId(currentTeam.getId());

        // 🔑 Inclure la référence de réponse
        if (currentReplyTo != null) {
            request.setReplyToId(currentReplyTo.getMessageId());
            request.setReplyToContent(currentReplyTo.getContent());
            request.setReplyToSenderId(currentReplyTo.getSenderId());
        }

        if (websocketReady) {
            webSocketService.sendMessage(request);

            MessageResponse tempMsg = new MessageResponse();
            tempMsg.setContent(content);
            tempMsg.setMessageType(selectedType);
            tempMsg.setTimestamp(Instant.now());
            tempMsg.setStatus(MessageStatus.SENT);
            tempMsg.setSenderId(currentUserId);
            tempMsg.setTeamId(request.getTeamId());

            // Copier les infos de réponse pour affichage immédiat
            if (currentReplyTo != null) {
                tempMsg.setReplyToContent(currentReplyTo.getContent());
                tempMsg.setReplyToSenderId(currentReplyTo.getSenderId());
            }

            addMessageBubble(tempMsg, true);
            messageInput.clear();
            scrollToBottom();
            if (currentTeam != null) showOwnTeamMessageInList(currentTeam.getId(), tempMsg);

            // Réinitialiser la réponse
            cancelReply();
        }
    }

    // Envoyer une requête de modification
    private void sendEditRequest(Long messageId, String newContent) {
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                String body = "{\"content\": \"" + newContent.replace("\"", "\\\"") + "\"}";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:8080/api/messages/" + messageId))
                        .header("Authorization", "Bearer " + AuthService.getCurrentJwtToken())
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> response = client.send(request,
                        HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        updateMessageInChat(messageId, newContent);
                        System.out.println("✏️ Message modifié ID: " + messageId);
                    }
                    // Réinitialiser le mode édition
                    currentEditMessageId = null;
                    messageInput.clear();
                    messageInput.setStyle(""); // Remettre le style normal
                });
            } catch (Exception e) {
                System.err.println("Erreur édition: " + e.getMessage());
            }
        }).start();
    }

    // Mettre à jour visuellement le message modifié
    private void updateMessageInChat(Long messageId, String newContent) {
        chatContainer.getChildren().forEach(node -> {
            if (node instanceof VBox vbox &&
                    vbox.getProperties().get("messageId") != null &&
                    vbox.getProperties().get("messageId").equals(messageId)) {
                // Trouver le Label du contenu et le mettre à jour
                findAndUpdateLabel(vbox, newContent);
            }
        });
    }

    private void findAndUpdateLabel(VBox container, String newContent) {
        container.getChildren().forEach(child -> {
            if (child instanceof HBox hbox) {
                hbox.getChildren().forEach(sub -> {
                    if (sub instanceof VBox vbox) {
                        vbox.getChildren().forEach(item -> {
                            if (item instanceof VBox inner) {
                                inner.getChildren().forEach(lbl -> {
                                    if (lbl instanceof Label label &&
                                            !label.getText().isEmpty() &&
                                            !label.getStyle().contains("font-size: 10px")) {
                                        label.setText(newContent + " ✏️");
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }


    // ================================================================
    // UTILITAIRE
    // ================================================================
    private String truncateText(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    // ================================================================
    // SOS
    // ================================================================
    private void showSOSDialog() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation d'alerte SOS");
        confirmAlert.setHeaderText("⚠️ ENVOYER UNE ALERTE SOS ?");
        confirmAlert.setContentText("Cette action va envoyer une alerte d'urgence à TOUS les utilisateurs.\n\nVoulez-vous vraiment continuer ?");

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
                sendSOSAlert(locationField.getText().trim(), descriptionArea.getText().trim());
            }
        });
    }

    private void sendSOSAlert(String location, String description) {
        new Thread(() -> {
            SOSAlertDto alert = sosAlertService.sendSOSAlert(location, description);
            Platform.runLater(() -> {
                if (alert != null) {
                    Alert ok = new Alert(Alert.AlertType.INFORMATION);
                    ok.setTitle("Alerte envoyée");
                    ok.setHeaderText("✅ Alerte SOS envoyée avec succès");
                    ok.setContentText("L'alerte a été diffusée à " + alert.getTotalOnlineUsers() + " utilisateurs en ligne.");
                    ok.show();
                } else {
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Erreur");
                    err.setHeaderText("❌ Échec de l'envoi");
                    err.setContentText("Impossible d'envoyer l'alerte SOS.");
                    err.show();
                }
            });
        }).start();
    }

    private void checkActiveSOSAlert() {
        String jwtToken = AuthService.getCurrentJwtToken();
        if (jwtToken == null) return;
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/sos/active"))
                    .header("Authorization", "Bearer " + jwtToken)
                    .GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && !response.body().isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                SOSAlertDto activeAlert = mapper.readValue(response.body(), SOSAlertDto.class);

                if (Boolean.TRUE.equals(activeAlert.getHasAcknowledged())) {
                    System.out.println("ℹ️ Alerte déjà accusée par cet utilisateur au démarrage.");
                    acknowledgedAlertIds.add(activeAlert.getId());
                    return;
                }
                System.out.println("🚨 Alerte SOS active au démarrage : ID=" + activeAlert.getId());
                Platform.runLater(() -> onSOSAlertReceived(activeAlert));
            } else {
                System.out.println("✅ Aucune alerte SOS active au démarrage.");
            }
        } catch (Exception e) {
            System.err.println("Erreur vérification alerte active: " + e.getMessage());
        }
    }

    @Override
    public void onSOSAlertReceived(SOSAlertDto alert) {
        Platform.runLater(() -> {
            if (acknowledgedAlertIds.contains(alert.getId())) {
                System.out.println("ℹ️ Alerte " + alert.getId() + " déjà accusée, ignorée.");
                return;
            }
            currentActiveAlert = alert;
            playAlarmSound();
            if (sidebarController != null) {
                NavBarController nav = sidebarController.getNavBarController();
                if (nav != null) nav.showSOSBadge();
            }
            System.out.println("🚨 Badge SOS affiché - alerte de " + alert.getSenderUsername());

            if (desktopNotifier != null && !desktopNotifier.isWindowActive()) {
                desktopNotifier.notify("🚨 ALERTE SOS – " + alert.getSenderUsername(),
                        (alert.getLocation() != null ? alert.getLocation() : "")
                                + (alert.getDescription() != null && !alert.getDescription().isEmpty()
                                ? " : " + alert.getDescription() : ""),
                        MessageType.ALERTE);
            }
        });
    }

    private void showSOSDetails() {
        if (currentActiveAlert == null) return;
        try {
            if (sosAlertStage != null && sosAlertStage.isShowing()) sosAlertStage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SOSAlertPopup.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/theme-mining.css").toExternalForm());

            sosAlertStage = new Stage();
            sosAlertStage.initModality(Modality.NONE);
            sosAlertStage.initStyle(StageStyle.UNDECORATED);
            sosAlertStage.setScene(scene);
            sosAlertStage.setAlwaysOnTop(true);

            sosPopupController = loader.getController();
            sosPopupController.setAlert(currentActiveAlert);
            sosPopupController.setOnAcknowledgeCallback(alertId -> {
                acknowledgedAlertIds.add(alertId);
                if (sidebarController != null) {
                    NavBarController nav = sidebarController.getNavBarController();
                    if (nav != null) nav.hideSOSBadge();
                }
            });

            sosAlertStage.show();
            sosAlertStage.toFront();
        } catch (Exception e) {
            System.err.println("Erreur affichage détails SOS: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onSOSAcknowledgment(SOSAcknowledgmentDto ack) {
        Platform.runLater(() -> {
            System.out.println("✅ Accusé reçu de : " + ack.getUsername());
            if (sosPopupController != null) sosPopupController.updateAckCount();
        });
    }

    private void playAlarmSound() {
        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(8000f, 8, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();
                for (int bip = 0; bip < 3; bip++) {
                    byte[] buffer = new byte[8000];
                    for (int i = 0; i < buffer.length; i++) {
                        double angle = i / (8000.0 / 880.0) * 2.0 * Math.PI;
                        buffer[i] = (byte) (Math.sin(angle) * 127);
                    }
                    line.write(buffer, 0, buffer.length);
                    Thread.sleep(200);
                }
                line.drain();
                line.close();
            } catch (Exception e) {
                System.err.println("⚠️ Son impossible: " + e.getMessage());
            }
        }).start();
    }
    // ================================================================
    // BULLES DE MESSAGE
    // ================================================================

    private void addMessageBubble(MessageResponse msg, boolean isSent) {
        VBox bubbleContainer = buildMessageBubbleNode(msg, isSent);
        chatContainer.getChildren().add(bubbleContainer);
    }

    private VBox buildMessageBubbleNode(MessageResponse msg, boolean isSent) {

        VBox bubbleContainer = new VBox(3);
        bubbleContainer.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        bubbleContainer.getProperties().put("messageId", msg.getMessageId());

        // ── Badge type de message ────────────────────────────────────────
        if (msg.getMessageType() != null && msg.getMessageType() != MessageType.TEXT) {
            Label typeBadge = new Label(msg.getMessageType().toString());
            typeBadge.setStyle(
                    "-fx-background-color: rgba(0,0,0,0.6); -fx-text-fill: white; " +
                            "-fx-font-size: 9px; -fx-padding: 2 6 2 6; -fx-background-radius: 8;"
            );
            HBox badgeBox = new HBox(typeBadge);
            badgeBox.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            badgeBox.setPadding(new Insets(0, 5, 2, 5));
            bubbleContainer.getChildren().add(badgeBox);
        }

        // ── Bulle principale ─────────────────────────────────────────────
        VBox messageContent = new VBox(6);
        messageContent.setPadding(new Insets(10, 15, 8, 15));
        messageContent.setMaxWidth(300);

        // Style : fichiers ont un fond neutre, messages normaux gardent leur couleur
        if ((isImageAttachment(msg) || isPdfAttachment(msg)) && !Boolean.TRUE.equals(msg.getIsDeleted())) {
            messageContent.setStyle(
                    "-fx-background-color: " + (isSent ? "rgba(230,230,230,0.95)" : "rgba(255,255,255,0.95)") + "; " +
                            "-fx-background-radius: " + (isSent ? "20 0 20 20" : "0 20 20 20") + "; " +
                            "-fx-padding: 8;"
            );
        } else {
            messageContent.setStyle(getMessageStyle(
                    msg.getMessageType() != null ? msg.getMessageType() : MessageType.TEXT, isSent
            ));
        }

        // ── Auteur (messages reçus dans une équipe) ─────────────────────
        if (!isSent && msg.getTeamId() != null && msg.getSenderUsername() != null) {
            Label author = new Label(msg.getSenderUsername());
            author.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #FF8C00;");
            messageContent.getChildren().add(author);
        }

        // ── Citation de réponse ──────────────────────────────────────────
        if (msg.getReplyToContent() != null && !msg.getReplyToContent().isEmpty()) {
            HBox quoteBox = new HBox(5);
            quoteBox.setStyle(
                    "-fx-background-color: rgba(0,0,0,0.08); " +
                            "-fx-background-radius: 6; -fx-padding: 6 8;"
            );
            FontIcon qIcon = new FontIcon("fas-hard-hat");
            qIcon.setIconSize(10);
            qIcon.setIconColor(Color.web("#FF8C00"));

            VBox qContent = new VBox(2);
            Label qSender = new Label(
                    msg.getReplyToSenderId() != null && msg.getReplyToSenderId().equals(currentUserId)
                            ? "Vous" : "..."
            );
            qSender.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #FF8C00;");
            Label qText = new Label(truncateText(msg.getReplyToContent(), 60));
            qText.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(0,0,0,0.6);");
            qContent.getChildren().addAll(qSender, qText);
            quoteBox.getChildren().addAll(qIcon, qContent);
            messageContent.getChildren().add(quoteBox);
        }

        // ── Contenu principal ────────────────────────────────────────────
        if (Boolean.TRUE.equals(msg.getIsDeleted())) {
            // Message supprimé
            Label del = new Label("🚫 Message supprimé");
            del.setStyle("-fx-font-style: italic; -fx-text-fill: rgba(0,0,0,0.4); -fx-font-size: 12px;");
            messageContent.getChildren().add(del);

        } else if (isImageAttachment(msg)) {
            // ── IMAGE ────────────────────────────────────────────────────
            buildImageContent(msg, messageContent);

        } else if (isPdfAttachment(msg)) {
            // ── PDF ──────────────────────────────────────────────────────
            buildPdfContent(msg, messageContent);

        } else {
            // ── TEXTE ────────────────────────────────────────────────────
            String text = msg.getContent() != null ? msg.getContent() : "";
            if (Boolean.TRUE.equals(msg.getIsEdited()) && !text.isEmpty()) text += "  ✏️";
            Label msgLabel = new Label(text);
            msgLabel.setWrapText(true);
            msgLabel.setMaxWidth(460);
            messageContent.getChildren().add(msgLabel);
        }

        // ── Métadonnées (heure + statut) ─────────────────────────────────
        HBox metaBox = new HBox(5);
        metaBox.setAlignment(Pos.CENTER_RIGHT);

        Label timeLabel = new Label(TIME_FORMATTER.format(msg.getTimestamp()));
        timeLabel.setStyle("-fx-text-fill: rgba(0,0,0,0.5); -fx-font-size: 10px;");
        metaBox.getChildren().add(timeLabel);

        if (isSent) {
            FontIcon statusIcon = new FontIcon();
            if (msg.getStatus() == MessageStatus.READ) {
                statusIcon.setIconLiteral("fas-check-double");
                statusIcon.setIconColor(Color.rgb(0, 150, 255));
            } else {
                statusIcon.setIconLiteral("fas-check");
                statusIcon.setIconColor(Color.rgb(150, 150, 150));
            }
            statusIcon.setIconSize(10);
            metaBox.getChildren().add(statusIcon);
        }
        messageContent.getChildren().add(metaBox);

        // ── Bouton d'action (casque minier) ─────────────────────────────
        Button actionBtn = createMiningActionButton();
        actionBtn.setVisible(false);
        actionBtn.setOnAction(e -> showMessageActionsPopup(msg, isSent, actionBtn));

        HBox messageRow = new HBox(6);
        messageRow.setAlignment(isSent ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        if (isSent) {
            messageRow.getChildren().addAll(actionBtn, messageContent);
        } else {
            messageRow.getChildren().addAll(messageContent, actionBtn);
        }

        messageRow.setOnMouseEntered(e -> actionBtn.setVisible(true));
        messageRow.setOnMouseExited(e -> {
            if (currentActionPopup == null || !currentActionPopup.isShowing()) {
                actionBtn.setVisible(false);
            }
        });

        bubbleContainer.getChildren().add(messageRow);
        return bubbleContainer;
    }


    // ================================================================
// 🔑 CONTENU IMAGE
// ================================================================
    private void buildImageContent(MessageResponse msg, VBox messageContent) {

        // Emplacement réservé dans la bulle : le placeholder puis l'image (ou l'erreur)
        // y sont placés, quel que soit l'ordre d'ajout des autres éléments.
        StackPane imageSlot = new StackPane();
        imageSlot.setAlignment(Pos.CENTER_LEFT);

        HBox loadingBox = new HBox(8);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(20, 30, 20, 30));
        loadingBox.setStyle("-fx-background-color: #e8e8e8; -fx-background-radius: 8;");

        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(24, 24);
        Label loadingLbl = new Label("Chargement...");
        loadingLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");
        loadingBox.getChildren().addAll(pi, loadingLbl);

        imageSlot.getChildren().add(loadingBox);
        messageContent.getChildren().add(imageSlot);
        messageContent.setMaxWidth(310);

        // Miniature 280px chargée en arrière-plan depuis l'URL présignée MinIO
        Image thumb = new Image(msg.getFileUrl(), 280, 0, true, true, true);

        boolean[] shown = {false};
        Runnable showResult = () -> {
            if (shown[0]) return;
            shown[0] = true;

            if (thumb.isError()) {
                Throwable ex = thumb.getException();
                System.err.println("❌ Image non chargée : " + msg.getFileUrl()
                        + " → " + (ex != null ? ex.getMessage() : "erreur inconnue"));
                Label errLabel = new Label("⚠️ Image non disponible");
                errLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px; -fx-padding: 10;");
                imageSlot.getChildren().setAll(errLabel);
                return;
            }

            ImageView iv = new ImageView(thumb);
            iv.setFitWidth(Math.min(280, thumb.getWidth()));
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            iv.setCursor(javafx.scene.Cursor.HAND);
            iv.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 4, 0, 0, 1);");
            iv.setOnMouseClicked(e -> openImageFullscreen(msg.getFileUrl(), msg.getFileName()));

            Label nameLbl = new Label("🖼  " + (msg.getFileName() != null ? msg.getFileName() : "image"));
            nameLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");

            imageSlot.getChildren().setAll(new VBox(4, iv, nameLbl));
        };

        // En cas d'erreur, progress n'atteint pas forcément 1.0 : on écoute aussi errorProperty
        if (thumb.isError() || thumb.getProgress() >= 1.0) {
            showResult.run();
        } else {
            thumb.progressProperty().addListener((obs, old, p) -> {
                if (p.doubleValue() >= 1.0) Platform.runLater(showResult);
            });
            thumb.errorProperty().addListener((obs, old, err) -> {
                if (err) Platform.runLater(showResult);
            });
        }
    }


    // ================================================================
// 🔑 CONTENU PDF
// ================================================================
    private void buildPdfContent(MessageResponse msg, VBox messageContent) {

        HBox pdfBox = new HBox(12);
        pdfBox.setAlignment(Pos.CENTER_LEFT);
        pdfBox.setPadding(new Insets(10, 15, 10, 12));
        pdfBox.setMaxWidth(280);
        pdfBox.setStyle(buildPdfStyle(false));

        // Icône PDF
        FontIcon pdfIcon = new FontIcon("fas-file-pdf");
        pdfIcon.setIconSize(38);
        pdfIcon.setIconColor(Color.web("#DC2626"));

        // Infos
        VBox pdfInfo = new VBox(3);
        Label pdfName = new Label(msg.getFileName() != null ? msg.getFileName() : "Document.pdf");
        pdfName.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #222;");
        pdfName.setMaxWidth(180);
        pdfName.setWrapText(false);

        String sizeTxt = msg.getFileSize() != null && msg.getFileSize() > 0
                ? formatFileSize(msg.getFileSize()) : "PDF";
        Label pdfMeta = new Label("PDF  •  " + sizeTxt);
        pdfMeta.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

        Label pdfHint = new Label("▶  Cliquer pour ouvrir");
        pdfHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #DC2626;");

        pdfInfo.getChildren().addAll(pdfName, pdfMeta, pdfHint);
        pdfBox.getChildren().addAll(pdfIcon, pdfInfo);

        // Hover
        pdfBox.setOnMouseEntered(e -> pdfBox.setStyle(buildPdfStyle(true)));
        pdfBox.setOnMouseExited(e -> pdfBox.setStyle(buildPdfStyle(false)));

        // Clic → Télécharger et ouvrir
        pdfBox.setOnMouseClicked(e -> openPdf(msg.getFileUrl(), msg.getFileName()));

        // Ajouté à la suite (la citation éventuelle est déjà au-dessus, l'heure viendra après)
        messageContent.getChildren().add(pdfBox);
        messageContent.setMaxWidth(320);
    }

    /** Pièce jointe image : type MIME image/*, ou à défaut type IMAGE / extension. */
    private boolean isImageAttachment(MessageResponse msg) {
        if (msg.getFileUrl() == null || msg.getFileUrl().isBlank()) return false;
        if (msg.getFileType() != null) return msg.getFileType().startsWith("image/");
        String name = msg.getFileName() != null ? msg.getFileName().toLowerCase() : "";
        return msg.getMessageType() == MessageType.IMAGE || name.matches(".*\\.(jpe?g|png|gif|webp)$");
    }

    /** Pièce jointe PDF : type MIME application/pdf, ou à défaut extension .pdf. */
    private boolean isPdfAttachment(MessageResponse msg) {
        if (msg.getFileUrl() == null || msg.getFileUrl().isBlank()) return false;
        if (msg.getFileType() != null) return "application/pdf".equals(msg.getFileType());
        String name = msg.getFileName() != null ? msg.getFileName().toLowerCase() : "";
        return name.endsWith(".pdf") || msg.getMessageType() == MessageType.FILE;
    }

    private String buildPdfStyle(boolean hovered) {
        return "-fx-background-color: " + (hovered ? "rgba(220,38,38,0.15)" : "rgba(220,38,38,0.08)") + "; " +
                "-fx-background-radius: 10; -fx-cursor: hand;";
    }

    // ================================================================
    //  CRÉER LE BOUTON D'ACTION UNIQUE MINING
    // ================================================================
    private Button createMiningActionButton() {
        Button btn = new Button();

        FontIcon icon = new FontIcon("fas-hard-hat");
        icon.setIconSize(14);
        icon.setIconColor(Color.WHITE);
        btn.setGraphic(icon);

        btn.setStyle(
                "-fx-background-color: rgba(255, 140, 0, 0.7); " +
                        "-fx-background-radius: 50%; " +
                        "-fx-padding: 6; " +
                        "-fx-cursor: hand; " +
                        "-fx-min-width: 28; -fx-max-width: 28; " +
                        "-fx-min-height: 28; -fx-max-height: 28;"
        );

        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: rgba(255, 140, 0, 1.0); " +
                        "-fx-background-radius: 50%; " +
                        "-fx-padding: 6; " +
                        "-fx-cursor: hand; " +
                        "-fx-min-width: 28; -fx-max-width: 28; " +
                        "-fx-min-height: 28; -fx-max-height: 28;"
        ));

        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: rgba(255, 140, 0, 0.7); " +
                        "-fx-background-radius: 50%; " +
                        "-fx-padding: 6; " +
                        "-fx-cursor: hand; " +
                        "-fx-min-width: 28; -fx-max-width: 28; " +
                        "-fx-min-height: 28; -fx-max-height: 28;"
        ));

        return btn;
    }


    // ================================================================
    //  POPUP DES ACTIONS
    // ================================================================
    private void showMessageActionsPopup(MessageResponse msg, boolean isSent, Button anchor) {
        // Fermer le popup précédent si ouvert
        if (currentActionPopup != null && currentActionPopup.isShowing()) {
            currentActionPopup.hide();
        }

        Popup popup = new Popup();
        popup.setAutoHide(true);
        currentActionPopup = popup;

        VBox menu = new VBox(0);
        menu.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 12, 0, 0, 3);"
        );

        //  Bouton Répondre (pour tous)
        Button replyBtn = createActionMenuItem("fas-reply", "Répondre", "#2196F3");
        replyBtn.setOnAction(e -> {
            handleReply(msg);
            popup.hide();
        });

        menu.getChildren().add(replyBtn);

        if (isSent) {
            // Séparateur
            menu.getChildren().add(createMenuSeparator());

            //  Bouton Modifier
            Button editBtn = createActionMenuItem("fas-edit", "Modifier", "#FF8C00");
            editBtn.setOnAction(e -> {
                handleEdit(msg);
                popup.hide();
            });

            // Séparateur
            menu.getChildren().add(editBtn);
            menu.getChildren().add(createMenuSeparator());

            //  Bouton Supprimer
            Button deleteBtn = createActionMenuItem("fas-trash-alt", "Supprimer", "#DC2626");
            deleteBtn.setOnAction(e -> {
                handleDelete(msg);
                popup.hide();
            });

            menu.getChildren().add(deleteBtn);
        }

        popup.getContent().add(menu);

        // Position du popup
        double x = anchor.localToScreen(anchor.getBoundsInLocal()).getMinX();
        double y = anchor.localToScreen(anchor.getBoundsInLocal()).getMaxY() + 5;
        popup.show(anchor, x, y);

        // Masquer le bouton quand popup se ferme
        popup.setOnHidden(e -> currentActionPopup = null);
    }

    private Button createActionMenuItem(String iconLiteral, String text, String color) {
        Button btn = new Button();
        btn.setMaxWidth(Double.MAX_VALUE);

        HBox content = new HBox(10);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(10, 20, 10, 15));

        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(15);
        icon.setIconColor(Color.web(color));

        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");

        content.getChildren().addAll(icon, lbl);
        btn.setGraphic(content);
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0; -fx-min-width: 180;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #f5f5f5; -fx-cursor: hand; -fx-padding: 0; -fx-min-width: 180;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0; -fx-min-width: 180;"));

        return btn;
    }

    private javafx.scene.control.Separator createMenuSeparator() {
        javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
        sep.setStyle("-fx-padding: 0;");
        return sep;
    }


    // ================================================================
//  RÉPONDRE
// ================================================================
    private void handleReply(MessageResponse msg) {
        currentReplyTo = msg;

        if (replyPreviewBar != null) {
            replyToLabel.setText("Répondre à " + (msg.getSenderId().equals(currentUserId) ? "vous-même"
                    : msg.getSenderUsername() != null ? msg.getSenderUsername() : "..."));
            replyPreviewText.setText(truncateText(msg.getContent(), 80));
            replyPreviewBar.setVisible(true);
            replyPreviewBar.setManaged(true);
            messageInput.requestFocus();
        }

        System.out.println("↩️ Répondre à : " + msg.getContent());
    }

    @FXML
    private void cancelReply() {
        currentReplyTo = null;
        if (replyPreviewBar != null) {
            replyPreviewBar.setVisible(false);
            replyPreviewBar.setManaged(false);
        }
    }


    // ================================================================
//  MODIFIER
// ================================================================
    private void handleEdit(MessageResponse msg) {
        currentEditMessageId = msg.getMessageId();

        // Pré-remplir le champ de saisie avec le texte à modifier
        messageInput.setText(msg.getContent());
        messageInput.requestFocus();
        messageInput.positionCaret(msg.getContent().length());

        // Changer le style du champ pour indiquer le mode édition
        messageInput.setStyle(
                "-fx-background-color: rgba(255, 140, 0, 0.1); " +
                        "-fx-border-color: #FF8C00; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8;"
        );

        System.out.println(" Mode édition pour message ID: " + msg.getMessageId());
    }


    // ================================================================
    //  SUPPRIMER
    // ================================================================
    private void handleDelete(MessageResponse msg) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le message");
        confirm.setHeaderText("⛏️ Supprimer ce message ?");
        confirm.setContentText("Cette action est irréversible.\n\n\"" +
                truncateText(msg.getContent(), 50) + "\"");

        ButtonType btnDelete = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnDelete, btnCancel);

        confirm.showAndWait().ifPresent(response -> {
            if (response == btnDelete) {
                new Thread(() -> {
                    try {
                        HttpClient client = HttpClient.newHttpClient();
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(new URI("http://localhost:8080/api/messages/" + msg.getMessageId()))
                                .header("Authorization", "Bearer " + AuthService.getCurrentJwtToken())
                                .DELETE()
                                .build();
                        HttpResponse<String> httpResponse = client.send(request,
                                HttpResponse.BodyHandlers.ofString());

                        Platform.runLater(() -> {
                            if (httpResponse.statusCode() == 200) {
                                // Supprimer visuellement de la liste
                                removeMessageFromChat(msg.getMessageId());
                                System.out.println("🗑️ Message supprimé ID: " + msg.getMessageId());
                            } else {
                                System.err.println("❌ Erreur suppression: " + httpResponse.statusCode());
                            }
                        });
                    } catch (Exception e) {
                        System.err.println("Erreur suppression: " + e.getMessage());
                    }
                }).start();
            }
        });
    }

    private void removeMessageFromChat(Long messageId) {
        chatContainer.getChildren().removeIf(node -> {
            if (node instanceof VBox vbox) {
                return vbox.getProperties().containsKey("messageId") &&
                        vbox.getProperties().get("messageId").equals(messageId);
            }
            return false;
        });
    }


    private void addSystemMessage(String content) {
        Label lbl = new Label(content);
        lbl.setStyle("-fx-text-fill: #888; -fx-font-size: 11px; -fx-font-style: italic;");
        HBox box = new HBox(lbl);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(5, 0, 5, 0));
        box.getStyleClass().add("system-mssg");
        chatContainer.getChildren().add(box);
    }

    private String getMessageStyle(MessageType type, boolean isSent) {
        String base = isSent
                ? "-fx-background-radius: 20 0 20 20; -fx-font-size: 13px; -fx-text-fill: white;"
                : "-fx-background-radius: 0 20 20 20; -fx-font-size: 13px; -fx-text-fill: white;";

        String bg = switch (type) {
            case TEXT               -> "rgba(255, 255, 255)";
            case INCIDENT           -> "rgba(255, 68, 68, 0.8)";
            case URGENT             -> "rgba(255, 102, 0, 0.8)";
            case ALERTE             -> "rgba(204, 0, 0, 0.8)";
            case MAINTENANCE        -> "rgba(255, 170, 0, 0.8)";
            case SECURITE           -> "rgba(255, 153, 0, 0.8)";
            case IMAGE              -> "rgba(156, 39, 176, 0.8)";
            case FILE               -> "rgba(96, 125, 139, 0.8)";
            case DEMANDE_DE_SUPPORT -> "rgba(76, 175, 80, 0.8)";
        };
        return base + " -fx-background-color: " + bg + ";";
    }

    // ================================================================
    // FENÊTRE & SNAP
    // ================================================================
    @FXML private void closeWindow() {
        // Comme Telegram/WhatsApp : ✕ envoie MineCom en arrière-plan (zone de notification)
        if (desktopNotifier != null && desktopNotifier.isTraySupported()) {
            desktopNotifier.hideToTray();
        } else {
            ((Stage) titleBar.getScene().getWindow()).close();
        }
    }
    @FXML private void minimizeWindow() { ((Stage) titleBar.getScene().getWindow()).setIconified(true); }

    @FXML
    private void maximizeWindow() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        if (stage.isFullScreen()) {
            stage.setFullScreen(false);
            stage.setWidth(1400); stage.setHeight(800); stage.centerOnScreen();
        } else if (isMaximized(stage)) {
            stage.setWidth(1400); stage.setHeight(800); stage.centerOnScreen();
        } else {
            snapToMaximized();
        }
    }

    private boolean isMaximized(Stage stage) {
        Rectangle2D b = Screen.getPrimary().getVisualBounds();
        return Math.abs(stage.getX() - b.getMinX()) < 10 &&
                Math.abs(stage.getY() - b.getMinY()) < 10 &&
                Math.abs(stage.getWidth() - b.getWidth()) < 10 &&
                Math.abs(stage.getHeight() - b.getHeight()) < 10;
    }

    private void setupSnapMenu() {
        maximizeBtn.setOnMouseEntered(event -> showSnapPopup());
    }

    private void showSnapPopup() {
        if (snapPopup == null) snapPopup = createSnapPopup();
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
        container.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);");
        container.setOnMouseEntered(e -> {});
        Label title = new Label("Positionner la fenêtre");
        title.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #666;");
        GridPane grid = new GridPane(); grid.setHgap(8); grid.setVgap(8);
        Button b1 = createSnapButton("fas-window-maximize", "Maximiser");
        b1.setOnAction(e -> { snapToMaximized(); popup.hide(); });
        Button b2 = createSnapButton("fas-expand", "Plein écran");
        b2.setOnAction(e -> { snapToTrueFullScreen(); popup.hide(); });
        Button b3 = createSnapButton("fas-align-left", "Moitié gauche");
        b3.setOnAction(e -> { snapToLeft(); popup.hide(); });
        Button b4 = createSnapButton("fas-align-right", "Moitié droite");
        b4.setOnAction(e -> { snapToRight(); popup.hide(); });
        Button b5 = createSnapButton("fas-th", "Haut gauche");
        b5.setOnAction(e -> { snapToTopLeft(); popup.hide(); });
        Button b6 = createSnapButton("fas-th", "Haut droit");
        b6.setOnAction(e -> { snapToTopRight(); popup.hide(); });
        grid.add(b1, 0, 0); grid.add(b2, 1, 0);
        grid.add(b3, 0, 1); grid.add(b4, 1, 1);
        grid.add(b5, 0, 2); grid.add(b6, 1, 2);
        container.getChildren().addAll(title, grid);
        popup.getContent().add(container);
        return popup;
    }

    private Button createSnapButton(String iconLiteral, String tooltip) {
        Button btn = new Button();
        VBox content = new VBox(5); content.setAlignment(Pos.CENTER);
        FontIcon ic = new FontIcon(iconLiteral); ic.setIconSize(20); ic.setIconColor(Color.web("#666"));
        Label lbl = new Label(tooltip); lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        content.getChildren().addAll(ic, lbl); btn.setGraphic(content);
        String n = "-fx-background-color: #f5f5f5; -fx-background-radius: 6; -fx-padding: 10; -fx-cursor: hand; -fx-min-width: 100; -fx-pref-width: 100;";
        String h = "-fx-background-color: #e0e0e0; -fx-background-radius: 6; -fx-padding: 10; -fx-cursor: hand; -fx-min-width: 100; -fx-pref-width: 100;";
        btn.setStyle(n); btn.setOnMouseEntered(e -> btn.setStyle(h)); btn.setOnMouseExited(e -> btn.setStyle(n));
        return btn;
    }

    private void snapToMaximized() {
        Stage s = (Stage) titleBar.getScene().getWindow();
        Rectangle2D b = Screen.getPrimary().getVisualBounds();
        s.setFullScreen(false); s.setX(b.getMinX()); s.setY(b.getMinY());
        s.setWidth(b.getWidth()); s.setHeight(b.getHeight());
    }
    private void snapToTrueFullScreen() { ((Stage) titleBar.getScene().getWindow()).setFullScreen(true); }
    private void snapToLeft() {
        Stage s = (Stage) titleBar.getScene().getWindow();
        Rectangle2D b = Screen.getPrimary().getVisualBounds();
        s.setX(b.getMinX()); s.setY(b.getMinY()); s.setWidth(b.getWidth()/2); s.setHeight(b.getHeight());
    }
    private void snapToRight() {
        Stage s = (Stage) titleBar.getScene().getWindow();
        Rectangle2D b = Screen.getPrimary().getVisualBounds();
        s.setX(b.getMinX()+b.getWidth()/2); s.setY(b.getMinY()); s.setWidth(b.getWidth()/2); s.setHeight(b.getHeight());
    }
    private void snapToTopLeft() {
        Stage s = (Stage) titleBar.getScene().getWindow();
        Rectangle2D b = Screen.getPrimary().getVisualBounds();
        s.setX(b.getMinX()); s.setY(b.getMinY()); s.setWidth(b.getWidth()/2); s.setHeight(b.getHeight()/2);
    }
    private void snapToTopRight() {
        Stage s = (Stage) titleBar.getScene().getWindow();
        Rectangle2D b = Screen.getPrimary().getVisualBounds();
        s.setX(b.getMinX()+b.getWidth()/2); s.setY(b.getMinY()); s.setWidth(b.getWidth()/2); s.setHeight(b.getHeight()/2);
    }

    private void setupMessageInput() {
        messageInput.textProperty().addListener((obs, oldVal, newVal) -> {
            int lines = newVal.split("\n").length;
            messageInput.setPrefHeight(Math.min(lines * 20 + 10, 150));
        });
        messageInput.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown() && !event.isControlDown()) {
                event.consume();
                handleSendMessage();
            }
        });
    }


    @Override
    public void onMessageEdited(MessageResponse msg) {
        // Seule une bulle affichée portant ce messageId est remplacée
        Platform.runLater(() -> refreshMessageBubble(msg));
    }

    @Override
    public void onMessageDeleted(MessageResponse msg) {
        Platform.runLater(() -> refreshMessageBubble(msg));
    }

    // 🔑 Remplace entièrement la bulle existante par la version à jour
    private void refreshMessageBubble(MessageResponse msg) {
        for (int i = 0; i < chatContainer.getChildren().size(); i++) {
            javafx.scene.Node node = chatContainer.getChildren().get(i);
            if (node instanceof VBox vbox &&
                    msg.getMessageId() != null &&
                    msg.getMessageId().equals(vbox.getProperties().get("messageId"))) {

                boolean isSent = msg.getSenderId().equals(currentUserId);
                chatContainer.getChildren().remove(i);

                VBox newBubble = buildMessageBubbleNode(msg, isSent);
                chatContainer.getChildren().add(i, newBubble);
                return;
            }
        }
    }

    // 🔑 UPLOAD + ENVOI via WebSocket
    private void uploadAndSendFile(File file, MessageType type) {

        // Afficher un indicateur de chargement dans le chat
        addSystemMessage("⏳ Envoi de " + file.getName() + "...");
        final Long receiverId = currentReceiverId; // figé : l'utilisateur peut changer de contact pendant l'upload
        final Long teamId = currentTeam != null ? currentTeam.getId() : null;

        new Thread(() -> {
            try {
                // 1. Upload vers le serveur (MinIO)
                FileService.FileUploadResult uploaded = fileService.uploadFile(file);

                Platform.runLater(() -> {
                    // 2. Supprimer le message "En cours..."
                    removeLastSystemMessage();

                    if (!websocketReady) {
                        showAlert("❌ WebSocket non connecté.");
                        return;
                    }

                    // 3. Construire la requête avec les infos fichier
                    MessageRequest request = new MessageRequest(receiverId, "", type);
                    request.setTeamId(teamId);
                    request.setFileUrl(uploaded.getUrl());
                    request.setFileName(uploaded.getFilename());
                    request.setFileType(uploaded.getType());
                    request.setFileSize(uploaded.getSizeAsLong());

                    // 4. Envoyer via WebSocket
                    webSocketService.sendMessage(request);

                    // 5. Afficher localement immédiatement
                    MessageResponse tempMsg = new MessageResponse();
                    tempMsg.setContent("");
                    tempMsg.setMessageType(type);
                    tempMsg.setTimestamp(Instant.now());
                    tempMsg.setStatus(MessageStatus.SENT);
                    tempMsg.setSenderId(currentUserId);
                    tempMsg.setFileUrl(uploaded.getUrl());
                    tempMsg.setFileName(uploaded.getFilename());
                    tempMsg.setFileType(uploaded.getType());
                    tempMsg.setFileSize(uploaded.getSizeAsLong());

                    tempMsg.setReceiverId(receiverId);
                    tempMsg.setTeamId(teamId);

                    // N'afficher la bulle que si la conversation est toujours ouverte
                    boolean stillOpen = teamId != null
                            ? currentTeam != null && teamId.equals(currentTeam.getId())
                            : receiverId != null && receiverId.equals(currentReceiverId);
                    if (stillOpen) {
                        addMessageBubble(tempMsg, true);
                        scrollToBottom();
                    }
                    if (teamId != null) showOwnTeamMessageInList(teamId, tempMsg);

                    System.out.println("✅ Fichier envoyé : " + uploaded.getFilename());
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    removeLastSystemMessage();
                    System.err.println("❌ Erreur envoi fichier: " + e.getMessage());
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur d'envoi");
                    alert.setHeaderText("⛏️ Impossible d'envoyer le fichier");
                    alert.setContentText(e.getMessage());
                    alert.show();
                });
            }
        }).start();
    }


    // ================================================================
// 🔑 OUVRIR IMAGE EN PLEIN ÉCRAN (pleine résolution, pas la miniature 280px)
// ================================================================
    private void openImageFullscreen(String fileUrl, String filename) {
        Stage imgStage = new Stage();
        imgStage.setTitle("🖼 " + (filename != null ? filename : "Image"));

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double maxW = screen.getWidth() * 0.85;
        double maxH = screen.getHeight() * 0.85;

        Image full = new Image(fileUrl, true);
        ImageView iv = new ImageView(full);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.setFitWidth(maxW);
        iv.setFitHeight(maxH);

        ProgressIndicator pi = new ProgressIndicator();
        pi.progressProperty().bind(full.progressProperty());
        pi.visibleProperty().bind(full.progressProperty().lessThan(1.0).and(full.errorProperty().not()));

        Label err = new Label("⚠️ Image non disponible");
        err.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        err.visibleProperty().bind(full.errorProperty());

        // Fond noir + fermer avec ESC ou clic
        StackPane root = new StackPane(iv, pi, err);
        root.setStyle("-fx-background-color: black;");
        root.setOnMouseClicked(e -> imgStage.close());

        Scene scene = new Scene(root, maxW, maxH);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) imgStage.close();
        });

        imgStage.setScene(scene);
        imgStage.centerOnScreen();
        imgStage.show();
    }


    // ================================================================
// 🔑 OUVRIR PDF avec l'application par défaut du PC
// ================================================================
    private void openPdf(String fileUrl, String filename) {
        if (fileUrl == null || fileUrl.isBlank()) {
            showAlert("❌ Lien du PDF manquant.");
            return;
        }
        addSystemMessage("⏳ Téléchargement du PDF...");

        new Thread(() -> {
            try {
                File tempPdf = fileService.downloadPdfToTemp(fileUrl, filename);

                if (!java.awt.Desktop.isDesktopSupported()
                        || !java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                    throw new Exception("ouverture de fichiers non supportée sur ce système");
                }
                // Desktop (AWT) est appelé hors du thread JavaFX pour ne pas le bloquer
                java.awt.Desktop.getDesktop().open(tempPdf);
                System.out.println("✅ PDF ouvert : " + filename);

                Platform.runLater(this::removeLastSystemMessage);

            } catch (Exception e) {
                Platform.runLater(() -> {
                    removeLastSystemMessage();
                    showAlert("❌ Impossible d'ouvrir le PDF : " + e.getMessage());
                });
            }
        }, "pdf-open").start();
    }


    // ================================================================
// UTILITAIRES
// ================================================================
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("MineCom");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private void removeLastSystemMessage() {
        if (!chatContainer.getChildren().isEmpty()) {
            int last = chatContainer.getChildren().size() - 1;
            javafx.scene.Node lastNode = chatContainer.getChildren().get(last);
            if (lastNode instanceof HBox hbox && hbox.getStyleClass().contains("system-msg")) {
                chatContainer.getChildren().remove(last);
            }
        }
    }
}