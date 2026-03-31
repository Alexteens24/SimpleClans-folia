package net.sacredlabyrinth.phaed.simpleclans.conversation;

import org.jetbrains.annotations.NotNull;

public abstract class StringPrompt extends Prompt {

    @Override
    public boolean blocksForInput(@NotNull ConversationContext context) {
        return true;
    }
}