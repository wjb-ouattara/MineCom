module com.mining.minecom.common {
    // ========================================
    // DÉPENDANCES
    // ========================================
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires jakarta.validation;

    // ========================================
    // EXPORTS (Rendre les packages visibles)
    // ========================================
    exports com.mining.minecom.common.dto;
    exports com.mining.minecom.common.enums;
    exports com.mining.minecom_server.common.dto;
    exports com.mining.minecom_server.common.enums;

    // ========================================
    // OPENS (pour Jackson et la réflexion)
    // ========================================
    opens com.mining.minecom.common.dto to com.fasterxml.jackson.databind;
    opens com.mining.minecom.common.enums to com.fasterxml.jackson.databind;
    opens com.mining.minecom_server.common.dto to com.fasterxml.jackson.databind;
    opens com.mining.minecom_server.common.enums to com.fasterxml.jackson.databind;
}