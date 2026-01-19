package com.mining.minecom.controller;

import com.mining.minecom.common.dto.ConversationPreviewDto;
import com.mining.minecom.common.dto.UserDto;
import com.mining.minecom.interfaces.ContactSelectionListener;
import com.mining.minecom_server.common.dto.LastMessageDto;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.geometry.Pos;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class UserListController {

    @FXML private ListView<ConversationPreviewDto> userListView;
    @FXML private TextField searchFiedUser;

    private ContactSelectionListener selectionListener;
    private ObservableList<ConversationPreviewDto> conversationList = FXCollections.observableArrayList();
    private ObservableList<ConversationPreviewDto> filteredConversations = FXCollections.observableArrayList();

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM").withZone(ZoneId.systemDefault());

    public void setSelectionListener(ContactSelectionListener listener) {
        this.selectionListener = listener;
    }

    @FXML
    public void initialize() {
        userListView.setItems(filteredConversations);
        if (searchFiedUser != null) {
            setupSearchField();
        }
        userListView.setCellFactory(lv -> new ListCell<ConversationPreviewDto>() {
            @Override
            protected void updateItem(ConversationPreviewDto conversation, boolean empty) {
                super.updateItem(conversation, empty);

                if (empty || conversation == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().remove("user-item");
                } else {
                    HBox userItem = new HBox(12);
                    userItem.getStyleClass().add("user-item");
                    userItem.setAlignment(Pos.CENTER_LEFT);

                    StackPane avatarContainer = createAvatar(conversation);

                    VBox userInfo = new VBox(4);
                    userInfo.setAlignment(Pos.CENTER_LEFT);
                    HBox.setHgrow(userInfo, Priority.ALWAYS);

                    Label nameLabel = new Label(conversation.getUsername());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                    String lastMessageText = "Aucun message";
                    if (conversation.getLastMessage() != null) {
                        LastMessageDto lastMsg = conversation.getLastMessage();
                        String prefix = lastMsg.isSentByMe() ? "Vous: " : "";
                        lastMessageText = prefix + truncate(lastMsg.getContent(), 30);
                    }

                    Label lastMessageLabel = new Label(lastMessageText);
                    lastMessageLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
                    lastMessageLabel.setMaxWidth(200);

                    userInfo.getChildren().addAll(nameLabel, lastMessageLabel);

                    VBox rightSection = new VBox(4);
                    rightSection.setAlignment(Pos.TOP_RIGHT);

                    String timeText = "";
                    if (conversation.getLastMessage() != null) {
                        timeText = formatTimestamp(conversation.getLastMessage().getTimestamp());
                    }

                    Label timeLabel = new Label(timeText);
                    timeLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");

                    rightSection.getChildren().add(timeLabel);

                    userItem.getChildren().addAll(avatarContainer, userInfo, rightSection);
                    setGraphic(userItem);
                }
            }
        });

        userListView.setOnMouseClicked(event -> {
            ConversationPreviewDto selected = userListView.getSelectionModel().getSelectedItem();
            if (selected != null && selectionListener != null) {
                // Convertir en UserDto pour la compatibilité
                UserDto userDto = new UserDto(selected.getUserId(), selected.getUsername(), selected.getIsOnline());
                selectionListener.onContactSelected(userDto);
            }
        });
    }
    private void setupSearchField() {
        searchFiedUser.textProperty().addListener((observable, oldValue, newValue) -> {
            filterConversations(newValue.toLowerCase().trim());
        });
    }
    // Filtrer les conversations
    private void filterConversations(String searchText) {
        if (searchText.isEmpty()) {
            // Si la recherche est vide, afficher toutes les conversations
            filteredConversations.setAll(conversationList);
        } else {
            // Filtrer par nom d'utilisateur OU contenu du message
            List<ConversationPreviewDto> filtered = conversationList.stream()
                    .filter(conversation -> {
                        // Recherche dans le nom d'utilisateur
                        boolean matchUsername = conversation.getUsername().toLowerCase().contains(searchText);

                        // Recherche dans le dernier message
                        boolean matchMessage = false;
                        if (conversation.getLastMessage() != null) {
                            matchMessage = conversation.getLastMessage().getContent()
                                    .toLowerCase().contains(searchText);
                        }

                        return matchUsername || matchMessage;
                    })
                    .collect(Collectors.toList());

            filteredConversations.setAll(filtered);

            System.out.println("🔍 Recherche: '" + searchText + "' - " +
                    filtered.size() + " résultats trouvés");
        }
    }

    // 🔑 FORMATER L'HEURE (Aujourd'hui: HH:mm, Sinon: dd/MM)
    private String formatTimestamp(Instant timestamp) {
        LocalDate messageDate = timestamp.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate today = LocalDate.now();

        if (messageDate.equals(today)) {
            return TIME_FORMATTER.format(timestamp);
        } else {
            return DATE_FORMATTER.format(timestamp);
        }
    }

    // Tronquer le texte
    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private StackPane createAvatar(ConversationPreviewDto conversation) {
        StackPane avatarContainer = new StackPane();
        avatarContainer.setPrefSize(45, 45);
        avatarContainer.setMinSize(45, 45);
        avatarContainer.setMaxSize(45, 45);

        Circle background = new Circle(22.5);
        background.setFill(getColorForUser(conversation.getUsername()));

        String initials = getInitials(conversation.getUsername());
        Label initialsLabel = new Label(initials);
        initialsLabel.setStyle(
                "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 16px;"
        );

        Circle statusIndicator = new Circle(6);
        statusIndicator.setFill(conversation.getIsOnline() ? Color.rgb(0, 200, 0) : Color.GRAY);
        statusIndicator.setStroke(Color.WHITE);
        statusIndicator.setStrokeWidth(2);
        StackPane.setAlignment(statusIndicator, Pos.BOTTOM_RIGHT);

        avatarContainer.getChildren().addAll(background, initialsLabel, statusIndicator);
        return avatarContainer;
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
        Color[] colors = {
                Color.rgb(255, 107, 107), Color.rgb(78, 205, 196),
                Color.rgb(69, 183, 209), Color.rgb(255, 159, 64),
                Color.rgb(153, 102, 255), Color.rgb(255, 99, 132),
                Color.rgb(54, 162, 235), Color.rgb(255, 206, 86),

        };
        return colors[Math.abs(username.hashCode()) % colors.length];
    }

    // 🔑 MÉTHODE MISE À JOUR
    public void updateConversations(List<ConversationPreviewDto> conversations) {
        conversationList.setAll(conversations);
        filteredConversations.setAll(conversations); // Copier dans la liste filtrée
    }

    // Pour compatibilité avec l'ancienne méthode
    public void updateOrCreateUser(UserDto update) {
        // Trouver et mettre à jour si existe
        conversationList.stream()
                .filter(c -> c.getUserId().equals(update.getId()))
                .findFirst()
                .ifPresent(c -> {
                    c.setIsOnline(update.getIsOnline());
                    userListView.refresh();
                });
    }
}