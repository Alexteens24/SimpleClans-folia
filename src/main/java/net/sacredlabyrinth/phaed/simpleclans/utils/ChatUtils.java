package net.sacredlabyrinth.phaed.simpleclans.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.sacredlabyrinth.phaed.simpleclans.SimpleClans.lang;

public class ChatUtils {

    public static boolean HEX_COLOR_SUPPORT;

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("(%([A-Za-z]+)%)");
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("&#([0-9A-Fa-f]{6})");
    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("[&§][0-9a-fA-Fk-orK-OR]");
    private static final Pattern HEX_STRIP_COLOR_PATTERN = Pattern.compile("([&§]#[0-9A-Fa-f]{6})|([&§][0-9a-fA-Fk-orK-OR])|([&§]x([&§][a-fA-F0-9]){6})");
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    static {
        HEX_COLOR_SUPPORT = true;
    }

    private ChatUtils() {
    }

    public static String getColorByChar(char character) {
        return LegacyColor.getByChar(character);
    }

    public static String parseColors(@NotNull String text) {
        // Special thanks to the Spigot community!
        // https://www.spigotmc.org/threads/hex-color-code-translate.449748/#post-3867795
        if (HEX_COLOR_SUPPORT) {
            Matcher matcher = HEX_COLOR_PATTERN.matcher(text);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(buffer, LegacyColor.hexToColor(matcher.group(1)));
            }
            text = matcher.appendTail(buffer).toString();
        }
        return LegacyColor.translateAlternateColorCodes('&', text);
    }

    public static String stripColors(String text) {
        Pattern pattern = HEX_COLOR_SUPPORT ? HEX_STRIP_COLOR_PATTERN : STRIP_COLOR_PATTERN;
        Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, "");
        }
        return matcher.appendTail(buffer).toString();
    }

    public static String getLastColorCode(String msg) {
        if (msg.length() < 2) {
            return "";
        }

        String one = msg.substring(msg.length() - 2, msg.length() - 1);
        String two = msg.substring(msg.length() - 1);

        if (one.equals("§")) {
            return one + two;
        }

        if (one.equals("&")) {
            return getColorByChar(two.charAt(0));
        }

        return "";
    }

    public static @NotNull Component toComponent(@Nullable CommandSender receiver, @NotNull String text) {
        TextComponent.Builder builder = Component.text();
        ArrayList<String> placeholders = new ArrayList<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        while (matcher.find()) {
            placeholders.add(matcher.group(0));
        }
        String[] split = PLACEHOLDER_PATTERN.split(text);
        for (int i = 0; i < split.length; i++) {
            if (!split[i].isEmpty()) {
                builder.append(LEGACY_SECTION.deserialize(split[i]));
            }
            if (i >= placeholders.size()) {
                continue;
            }
            appendPlaceholder(receiver, builder, placeholders.get(i));
        }

        return builder.build();
    }

    public static @NotNull Component toLegacyComponent(@NotNull String text) {
        return LEGACY_SECTION.deserialize(text);
    }

    private static void appendPlaceholder(@Nullable CommandSender receiver, TextComponent.Builder builder, String placeholder) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(placeholder);
        if (!matcher.find()) {
            return;
        }
        placeholder = matcher.group(2);
        builder.append(LEGACY_SECTION.deserialize(lang("clickable." + placeholder, receiver))
                .clickEvent(ClickEvent.runCommand("/" + placeholder))
                .hoverEvent(HoverEvent.showText(LEGACY_SECTION.deserialize(
                        lang("hover.click.to." + placeholder, receiver)))));
    }

    /**
     * Loops through the input and returns the last color codes
     *
     * @param input the input
     * @return the last color codes
     */
    @NotNull
    public static String getLastColors(@NotNull String input) {
        StringBuilder result = new StringBuilder();
        int length = input.length();

        for (int index = length - 1; index > -1; index--) {
            boolean found = false;
            String color = String.valueOf(input.charAt(index));
            if (LegacyColor.ALL_CODES.contains(color)) {
                if (index - 1 >= 0) {
                    char section = input.charAt(index - 1);
                    if (section == LegacyColor.COLOR_CHAR) {
                        index--;
                        result.insert(0, section + color);
                        found = true;
                    }
                }
            }
            if (!found && result.length() != 0) {
                break;
            }
        }

        return result.toString();
    }

    public static void applyLastColorToFollowingLines(@NotNull List<String> lines) {
        if (lines.get(0).isEmpty() || lines.get(0).charAt(0) != LegacyColor.COLOR_CHAR) {
            lines.set(0, LegacyColor.WHITE + lines.get(0));
        }
        for (int i = 1; i < lines.size(); i++) {
            final String pLine = lines.get(i - 1);
            final String subLine = lines.get(i);

            if (subLine.isEmpty() || subLine.charAt(0) != LegacyColor.COLOR_CHAR) {
                lines.set(i, getLastColors(pLine) + subLine);
            }
        }
    }

}
