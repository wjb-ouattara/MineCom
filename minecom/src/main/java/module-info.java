module com.mining.minecom {
    // ========================================
    // DÉPENDANCES JAVAFX
    // ========================================
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.media;

    // ========================================
    // DÉPENDANCES RÉSEAU ET HTTP
    // ========================================
    requires java.net.http;

    // ========================================
    // DÉPENDANCES ICÔNES
    // ========================================
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires org.kordamp.ikonli.materialdesign2;

    // ========================================
    // DÉPENDANCES JACKSON (JSON)
    // ========================================
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.core;

    // ========================================
    // DÉPENDANCES SPRING WEBSOCKET
    // ========================================
    requires spring.websocket;
    requires spring.messaging;
    requires spring.core;

    // ========================================
    // 🔑 DÉPENDANCE AU MODULE COMMON (CRITIQUE!)
    // ========================================
    requires com.mining.minecom.common;

    // ========================================
    // OPENS (pour la réflexion)
    // ========================================
    opens com.mining.minecom to javafx.fxml;
    opens com.mining.minecom.service to spring.core;
    opens com.mining.minecom.controller to javafx.fxml;

    // ========================================
    // EXPORTS
    // ========================================
    exports com.mining.minecom;
    exports com.mining.minecom.controller;
    exports com.mining.minecom.service;
    exports com.mining.minecom.interfaces;
}