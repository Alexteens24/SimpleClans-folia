package net.sacredlabyrinth.phaed.simpleclans.conversation;

import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class ConversationContext {

    private final SCConversation conversation;
    private final SimpleClans plugin;
    private final Player forWhom;
    private final Map<Object, Object> sessionData;

    ConversationContext(@NotNull SCConversation conversation, @NotNull SimpleClans plugin, @NotNull Player forWhom,
                        @NotNull Map<Object, Object> sessionData) {
        this.conversation = conversation;
        this.plugin = plugin;
        this.forWhom = forWhom;
        this.sessionData = sessionData;
    }

    public @NotNull SCConversation getConversation() {
        return conversation;
    }

    public @NotNull SimpleClans getPlugin() {
        return plugin;
    }

    public @NotNull Player getForWhom() {
        return forWhom;
    }

    public @Nullable Object getSessionData(@NotNull Object key) {
        return sessionData.get(key);
    }

    public void setSessionData(@NotNull Object key, @Nullable Object value) {
        if (value == null) {
            sessionData.remove(key);
            return;
        }

        sessionData.put(key, value);
    }
}