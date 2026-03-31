package net.sacredlabyrinth.phaed.simpleclans.conversation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class MessagePrompt extends Prompt {

    @Override
    public final boolean blocksForInput(@NotNull ConversationContext context) {
        return false;
    }

    @Override
    public final @Nullable Prompt acceptInput(@NotNull ConversationContext context, @Nullable String input) {
        return getNextPrompt(context);
    }

    protected abstract @Nullable Prompt getNextPrompt(@NotNull ConversationContext context);
}