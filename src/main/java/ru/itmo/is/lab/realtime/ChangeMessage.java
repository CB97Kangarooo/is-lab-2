package ru.itmo.is.lab.realtime;

import ru.itmo.is.lab.event.DataChangedEvent;

import java.util.Locale;

public final class ChangeMessage {

    public static final String PONG = "pong";
    public static final String PING = "ping";

    private ChangeMessage() {
    }

    public static String toJson(DataChangedEvent event) {
        String entity = entityOf(event.getEntityType());
        if (entity == null || event.getChangeType() == null) {
            return null;
        }
        String change = event.getChangeType().name().toLowerCase(Locale.ROOT);
        String id = event.getEntityId() == null ? "null" : event.getEntityId().toString();
        return "{\"entity\":\"" + entity + "\",\"change\":\"" + change + "\",\"id\":" + id + "}";
    }

    static String entityOf(String entityType) {
        if ("HumanBeing".equals(entityType)) {
            return "human";
        }
        if ("Car".equals(entityType)) {
            return "car";
        }
        return null;
    }
}
