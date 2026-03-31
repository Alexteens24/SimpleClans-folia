package net.sacredlabyrinth.phaed.simpleclans.conversation;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static net.sacredlabyrinth.phaed.simpleclans.SimpleClans.lang;

public class RequestCanceller implements ConversationCanceller {

    @NotNull
    private final String cancelledMessage;
    @NotNull
    private final String escapeSequence;

    public RequestCanceller(@NotNull String escapeSequence, @NotNull String cancelledMessage) {
        this.escapeSequence = escapeSequence;
        this.cancelledMessage = cancelledMessage;
    }

    public RequestCanceller(@NotNull CommandSender sender, @NotNull String cancelledMessage) {
        this(lang("cancel", sender), cancelledMessage);
    }

    @Override
    public boolean cancelBasedOnInput(@NotNull ConversationContext context, @NotNull String input) {
        if (input.equalsIgnoreCase(escapeSequence)) {
            context.getForWhom().sendMessage(cancelledMessage);
            return true;
        }

        return false;
    }
}
