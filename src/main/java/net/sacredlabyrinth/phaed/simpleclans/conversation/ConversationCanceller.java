package net.sacredlabyrinth.phaed.simpleclans.conversation;

import org.jetbrains.annotations.NotNull;

public interface ConversationCanceller {

    boolean cancelBasedOnInput(@NotNull ConversationContext context, @NotNull String input);
}