package com.mining.minecom.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class SidebarController implements Initializable {

    @FXML private UserListController userListController;
    @FXML private NavBarController navBarController; // 🔑 AJOUT

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialization si nécessaire
    }

    public UserListController getUserListController() {
        return userListController;
    }

    // 🔑 NOUVEAU : Accès au NavBarController
    public NavBarController getNavBarController() {
        return navBarController;
    }
}