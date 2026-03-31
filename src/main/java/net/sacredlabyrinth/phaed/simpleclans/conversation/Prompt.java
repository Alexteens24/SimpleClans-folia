package net.sacredlabyrinth.phaed.simpleclans.conversation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Prompt {

    public static final Prompt END_OF_CONVERSATION = null;

    public boolean blocksForInput(@NotNull ConversationContext context) {
        return false;
    }

    public abstract @NotNull String getPromptText(@NotNull ConversationContext context);

    public abstract @Nullable Prompt acceptInput(@NotNull ConversationContext context, @Nullable String input);
}