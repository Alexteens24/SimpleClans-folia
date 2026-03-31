package net.sacredlabyrinth.phaed.simpleclans.chat.handlers;

import net.sacredlabyrinth.phaed.simpleclans.ChatBlock;
import net.sacredlabyrinth.phaed.simpleclans.ClanPlayer;
import net.sacredlabyrinth.phaed.simpleclans.chat.ChatHandler;
import net.sacredlabyrinth.phaed.simpleclans.chat.SCMessage;
import net.sacredlabyrinth.phaed.simpleclans.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static net.sacredlabyrinth.phaed.simpleclans.chat.SCMessage.Source;
import static net.sacredlabyrinth.phaed.simpleclans.chat.SCMessage.Source.*;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.PERFORMANCE_USE_BUNGEECORD;

/**
 * Handles delivering messages from {@link Source#SPIGOT} or {@link Source#DISCORD} to internal spy chat.
 */
public class SpyChatHandler implements ChatHandler {

    @Override
    public void sendMessage(SCMessage message) {
        ConfigField formatField = ConfigField.valueOf(String.format("%sCHAT_SPYFORMAT",
                message.getSource() == DISCORD ? "DISCORD" : message.getChannel()));
        String format = settingsManager.getString(formatField);
        message.setContent(ChatUtils.stripColors(message.getContent()));
        String formattedMessage = chatManager.parseChatFormat(format, message);

        Set<UUID> directReceivers = message.getReceivers().stream()
                .map(ClanPlayer::getUniqueId)
                .collect(Collectors.toSet());

        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getFoliaScheduler().runAtEntity(player, () -> {
                if (!permissionsManager.has(player, "simpleclans.admin.all-seeing-eye")) {
                    return;
                }

                ClanPlayer clanPlayer = plugin.getClanManager().getCreateClanPlayer(player.getUniqueId());
                if (clanPlayer == null || clanPlayer.isMuted() || directReceivers.contains(clanPlayer.getUniqueId())) {
                    return;
                }

                ChatBlock.sendMessage(clanPlayer, formattedMessage);
            });
        }
    }

    @Override
    public boolean canHandle(SCMessage.Source source) {
        return source == SPIGOT || (source == PROXY && settingsManager.is(PERFORMANCE_USE_BUNGEECORD))
                || (source == DISCORD && chatManager.isDiscordHookEnabled());
    }
}
