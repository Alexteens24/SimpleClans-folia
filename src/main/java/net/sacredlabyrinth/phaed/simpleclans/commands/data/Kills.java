package net.sacredlabyrinth.phaed.simpleclans.commands.data;

import net.sacredlabyrinth.phaed.simpleclans.ChatBlock;
import net.sacredlabyrinth.phaed.simpleclans.Helper;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import net.sacredlabyrinth.phaed.simpleclans.utils.LegacyColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static net.sacredlabyrinth.phaed.simpleclans.SimpleClans.lang;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.PAGE_CLAN_NAME_COLOR;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.PAGE_SEPARATOR;

public class Kills extends Sendable {

    private final Player player;
    private final String polled;

    public Kills(@NotNull SimpleClans plugin, @NotNull Player player, @NotNull String polled) {
        super(plugin, player);
        this.player = player;
        this.polled = polled;
    }

    @Override
    public void send() {
        plugin.getStorageManager().getKillsPerPlayer(polled, data -> plugin.getFoliaScheduler().runAtEntity(player, () -> {
            if (data.isEmpty()) {
                ChatBlock.sendMessage(player, LegacyColor.RED + lang("nokillsfound", player));
                return;
            }
            configureAndSendHeader();
            addLines(data);

            sendBlock();
        }));
    }

    private void addLines(Map<String, Integer> data) {
        Map<String, Integer> killsPerPlayer = Helper.sortByValue(data);

        for (Map.Entry<String, Integer> playerKills : killsPerPlayer.entrySet()) {
            int count = playerKills.getValue();
            chatBlock.addRow("  " + playerKills.getKey(), LegacyColor.AQUA + "" + count);
        }
    }

    private void configureAndSendHeader() {
        chatBlock.setFlexibility(true, false);
        chatBlock.setAlignment("l", "c");
        chatBlock.addRow("  " + headColor + lang("victim", player), lang("killcount", player));
        ChatBlock.saySingle(player, sm.getColored(PAGE_CLAN_NAME_COLOR) + polled + subColor
                + " " + lang("kills", player) + " " + headColor +
                Helper.generatePageSeparator(sm.getString(PAGE_SEPARATOR)));
        ChatBlock.sendBlank(player);
    }
}
