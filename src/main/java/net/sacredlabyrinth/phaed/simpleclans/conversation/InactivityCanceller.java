package net.sacredlabyrinth.phaed.simpleclans.conversation;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import static net.sacredlabyrinth.phaed.simpleclans.SimpleClans.lang;
import static net.sacredlabyrinth.phaed.simpleclans.utils.LegacyColor.RED;

public class InactivityCanceller {

    private final int timeoutSeconds;

    public InactivityCanceller(@NotNull Plugin plugin, int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public long getTimeoutTicks() {
        return timeoutSeconds * 20L;
    }

    public void cancelling(@NotNull SCConversation conversation) {
        Player forWhom = conversation.getContext().getForWhom();
        forWhom.sendMessage(RED + lang("you.did.not.answer.in.time", forWhom));
    }
}
