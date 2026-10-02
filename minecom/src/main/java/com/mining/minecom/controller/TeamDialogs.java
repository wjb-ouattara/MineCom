package com.mining.minecom.controller;

import com.mining.minecom.common.dto.UserDto;
import com.mining.minecom.service.AuthService;
import com.mining.minecom.service.TeamService;
import com.mining.minecom.service.UserService;
import com.mining.minecom_server.common.dto.TeamDto;
import com.mining.minecom_server.common.dto.TeamMemberDto;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.*;
import java.util.function.Consumer;

/**
 * Boîtes de dialogue des équipes : création, infos et gestion des membres.
 * Les appels réseau sont faits hors du thread JavaFX ; les callbacks sont rappelés sur le thread JavaFX.
 */
public final class TeamDialogs {

    private static final TeamService teamService = new TeamService();
    private static final UserService userService = new UserService();

    private TeamDialogs() {}

    // ================================================================
    // CRÉER UNE ÉQUIPE
    // ================================================================

    public static void showCreateTeam(Window owner, Consumer<TeamDto> onCreated) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Nouvelle équipe");
        dialog.setHeaderText("👥 Créer une équipe\nSeuls les membres pourront voir et envoyer des messages.");

        ButtonType createType = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createType, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.setPromptText("Nom de l'équipe (ex : Équipe de nuit – Galerie B)");
        TextField descField = new TextField();
        descField.setPromptText("Description (facultatif)");

        MemberPicker picker = new MemberPicker(Set.of(AuthService.getCurrentUserId()));

        VBox content = new VBox(8,
                new Label("Nom"), nameField,
                new Label("Description"), descField,
                new Label("Membres"), picker.getNode());
        content.setPadding(new Insets(10));
        content.setPrefWidth(420);
        dialog.getDialogPane().setContent(content);

        Button createBtn = (Button) dialog.getDialogPane().lookupButton(createType);
        createBtn.disableProperty().bind(nameField.textProperty().isEmpty());
        Platform.runLater(nameField::requestFocus);

        dialog.showAndWait()
                .filter(bt -> bt == createType)
                .ifPresent(bt -> {
                    String name = nameField.getText().trim();
                    String desc = descField.getText().trim();
                    List<Long> ids = picker.getSelectedIds();
                    runAsync(() -> teamService.createTeam(name, desc, ids), owner, onCreated);
                });
    }

    // ================================================================
    // INFOS / GESTION D'UNE ÉQUIPE
    // ================================================================

    /**
     * @param onUpdated appelé avec la nouvelle version de l'équipe après un ajout/retrait de membre
     * @param onLeft    appelé quand l'utilisateur a quitté l'équipe
     */
    public static void showTeamInfo(Window owner, TeamDto team, Consumer<TeamDto> onUpdated, Runnable onLeft) {
        Long me = AuthService.getCurrentUserId();
        boolean iAmAdmin = team.getMembers().stream()
                .anyMatch(m -> m.getUserId().equals(me) && m.isAdmin());

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Infos de l'équipe");
        dialog.setHeaderText("👥 " + team.getName()
                + (team.getDescription() != null && !team.getDescription().isBlank()
                ? "\n" + team.getDescription() : ""));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox membersBox = new VBox(4);
        for (TeamMemberDto member : team.getMembers()) {
            membersBox.getChildren().add(memberRow(member, me, iAmAdmin, () -> {
                dialog.close();
                runAsync(() -> {
                    teamService.removeMember(team.getId(), member.getUserId());
                    return null;
                }, owner, ignored -> {});
            }));
        }
        ScrollPane scroll = new ScrollPane(membersBox);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(260);

        Label title = new Label(team.getMembers().size() + " membre(s)");
        title.setStyle("-fx-font-weight: bold;");

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        if (iAmAdmin) {
            Button addBtn = new Button("Ajouter des membres", icon("fas-user-plus", "#2196F3"));
            addBtn.setOnAction(e -> {
                dialog.close();
                showAddMembers(owner, team, onUpdated);
            });
            actions.getChildren().add(addBtn);
        }
        Button leaveBtn = new Button("Quitter l'équipe", icon("fas-sign-out-alt", "#DC2626"));
        leaveBtn.setOnAction(e -> {
            if (!confirm(owner, "Quitter l'équipe « " + team.getName() + " » ?",
                    "Vous ne recevrez plus ses messages et n'aurez plus accès à l'historique.")) return;
            dialog.close();
            runAsync(() -> {
                teamService.removeMember(team.getId(), me);
                return null;
            }, owner, ignored -> onLeft.run());
        });
        actions.getChildren().add(leaveBtn);

        VBox content = new VBox(10, title, scroll, actions);
        content.setPadding(new Insets(10));
        content.setPrefWidth(400);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private static void showAddMembers(Window owner, TeamDto team, Consumer<TeamDto> onUpdated) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Ajouter des membres");
        dialog.setHeaderText("👥 " + team.getName());

        ButtonType addType = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        Set<Long> alreadyIn = new HashSet<>();
        team.getMembers().forEach(m -> alreadyIn.add(m.getUserId()));
        MemberPicker picker = new MemberPicker(alreadyIn);

        VBox content = new VBox(8, picker.getNode());
        content.setPadding(new Insets(10));
        content.setPrefWidth(400);
        dialog.getDialogPane().setContent(content);

        dialog.showAndWait()
                .filter(bt -> bt == addType)
                .ifPresent(bt -> {
                    List<Long> ids = picker.getSelectedIds();
                    if (ids.isEmpty()) return;
                    runAsync(() -> teamService.addMembers(team.getId(), ids), owner, onUpdated);
                });
    }

    private static HBox memberRow(TeamMemberDto member, Long me, boolean iAmAdmin, Runnable onRemove) {
        boolean isMe = member.getUserId().equals(me);

        Region dot = new Region();
        dot.setMinSize(8, 8);
        dot.setMaxSize(8, 8);
        dot.setStyle("-fx-background-radius: 4; -fx-background-color: "
                + (Boolean.TRUE.equals(member.getIsOnline()) ? "#00C800" : "#999") + ";");

        Label name = new Label(member.getUsername() + (isMe ? " (vous)" : ""));
        name.setStyle("-fx-font-size: 13px;");

        HBox row = new HBox(8, dot, name);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 6, 4, 6));

        if (member.isAdmin()) {
            Label admin = new Label("Admin");
            admin.setStyle("-fx-background-color: rgba(255,140,0,0.15); -fx-text-fill: #FF8C00; "
                    + "-fx-font-size: 10px; -fx-padding: 1 6; -fx-background-radius: 8;");
            row.getChildren().add(admin);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().add(spacer);

        if (iAmAdmin && !isMe) {
            Button remove = new Button("Retirer");
            remove.setStyle("-fx-font-size: 11px; -fx-text-fill: #DC2626;");
            remove.setOnAction(e -> {
                if (confirm(remove.getScene().getWindow(), "Retirer " + member.getUsername() + " de l'équipe ?",
                        "Ce membre n'aura plus accès aux messages de l'équipe.")) {
                    onRemove.run();
                }
            });
            row.getChildren().add(remove);
        }
        return row;
    }

    // ================================================================
    // SÉLECTION DE MEMBRES (cases à cocher + recherche)
    // ================================================================

    private static final class MemberPicker {
        private final ObservableList<UserDto> users = FXCollections.observableArrayList();
        private final Map<Long, BooleanProperty> selected = new HashMap<>();
        private final VBox node;

        MemberPicker(Set<Long> excludedIds) {
            TextField search = new TextField();
            search.setPromptText("Rechercher un utilisateur...");

            FilteredList<UserDto> filtered = new FilteredList<>(users, u -> true);
            search.textProperty().addListener((obs, o, text) -> {
                String q = text.toLowerCase().trim();
                filtered.setPredicate(u -> q.isEmpty() || u.getUsername().toLowerCase().contains(q));
            });

            ListView<UserDto> list = new ListView<>(filtered);
            list.setPrefHeight(240);
            list.setPlaceholder(new Label("Chargement des utilisateurs..."));
            list.setCellFactory(CheckBoxListCell.forListView(
                    u -> selected.computeIfAbsent(u.getId(), id -> new SimpleBooleanProperty(false)),
                    new javafx.util.StringConverter<>() {
                        @Override public String toString(UserDto u) {
                            return u == null ? "" : u.getUsername()
                                    + (Boolean.TRUE.equals(u.getIsOnline()) ? "  ● en ligne" : "");
                        }
                        @Override public UserDto fromString(String s) { return null; }
                    }));

            node = new VBox(6, search, list);

            new Thread(() -> {
                List<UserDto> contacts = userService.getContacts();
                Platform.runLater(() -> {
                    contacts.stream()
                            .filter(u -> !excludedIds.contains(u.getId()))
                            .sorted(Comparator.comparing(UserDto::getUsername, String.CASE_INSENSITIVE_ORDER))
                            .forEach(users::add);
                    list.setPlaceholder(new Label("Aucun utilisateur disponible"));
                });
            }, "team-contacts").start();
        }

        Node getNode() { return node; }

        List<Long> getSelectedIds() {
            List<Long> ids = new ArrayList<>();
            selected.forEach((id, prop) -> { if (prop.get()) ids.add(id); });
            return ids;
        }
    }

    // ================================================================
    // UTILITAIRES
    // ================================================================

    private interface Call<T> { T run() throws Exception; }

    private static <T> void runAsync(Call<T> call, Window owner, Consumer<T> onSuccess) {
        new Thread(() -> {
            try {
                T result = call.run();
                Platform.runLater(() -> onSuccess.accept(result));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.initOwner(owner);
                    alert.setTitle("Équipe");
                    alert.setHeaderText("⛏️ Opération impossible");
                    alert.setContentText(e.getMessage());
                    alert.show();
                });
            }
        }, "team-action").start();
    }

    private static boolean confirm(Window owner, String header, String text) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.setTitle("Équipe");
        alert.setHeaderText(header);
        alert.setContentText(text);
        return alert.showAndWait().filter(bt -> bt == ButtonType.OK).isPresent();
    }

    private static FontIcon icon(String literal, String color) {
        FontIcon icon = new FontIcon(literal);
        icon.setIconSize(13);
        icon.setIconColor(Color.web(color));
        return icon;
    }
}
