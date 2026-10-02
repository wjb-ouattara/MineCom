// Dans com.mining.minecom.interfaces.ContactSelectionListener.java

package com.mining.minecom.interfaces;

import com.mining.minecom.common.dto.UserDto;
import com.mining.minecom_server.common.dto.TeamDto;

/**
 * Interface de rappel (Callback) utilisée par UserListController
 * pour notifier le DashboardController (le hub) qu'un utilisateur a été sélectionné.
 */
public interface ContactSelectionListener {

    /**
     * Appelé lorsqu'un utilisateur est sélectionné dans la liste.
     * @param user Le DTO complet de l'utilisateur sélectionné.
     */
    void onContactSelected(UserDto user);

    /** Appelé lorsqu'une équipe est sélectionnée (ou vient d'être créée). */
    void onTeamSelected(TeamDto team);

    /** Appelé quand l'utilisateur a quitté une équipe. */
    void onTeamLeft(Long teamId);
}