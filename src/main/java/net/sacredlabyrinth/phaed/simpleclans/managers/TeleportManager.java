package net.sacredlabyrinth.phaed.simpleclans.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.sacredlabyrinth.phaed.simpleclans.*;
import net.sacredlabyrinth.phaed.simpleclans.events.ClanPlayerTeleportEvent;
import net.sacredlabyrinth.phaed.simpleclans.utils.VanishUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static net.sacredlabyrinth.phaed.simpleclans.SimpleClans.lang;
import static net.sacredlabyrinth.phaed.simpleclans.managers.SettingsManager.ConfigField.*;
import static net.sacredlabyrinth.phaed.simpleclans.utils.LegacyColor.AQUA;
import static net.sacredlabyrinth.phaed.simpleclans.utils.LegacyColor.RED;

/**
 * Class responsible for managing teleports and its queue
 */
public final class TeleportManager {
    private final SimpleClans plugin;
    private final ConcurrentHashMap<UUID, TeleportState> waitingPlayers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ScheduledTask> countdownTasks = new ConcurrentHashMap<>();

    public TeleportManager() {
        plugin = SimpleClans.getInstance();
    }

    /**
     * Add player to teleport waiting queue
     *
     * @param player      the Player
     * @param destination the destination
     * @param clanName    the Clan name
     */
    public void addPlayer(Player player, Location destination, String clanName) {
        PermissionsManager pm = plugin.getPermissionsManager();

        int secs = SimpleClans.getInstance().getSettingsManager().getInt(CLAN_TELEPORT_DELAY);
        if (pm.has(player, "simpleclans.mod.bypass") || pm.has(player, "simpleclans.vip.teleport-delay")) {
            secs = 0;
        }
        final int delaySeconds = secs;
        UUID playerId = player.getUniqueId();
        TeleportState state = new TeleportState(player, destination.clone(), clanName, delaySeconds);
        waitingPlayers.put(playerId, state);

        ScheduledTask previousTask = countdownTasks.remove(playerId);
        if (previousTask != null && !previousTask.isCancelled()) {
            previousTask.cancel();
        }

        if (plugin.getFoliaScheduler().runAtEntity(player, () -> {
            if (waitingPlayers.get(playerId) != state) {
                return;
            }
            sendTeleportBlocks(player, state.getDestination());
            if (delaySeconds > 0) {
                ChatBlock.sendMessage(player, AQUA + lang("waiting.for.teleport.stand.still.for.0.seconds", player, delaySeconds));
                return;
            }
            cleanupTeleportState(playerId, state, null);
            teleport(state);
        }) == null) {
            cleanupTeleportState(playerId, state, null);
            return;
        }

        if (delaySeconds > 0) {
            ScheduledTask task = plugin.getFoliaScheduler().runAtEntityTimer(player, scheduledTask -> {
                if (waitingPlayers.get(playerId) != state) {
                    cleanupTeleportState(playerId, state, scheduledTask);
                    return;
                }
                if (!isSameBlock(player.getLocation(), state.getLocation())) {
                    ChatBlock.sendMessage(player, RED + lang("you.moved.teleport.cancelled", player));
                    cleanupTeleportState(playerId, state, scheduledTask);
                    return;
                }
                if (state.isTeleportTime()) {
                    cleanupTeleportState(playerId, state, scheduledTask);
                    teleport(state);
                } else {
                    ChatBlock.sendMessage(player, AQUA + "" + state.getCounter());
                }
            }, () -> cleanupTeleportState(playerId, state, null), 20L, 20L);
            if (task == null) {
                cleanupTeleportState(playerId, state, null);
                return;
            }
            countdownTasks.put(playerId, task);
        }
    }

    /**
     * Teleports all online and non-vanished members of this {@link Clan} to the specified {@link Location}
     *
     * @param requester the Player requesting the teleport
     * @param clan      the Clan
     * @param location  the Location
     */
    public void teleport(@NotNull Player requester, @NotNull Clan clan, @NotNull Location location) {
        teleport(clan, location, VanishUtils.getNonVanished(requester, clan));
    }

    /**
     * Teleports all online and non-vanished members of this {@link Clan} to the specified {@link Location}
     *
     * @param clan     the Clan
     * @param location the Location
     */
    public void teleport(Clan clan, Location location) {
        teleport(clan, location, VanishUtils.getNonVanished(null, clan));
    }

    public void teleportToHome(@NotNull Player player, @NotNull Location destination, @NotNull String clanName) {
        plugin.getFoliaScheduler().runRegion(destination, () -> {
            Location safeDestination = getSafe(destination);
            player.teleportAsync(safeDestination, PlayerTeleportEvent.TeleportCause.COMMAND).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    plugin.getLogger().log(Level.WARNING, "An error occurred while teleporting a player", throwable);
                    return;
                }
                plugin.getFoliaScheduler().runAtEntity(player, () -> {
                    if (Boolean.TRUE.equals(result)) {
                        ChatBlock.sendMessage(player, AQUA + lang("now.at.homebase", player, clanName));
                    } else {
                        plugin.getLogger().log(Level.WARNING, "An error occurred while teleporting a player");
                    }
                });
            });
        });
    }

    public void teleportToHome(@NotNull Player player, @NotNull Clan clan) {
        if (clan.getHomeLocation() == null) {
            return;
        }
        teleportToHome(player, clan.getHomeLocation(), clan.getName());
    }

    private boolean isSameBlock(Location loc, Location loc2) {
        return loc.getBlockX() == loc2.getBlockX() && loc.getBlockY() == loc2.getBlockY() &&
                loc.getBlockZ() == loc2.getBlockZ();
    }

    /**
     * Converts the specified {@link Location} to a safe one, i.e. where there is no risk of suffocation
     *
     * @param location the Location
     * @return the safe Location
     */
    public @NotNull Location getSafe(@NotNull Location location) {
        Location safeLocation = location.clone();
        int minHeight = safeLocation.getWorld().getMinHeight();
        int maxHeight = safeLocation.getWorld().getMaxHeight() - 1;
        int startY = Math.max(minHeight, Math.min(maxHeight - 1, safeLocation.getBlockY()));

        for (int currentY = startY; currentY < maxHeight; currentY++) {
            safeLocation.setY(currentY);
            Block bottom = safeLocation.getBlock();
            Block top = safeLocation.clone().add(0, 1, 0).getBlock();
            if (isAir(bottom) && isAir(top)) {
                return safeLocation;
            }
        }

        safeLocation.setY(Math.min(maxHeight, safeLocation.getWorld().getHighestBlockYAt(safeLocation) + 1));
        return safeLocation;
    }

    private void dropItems(Player player) {
        if (plugin.getPermissionsManager().has(player, "simpleclans.mod.keep-items")) {
            return;
        }
        List<Material> itemsList = plugin.getSettingsManager().getItemList();
        PlayerInventory inv = player.getInventory();
        boolean dropOnHome = plugin.getSettingsManager().is(DROP_ITEMS_ON_CLAN_HOME);
        boolean keepOnHome = plugin.getSettingsManager().is(KEEP_ITEMS_ON_CLAN_HOME);
        ItemStack[] contents = inv.getContents();
        for (ItemStack item : contents) {
            if (item == null) {
                continue;
            }
            if ((dropOnHome && itemsList.contains(item.getType())) ||
                    (keepOnHome && !itemsList.contains(item.getType()))) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
                inv.remove(item);
            }
        }
    }

    private void teleport(TeleportState state) {
        Player player = state.getPlayer();
        if (player == null) {
            return;
        }
        ClanPlayer cp = plugin.getClanManager().getCreateClanPlayer(player.getUniqueId());
        ClanPlayerTeleportEvent event = new ClanPlayerTeleportEvent(cp, state.getLocation(), state.getDestination());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        Location loc = state.getDestination().clone();
        dropItems(player);
        loc = loc.add(.5, .5, .5);
        teleportToHome(player, loc, state.getClanName());
    }

    private void sendTeleportBlocks(Player player, Location loc) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        if (plugin.getSettingsManager().is(TELEPORT_BLOCKS)) {
            player.sendBlockChange(new Location(loc.getWorld(), x + 1, loc.getBlockY() - 1, z + 1),
                Material.GLASS.createBlockData());
            player.sendBlockChange(new Location(loc.getWorld(), x - 1, loc.getBlockY() - 1, z - 1),
                Material.GLASS.createBlockData());
            player.sendBlockChange(new Location(loc.getWorld(), x + 1, loc.getBlockY() - 1, z - 1),
                Material.GLASS.createBlockData());
            player.sendBlockChange(new Location(loc.getWorld(), x - 1, loc.getBlockY() - 1, z + 1),
                Material.GLASS.createBlockData());
        }
    }

    private void teleport(Clan clan, Location location, List<ClanPlayer> members) {
        for (ClanPlayer cp : members) {
            Player player = cp.toPlayer();
            if (player == null) {
                continue;
            }
            int x = location.getBlockX();
            int z = location.getBlockZ();

            Random r = new Random();
            int xx = r.nextInt(2) - 1;
            int zz = r.nextInt(2) - 1;
            if (xx == 0 && zz == 0) {
                xx = 1;
            }
            x = x + xx;
            z = z + zz;

            plugin.getTeleportManager().addPlayer(player, new Location(location.getWorld(), x + .5,
                    location.getBlockY(), z + .5, location.getYaw(), location.getPitch()), clan.getName());
        }
    }

    /**
     * Checks if all passed blocks are some kind of AIR
     *
     * @param blocks blocks to test
     * @return true if all blocks are AIR
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isAir(@NotNull Block... blocks) {
        for (Block b : blocks) {
            if (!b.getType().name().contains("AIR")) {
                return false;
            }
        }
        return true;
    }

    private void cleanupTeleportState(@NotNull UUID playerId, @NotNull TeleportState expectedState,
                                     @Nullable ScheduledTask scheduledTask) {
        waitingPlayers.remove(playerId, expectedState);
        if (scheduledTask != null) {
            scheduledTask.cancel();
            countdownTasks.remove(playerId, scheduledTask);
            return;
        }
        ScheduledTask activeTask = countdownTasks.remove(playerId);
        if (activeTask != null && !activeTask.isCancelled()) {
            activeTask.cancel();
        }
    }

}