package net.sacredlabyrinth.phaed.simpleclans.conversation;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ConversationListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        if (!SCConversation.hasActiveConversation(event.getPlayer())) {
            return;
        }

        event.setCancelled(true);
        String input = PlainTextComponentSerializer.plainText().serialize(event.message());
        SCConversation.submitInput(event.getPlayer(), input);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        SCConversation.abandon(event.getPlayer());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        SCConversation.abandon(event.getPlayer());
    }
}