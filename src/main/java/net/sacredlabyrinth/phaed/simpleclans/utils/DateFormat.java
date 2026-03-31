package net.sacredlabyrinth.phaed.simpleclans.utils;

import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.DATE_TIME_PATTERN;

public class DateFormat {

    private static final SimpleClans plugin = SimpleClans.getInstance();
    private static final DateTimeFormatter formatter;

    static {
        String pattern = plugin.getSettingsManager().getString(DATE_TIME_PATTERN);
        DateTimeFormatter f;
        try {
            f = DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning(String.format("%s is not a valid pattern!", pattern));
            f = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy").withZone(ZoneId.systemDefault());
        }
        formatter = f;
    }

    public static String formatDateTime(long date) {
        return formatter.format(Instant.ofEpochMilli(date));
    }

}
