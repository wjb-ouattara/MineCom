package com.mining.minecom_server.common.enums;

public enum MessageStatus {
    SENT,      // Envoyé
    DELIVERED, // Délivré (reçu par le serveur)
    READ,      // Lu
    UNREAD     // Non lu (peut être le statut initial)
}