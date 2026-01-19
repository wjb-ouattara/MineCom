package com.mining.minecom.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.util.Objects;

public class HomeController {

    // Déclarations des éléments FXML

    @FXML
    private StackPane chatPane;

    @FXML
    private ImageView bgImage;

    @FXML
    private TextField messageField;


    @FXML
    public void initialize() {

        // --- LOGIQUE DE L'IMAGE DE FOND ---
        Image image = new Image(
                Objects.requireNonNull(
                        getClass().getResource("/images/bg1-mine.jpg")
                ).toExternalForm()
        );

        bgImage.setImage(image);

        // Cette logique lie la taille de l'image à la taille du StackPane parent
        bgImage.setPreserveRatio(false);
        bgImage.setManaged(false);
        bgImage.fitWidthProperty().bind(chatPane.widthProperty());
        bgImage.fitHeightProperty().bind(chatPane.heightProperty());
        // ----------------------------------
    }
}