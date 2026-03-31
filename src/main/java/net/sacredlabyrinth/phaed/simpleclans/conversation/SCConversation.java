package net.sacredlabyrinth.phaed.simpleclans.conversation;

import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SCConversation {
    private static final Map<UUID, SCConversation> conversations = new ConcurrentHashMap<>();

    private final SimpleClans plugin;
    private final Player forWhom;
    private final Prompt firstPrompt;
    private final ConversationContext context;
    private final List<ConversationCanceller> cancellers = new ArrayList<>();
    private final InactivityCanceller inactivityCanceller;

    private volatile Prompt currentPrompt;
    private volatile boolean active;
    private volatile int timeoutGeneration;

    public SCConversation(@NotNull SimpleClans plugin, @NotNull Player forWhom, @Nullable Prompt firstPrompt) {
        this(plugin, forWhom, firstPrompt, new HashMap<>(), 10);
    }

    public SCConversation(@NotNull SimpleClans plugin, @NotNull Player forWhom, @Nullable Prompt firstPrompt,
                          int timeout) {
        this(plugin, forWhom, firstPrompt, new HashMap<>(), timeout);
    }

    public SCConversation(@NotNull SimpleClans plugin, @NotNull Player forWhom, @Nullable Prompt firstPrompt,
                          @NotNull Map<Object, Object> initialSessionData) {
        this(plugin, forWhom, firstPrompt, initialSessionData, 10);
    }

    public SCConversation(@NotNull SimpleClans plugin, @NotNull Player forWhom, @Nullable Prompt firstPrompt,
                          @NotNull Map<Object, Object> initialSessionData, int timeout) {
        this.plugin = plugin;
        this.forWhom = forWhom;
        this.firstPrompt = firstPrompt;
        this.context = new ConversationContext(this, plugin, forWhom, initialSessionData);
        this.inactivityCanceller = new InactivityCanceller(plugin, timeout);
    }

    public void begin() {
        plugin.getFoliaScheduler().runAtEntity(forWhom, this::beginInternal);
    }

    public @NotNull ConversationContext getContext() {
        return context;
    }

    public void addConversationCanceller(@NotNull ConversationCanceller canceller) {
        cancellers.add(canceller);
    }

    public static boolean hasActiveConversation(@NotNull Player player) {
        SCConversation conversation = conversations.get(player.getUniqueId());
        return conversation != null && conversation.active;
    }

    public static void submitInput(@NotNull Player player, @NotNull String input) {
        SCConversation conversation = conversations.get(player.getUniqueId());
        if (conversation != null) {
            conversation.handleInput(input);
        }
    }

    public static void abandon(@NotNull Player player) {
        SCConversation conversation = conversations.get(player.getUniqueId());
        if (conversation != null) {
            conversation.abandonInternal();
        }
    }

    private void beginInternal() {
        UUID uniqueId = forWhom.getUniqueId();
        SCConversation oldConversation = conversations.put(uniqueId, this);
        if (oldConversation != null && oldConversation != this) {
            oldConversation.abandonInternal();
        }

        active = true;
        transitionTo(firstPrompt);
    }

    private void handleInput(@NotNull String input) {
        plugin.getFoliaScheduler().runAtEntity(forWhom, () -> {
            if (!active || currentPrompt == null) {
                return;
            }

            for (ConversationCanceller canceller : cancellers) {
                if (canceller.cancelBasedOnInput(context, input)) {
                    abandonInternal();
                    return;
                }
            }

            transitionTo(currentPrompt.acceptInput(context, input));
        });
    }

    private void transitionTo(@Nullable Prompt nextPrompt) {
        currentPrompt = nextPrompt;

        while (active && currentPrompt != null) {
            String promptText = currentPrompt.getPromptText(context);
            if (!promptText.isEmpty()) {
                forWhom.sendMessage(promptText);
            }

            if (currentPrompt.blocksForInput(context)) {
                scheduleTimeout();
                return;
            }

            currentPrompt = currentPrompt.acceptInput(context, null);
        }

        abandonInternal();
    }

    private void scheduleTimeout() {
        timeoutGeneration++;
        int generation = timeoutGeneration;
        plugin.getFoliaScheduler().runAtEntityLater(forWhom, () -> {
            if (!active || currentPrompt == null || generation != timeoutGeneration) {
                return;
            }

            inactivityCanceller.cancelling(this);
            abandonInternal();
        }, null, inactivityCanceller.getTimeoutTicks());
    }

    private void abandonInternal() {
        active = false;
        currentPrompt = null;
        timeoutGeneration++;
        conversations.remove(forWhom.getUniqueId(), this);
    }
}
