package com.mining.minecom.controller;

import com.mining.minecom.common.dto.ConversationPreviewDto;
import com.mining.minecom.common.dto.UserDto;
import com.mining.minecom.common.enums.MessageType;
import com.mining.minecom.interfaces.ContactSelectionListener;
import com.mining.minecom_server.common.dto.LastMessageDto;
import com.mining.minecom_server.common.dto.TeamDto;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.geometry.Pos;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class UserListController {

    @FXML private ListView<ConversationPreviewDto> userListView;
    @FXML private TextField searchFiedUser;
    @FXML private Button filterAllBtn;
    @FXML private Button filterUnreadBtn;
    @FXML private Button filterTeamsBtn;

    private ContactSelectionListener selectionListener;
    private ObservableList<ConversationPreviewDto> conversationList = FXCollections.observableArrayList();
    private ObservableList<ConversationPreviewDto> filteredConversations = FXCollections.observableArrayList();

    /** Filtre actif (boutons sous la recherche). */
    private enum Filter { ALL, UNREAD, TEAMS }
    private Filter activeFilter = Filter.ALL;

    // 🔑 NOTIFICATIONS : clé de conversation ("U<userId>" ou "T<teamId>") -> types de messages non lus
    private final Map<String, List<MessageType>> unreadMessages = new HashMap<>();

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM").withZone(ZoneId.systemDefault());

    public void setSelectionListener(ContactSelectionListener listener) {
        this.selectionListener = listener;
    }

    // Les ids des utilisateurs et des équipes peuvent se recouper : clés distinctes
    private static String userKey(Long userId) { return "U" + userId; }
    private static String teamKey(Long teamId) { return "T" + teamId; }
    private static String key(ConversationPreviewDto c) {
        return c.isTeamConversation() ? teamKey(c.getTeam().getId()) : userKey(c.getUserId());
    }

    @FXML
    public void initialize() {
        userListView.setItems(filteredConversations);

        if (searchFiedUser != null) {
            setupSearchField();
        }
        updateFilterButtons();

        userListView.setCellFactory(lv -> new ListCell<ConversationPreviewDto>() {
            @Override
            protected void updateItem(ConversationPreviewDto conversation, boolean empty) {
                super.updateItem(conversation, empty);

                if (empty || conversation == null) {
                    setText(null);
                    setGraphic(null);
                    setContextMenu(null);
                    getStyleClass().remove("user-item");
                } else {
                    HBox userItem = new HBox(12);
                    userItem.getStyleClass().add("user-item");
                    userItem.setAlignment(Pos.CENTER_LEFT);

                    // Avatar avec badge de notification
                    StackPane avatarContainer = createAvatar(conversation);

                    VBox userInfo = new VBox(4);
                    userInfo.setAlignment(Pos.CENTER_LEFT);
                    HBox.setHgrow(userInfo, Priority.ALWAYS);

                    Label nameLabel = new Label(conversation.getUsername());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                    String lastMessageText = conversation.isTeamConversation()
                            ? conversation.getTeam().getMembers().size() + " membres"
                            : "Aucun message";
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
                    setContextMenu(conversation.isTeamConversation() ? teamContextMenu(conversation.getTeam()) : null);
                }
            }
        });

        userListView.setOnMouseClicked(event -> {
            if (event.getButton() != javafx.scene.input.MouseButton.PRIMARY) return;
            ConversationPreviewDto selected = userListView.getSelectionModel().getSelectedItem();
            if (selected == null || selectionListener == null) return;
            if (selected.isTeamConversation()) {
                selectionListener.onTeamSelected(selected.getTeam());
            } else {
                UserDto userDto = new UserDto(selected.getUserId(), selected.getUsername(), selected.getIsOnline());
                selectionListener.onContactSelected(userDto);
            }
        });
    }

    private ContextMenu teamContextMenu(TeamDto team) {
        MenuItem info = new MenuItem("Infos de l'équipe");
        info.setOnAction(e -> openTeamInfo(team));
        return new ContextMenu(info);
    }

    // ================================================================
    // 🔑 ÉQUIPES
    // ================================================================

    @FXML
    private void handleNewTeam() {
        TeamDialogs.showCreateTeam(userListView.getScene().getWindow(), team -> {
            upsertTeam(team);
            if (selectionListener != null) selectionListener.onTeamSelected(team);
        });
    }

    public void openTeamInfo(TeamDto team) {
        TeamDialogs.showTeamInfo(userListView.getScene().getWindow(), team,
                this::upsertTeam,
                () -> {
                    removeTeam(team.getId());
                    if (selectionListener != null) selectionListener.onTeamLeft(team.getId());
                });
    }

    /** Charge (ou recharge) toutes les équipes de l'utilisateur. */
    public void setTeams(List<TeamDto> teams) {
        conversationList.removeIf(ConversationPreviewDto::isTeamConversation);
        teams.forEach(t -> conversationList.add(ConversationPreviewDto.ofTeam(t)));
        sortByRecentActivity();
        applyFilter();
    }

    /** Ajoute une équipe ou remplace sa version (création, ajout/retrait de membres). */
    public void upsertTeam(TeamDto team) {
        Optional<ConversationPreviewDto> existing = findTeam(team.getId());
        if (existing.isPresent()) {
            // Garder l'aperçu local s'il est plus récent que celui fourni
            LastMessageDto local = existing.get().getLastMessage();
            if (team.getLastMessage() == null && local != null) team.setLastMessage(local);
            existing.get().setTeam(team);
        } else {
            conversationList.add(0, ConversationPreviewDto.ofTeam(team));
        }
        applyFilter();
        userListView.refresh();
    }

    public void removeTeam(Long teamId) {
        unreadMessages.remove(teamKey(teamId));
        conversationList.removeIf(c -> c.isTeamConversation() && c.getTeam().getId().equals(teamId));
        applyFilter();
    }

    public TeamDto getTeam(Long teamId) {
        return findTeam(teamId).map(ConversationPreviewDto::getTeam).orElse(null);
    }

    public void addUnreadTeamMessage(Long teamId, MessageType type) {
        unreadMessages.computeIfAbsent(teamKey(teamId), k -> new ArrayList<>()).add(type);
        refreshAfterUnreadChange();
    }

    public void clearTeamUnread(Long teamId) {
        unreadMessages.remove(teamKey(teamId));
        refreshAfterUnreadChange();
    }

    public boolean hasTeamUnread(Long teamId) {
        List<MessageType> list = unreadMessages.get(teamKey(teamId));
        return list != null && !list.isEmpty();
    }

    public void updateTeamLastMessage(Long teamId, String text, Instant timestamp, boolean sentByMe) {
        findTeam(teamId).ifPresent(c -> {
            c.setLastMessage(new LastMessageDto(
                    text != null ? text : "",
                    timestamp != null ? timestamp : Instant.now(),
                    sentByMe));
            userListView.refresh();
        });
    }

    public void moveTeamToTop(Long teamId) {
        findTeam(teamId).ifPresent(this::moveItemToTop);
    }

    private Optional<ConversationPreviewDto> findTeam(Long teamId) {
        return conversationList.stream()
                .filter(c -> c.isTeamConversation() && c.getTeam().getId().equals(teamId))
                .findFirst();
    }

    private Optional<ConversationPreviewDto> findUser(Long userId) {
        return conversationList.stream()
                .filter(c -> !c.isTeamConversation() && Objects.equals(c.getUserId(), userId))
                .findFirst();
    }

    // ================================================================
    // 🔑 GESTION DES NOTIFICATIONS
    // ================================================================

    /** Ajouter un message non lu pour un contact */
    public void addUnreadMessage(Long senderId, MessageType type) {
        unreadMessages.computeIfAbsent(userKey(senderId), k -> new ArrayList<>()).add(type);
        refreshAfterUnreadChange();
    }

    /** Effacer les non lus d'un contact (quand on ouvre la conversation) */
    public void clearUnread(Long userId) {
        unreadMessages.remove(userKey(userId));
        refreshAfterUnreadChange();
    }

    public boolean hasUnread(Long userId) {
        List<MessageType> list = unreadMessages.get(userKey(userId));
        return list != null && !list.isEmpty();
    }

    private void refreshAfterUnreadChange() {
        Platform.runLater(() -> {
            if (activeFilter == Filter.UNREAD) applyFilter();
            userListView.refresh();
        });
    }

    /** Nom d'un contact (null s'il n'est pas dans la liste) */
    public String getUsername(Long userId) {
        return findUser(userId).map(ConversationPreviewDto::getUsername).orElse(null);
    }

    /** Nombre total de messages non lus */
    public int getTotalUnreadCount() {
        return unreadMessages.values().stream().mapToInt(List::size).sum();
    }

    /** Tous les types de messages non lus (pour calculer la couleur globale) */
    public List<MessageType> getAllUnreadTypes() {
        return unreadMessages.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    // ================================================================
    // AVATAR avec badge
    // ================================================================

    private StackPane createAvatar(ConversationPreviewDto conversation) {
        StackPane avatarContainer = new StackPane();
        avatarContainer.setPrefSize(45, 45);
        avatarContainer.setMinSize(45, 45);
        avatarContainer.setMaxSize(45, 45);

        // Cercle de fond
        Circle background = new Circle(22.5);
        background.setFill(getColorForUser(conversation.getUsername()));
        avatarContainer.getChildren().add(background);

        if (conversation.isTeamConversation()) {
            // Équipe : icône de groupe, pas d'indicateur de présence
            FontIcon groupIcon = new FontIcon("fas-users");
            groupIcon.setIconSize(18);
            groupIcon.setIconColor(Color.WHITE);
            avatarContainer.getChildren().add(groupIcon);
        } else {
            // Initiales
            Label initialsLabel = new Label(getInitials(conversation.getUsername()));
            initialsLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");

            // Indicateur de statut
            Circle statusIndicator = new Circle(6);
            statusIndicator.setFill(Boolean.TRUE.equals(conversation.getIsOnline()) ? Color.rgb(0, 200, 0) : Color.GRAY);
            statusIndicator.setStroke(Color.WHITE);
            statusIndicator.setStrokeWidth(2);
            StackPane.setAlignment(statusIndicator, Pos.BOTTOM_RIGHT);

            avatarContainer.getChildren().addAll(initialsLabel, statusIndicator);
        }

        // 🔑 Badge de notification (messages non lus)
        List<MessageType> unread = unreadMessages.getOrDefault(key(conversation), Collections.emptyList());
        if (!unread.isEmpty()) {
            int count = unread.size();
            Color badgeColor = calculateBadgeColor(new HashSet<>(unread));

            Circle badgeBg = new Circle(11);
            badgeBg.setFill(badgeColor);
            badgeBg.setStroke(Color.WHITE);
            badgeBg.setStrokeWidth(2);

            Label badgeLabel = new Label(count > 99 ? "99+" : String.valueOf(count));
            badgeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: bold;");

            StackPane badge = new StackPane(badgeBg, badgeLabel);
            badge.setPrefSize(22, 22);
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);

            avatarContainer.getChildren().add(badge);
        }

        return avatarContainer;
    }

    // ================================================================
    // COULEURS
    // ================================================================

    /** Ordre de gravité : le plus inquiétant en premier. */
    private static final List<MessageType> PRIORITY = List.of(
            MessageType.ALERTE,
            MessageType.INCIDENT,
            MessageType.URGENT,
            MessageType.SECURITE,
            MessageType.MAINTENANCE,
            MessageType.DEMANDE_DE_SUPPORT,
            MessageType.FILE,
            MessageType.IMAGE,
            MessageType.TEXT
    );

    /** Type le plus grave parmi ceux fournis (null si aucun). */
    public static MessageType mostUrgent(Collection<MessageType> types) {
        return types.stream()
                .filter(Objects::nonNull)
                .min(Comparator.comparingInt(t -> {
                    int idx = PRIORITY.indexOf(t);
                    return idx >= 0 ? idx : PRIORITY.size(); // type inconnu → le moins prioritaire
                }))
                .orElse(null);
    }

    /** Couleur du badge : celle du message le plus grave */
    public static Color calculateBadgeColor(Set<MessageType> types) {
        MessageType top = mostUrgent(types);
        return top != null ? getColorForType(top) : Color.GRAY;
    }

    public static Color getColorForType(MessageType type) {
        return switch (type) {
            case TEXT               -> Color.rgb(100, 149, 237); // Bleu
            case INCIDENT           -> Color.rgb(255, 68, 68);   // Rouge
            case URGENT             -> Color.rgb(255, 102, 0);   // Orange
            case ALERTE             -> Color.rgb(204, 0, 0);     // Rouge foncé
            case MAINTENANCE        -> Color.rgb(255, 170, 0);   // Jaune
            case SECURITE           -> Color.rgb(255, 153, 0);   // Orange clair
            case IMAGE              -> Color.rgb(156, 39, 176);  // Violet
            case FILE               -> Color.rgb(96, 125, 139);  // Gris-bleu
            case DEMANDE_DE_SUPPORT -> Color.rgb(76, 175, 80);   // Vert
        };
    }

    // ================================================================
    // RECHERCHE & FILTRES
    // ================================================================

    private void setupSearchField() {
        searchFiedUser.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
    }

    @FXML private void showAll()    { setFilter(Filter.ALL); }
    @FXML private void showUnread() { setFilter(Filter.UNREAD); }
    @FXML private void showTeams()  { setFilter(Filter.TEAMS); }

    private void setFilter(Filter filter) {
        activeFilter = filter;
        updateFilterButtons();
        applyFilter();
    }

    private void updateFilterButtons() {
        Map<Filter, Button> buttons = new EnumMap<>(Filter.class);
        if (filterAllBtn != null) buttons.put(Filter.ALL, filterAllBtn);
        if (filterUnreadBtn != null) buttons.put(Filter.UNREAD, filterUnreadBtn);
        if (filterTeamsBtn != null) buttons.put(Filter.TEAMS, filterTeamsBtn);
        buttons.forEach((f, btn) -> {
            btn.getStyleClass().remove("filter-btn-active");
            if (f == activeFilter) btn.getStyleClass().add("filter-btn-active");
        });
    }

    /** Réapplique le filtre actif et la recherche sur la liste complète. */
    private void applyFilter() {
        String searchText = searchFiedUser != null ? searchFiedUser.getText().toLowerCase().trim() : "";
        List<ConversationPreviewDto> filtered = conversationList.stream()
                .filter(c -> switch (activeFilter) {
                    case ALL -> true;
                    case TEAMS -> c.isTeamConversation();
                    case UNREAD -> unreadMessages.containsKey(key(c));
                })
                .filter(c -> {
                    if (searchText.isEmpty()) return true;
                    boolean matchName = c.getUsername() != null && c.getUsername().toLowerCase().contains(searchText);
                    boolean matchMsg = c.getLastMessage() != null && c.getLastMessage().getContent() != null &&
                            c.getLastMessage().getContent().toLowerCase().contains(searchText);
                    return matchName || matchMsg;
                })
                .collect(Collectors.toList());
        filteredConversations.setAll(filtered);
        userListView.setPlaceholder(new Label(activeFilter == Filter.TEAMS
                ? "Aucune équipe. Cliquez sur ＋ pour en créer une."
                : "Aucune conversation"));
    }

    // ================================================================
    // UTILITAIRES
    // ================================================================

    private String formatTimestamp(Instant timestamp) {
        LocalDate messageDate = timestamp.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate today = LocalDate.now();
        return messageDate.equals(today)
                ? TIME_FORMATTER.format(timestamp)
                : DATE_FORMATTER.format(timestamp);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
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
        return colors[Math.abs(Objects.hashCode(username)) % colors.length];
    }

    /** Remplace les conversations privées (les équipes déjà chargées sont conservées). */
    public void updateConversations(List<ConversationPreviewDto> conversations) {
        conversationList.removeIf(c -> !c.isTeamConversation());
        conversationList.addAll(conversations);
        sortByRecentActivity();
        applyFilter();
    }

    /** Conversations (privées et équipes) de la plus récente à la plus ancienne. */
    private void sortByRecentActivity() {
        FXCollections.sort(conversationList, Comparator.comparing(
                (ConversationPreviewDto c) -> c.getLastMessage() != null ? c.getLastMessage().getTimestamp() : null,
                Comparator.nullsLast(Comparator.reverseOrder())));
    }

    public void updateOrCreateUser(UserDto update) {
        findUser(update.getId()).ifPresent(c -> {
            c.setIsOnline(update.getIsOnline());
            userListView.refresh();
        });
    }
    /** Mettre à jour l'aperçu du dernier message d'un contact */
    public void updateLastMessage(Long userId, String text, Instant timestamp, boolean sentByMe) {
        findUser(userId).ifPresent(c -> {
            c.setLastMessage(new LastMessageDto(
                    text != null ? text : "",
                    timestamp != null ? timestamp : Instant.now(),
                    sentByMe));
            userListView.refresh();
        });
    }

    /** 🔑 Remonter un contact en haut de la liste (quand message reçu) */
    public void moveToTop(Long userId) {
        findUser(userId).ifPresent(this::moveItemToTop);
    }

    private void moveItemToTop(ConversationPreviewDto c) {
        conversationList.remove(c);
        conversationList.add(0, c);
        // Réappliquer le filtre et la recherche
        applyFilter();
        System.out.println("⬆️ " + c.getUsername() + " remonté en haut");
    }
}
