package net.sacredlabyrinth.phaed.simpleclans.chat.handlers;

import net.sacredlabyrinth.phaed.simpleclans.ChatBlock;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.simpleclans.chat.ChatHandler;
import net.sacredlabyrinth.phaed.simpleclans.chat.SCMessage;
import net.sacredlabyrinth.phaed.simpleclans.events.ChatEvent;
import net.sacredlabyrinth.phaed.simpleclans.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import static net.sacredlabyrinth.phaed.simpleclans.chat.SCMessage.Source.*;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.PERFORMANCE_USE_BUNGEECORD;
import static org.bukkit.Bukkit.getPluginManager;

@SuppressWarnings("unused")
public class SpigotChatHandler implements ChatHandler {

    @Override
    public void sendMessage(SCMessage message) {
        Player sender = message.getSender().toPlayer();
        if (sender == null) {
            message.setContent(stripColorsAndFormatsPerPermission(null, message.getContent()));
            dispatchMessage(message);
            return;
        }

        plugin.getFoliaScheduler().runAtEntity(sender, () -> {
            String sanitizedMessage = stripColorsAndFormatsPerPermission(sender, message.getContent());
            plugin.getFoliaScheduler().executeGlobal(() -> {
                message.setContent(sanitizedMessage);
                dispatchMessage(message);
            });
        });
    }

    private void dispatchMessage(SCMessage message) {
        ChatEvent event = new ChatEvent(message.getContent(), message.getSender(), message.getReceivers(),
                ChatEvent.Type.valueOf(message.getChannel().name()));

        getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        message.setContent(event.getMessage());

        ConfigField configField = ConfigField.valueOf(String.format("%sCHAT_FORMAT",
                message.getSource() == DISCORD ? "DISCORD" : message.getChannel()));

        String format = settingsManager.getString(configField);
        String formattedMessage = chatManager.parseChatFormat(format, message, event.getPlaceholders());

        plugin.getLogger().info(ChatUtils.stripColors(formattedMessage));

        for (ClanPlayer cp : message.getReceivers()) {
            ChatBlock.sendMessage(cp, formattedMessage);
        }
    }

    private String stripColorsAndFormatsPerPermission(@Nullable Player sender, String message) {
        if (!permissionsManager.has(sender, "simpleclans.member.chat.color")) {
            message = stripColors(message);
        }
        if (!permissionsManager.has(sender, "simpleclans.member.chat.format")) {
            message = stripFormats(message);
        }
        return message;
    }

    private String stripColors(String message) {
        return message.replaceAll("[§&][0-9a-fA-FxX]", "");
    }

    private String stripFormats(String message) {
        return message.replaceAll("[§&][k-orK-OR]", "");
    }

    @Override
    public boolean canHandle(SCMessage.Source source) {
        return source == SPIGOT || (source == PROXY && settingsManager.is(PERFORMANCE_USE_BUNGEECORD))
                || (source == DISCORD && chatManager.isDiscordHookEnabled());
    }
}
