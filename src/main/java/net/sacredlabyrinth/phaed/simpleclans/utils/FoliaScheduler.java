package net.sacredlabyrinth.phaed.simpleclans.utils;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class FoliaScheduler {

    private static final long TICK_DURATION_MILLIS = 50L;

    private final SimpleClans plugin;

    public FoliaScheduler(@NotNull SimpleClans plugin) {
        this.plugin = plugin;
    }

    public void executeGlobal(@NotNull Runnable task) {
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, task);
    }

    public @NotNull ScheduledTask runGlobal(@NotNull Runnable task) {
        return plugin.getServer().getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
    }

    public @NotNull ScheduledTask runGlobalLater(@NotNull Runnable task, long delayTicks) {
        return plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> task.run(), delayTicks);
    }

    public @NotNull ScheduledTask runGlobalTimer(@NotNull Consumer<ScheduledTask> task, long initialDelayTicks,
                                                 long periodTicks) {
        return plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task,
            normalizeFixedRateTicks(initialDelayTicks), normalizeFixedRateTicks(periodTicks));
    }

    public void executeRegion(@NotNull Location location, @NotNull Runnable task) {
        plugin.getServer().getRegionScheduler().execute(plugin, location, task);
    }

    public @NotNull ScheduledTask runRegion(@NotNull Location location, @NotNull Runnable task) {
        return plugin.getServer().getRegionScheduler().run(plugin, location, scheduledTask -> task.run());
    }

    public @NotNull ScheduledTask runRegionLater(@NotNull Location location, @NotNull Runnable task, long delayTicks) {
        return plugin.getServer().getRegionScheduler().runDelayed(plugin, location, scheduledTask -> task.run(), delayTicks);
    }

    public @Nullable ScheduledTask runAtEntity(@NotNull Entity entity, @NotNull Runnable task) {
        return entity.getScheduler().run(plugin, scheduledTask -> task.run(), null);
    }

    public @Nullable ScheduledTask runAtEntityLater(@NotNull Entity entity, @NotNull Runnable task,
                                                    @Nullable Runnable retiredTask, long delayTicks) {
        return entity.getScheduler().runDelayed(plugin, scheduledTask -> task.run(), retiredTask, delayTicks);
    }

    public @Nullable ScheduledTask runAtEntityTimer(@NotNull Entity entity, @NotNull Consumer<ScheduledTask> task,
                                                    @Nullable Runnable retiredTask, long initialDelayTicks,
                                                    long periodTicks) {
        return entity.getScheduler().runAtFixedRate(plugin, task, retiredTask,
            normalizeFixedRateTicks(initialDelayTicks), normalizeFixedRateTicks(periodTicks));
    }

    public boolean executeAtEntity(@NotNull Entity entity, @NotNull Runnable task, @Nullable Runnable retiredTask,
                                   long delayTicks) {
        return entity.getScheduler().execute(plugin, task, retiredTask, delayTicks);
    }

    public @NotNull ScheduledTask runAsync(@NotNull Runnable task) {
        return plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
    }

    public @NotNull ScheduledTask runAsyncLater(@NotNull Runnable task, long delayTicks) {
        return plugin.getServer().getAsyncScheduler().runDelayed(plugin, scheduledTask -> task.run(),
                ticksToMillis(delayTicks), TimeUnit.MILLISECONDS);
    }

    public @NotNull ScheduledTask runAsyncTimer(@NotNull Consumer<ScheduledTask> task, long initialDelayTicks,
                                                long periodTicks) {
        return plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, task,
            ticksToFixedRateMillis(initialDelayTicks), ticksToFixedRateMillis(periodTicks), TimeUnit.MILLISECONDS);
    }

    public void cancelAllTasks() {
        plugin.getServer().getAsyncScheduler().cancelTasks(plugin);
        plugin.getServer().getGlobalRegionScheduler().cancelTasks(plugin);
    }

    private long ticksToMillis(long ticks) {
        return Math.max(0L, ticks) * TICK_DURATION_MILLIS;
    }

    private long ticksToFixedRateMillis(long ticks) {
        return normalizeFixedRateTicks(ticks) * TICK_DURATION_MILLIS;
    }

    private long normalizeFixedRateTicks(long ticks) {
        return Math.max(1L, ticks);
    }
}