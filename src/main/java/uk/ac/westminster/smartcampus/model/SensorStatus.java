package uk.ac.westminster.smartcampus.model;

import java.util.Locale;

public final class SensorStatus {

    public static final String ACTIVE = "ACTIVE";
    public static final String MAINTENANCE = "MAINTENANCE";
    public static final String OFFLINE = "OFFLINE";

    private SensorStatus() {
    }

    public static String normalize(String status) {
        return status == null ? null : status.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isValid(String status) {
        return ACTIVE.equals(status) || MAINTENANCE.equals(status) || OFFLINE.equals(status);
    }

    public static boolean isActive(String status) {
        return ACTIVE.equals(normalize(status));
    }

    public static boolean isMaintenance(String status) {
        return MAINTENANCE.equals(normalize(status));
    }
}
