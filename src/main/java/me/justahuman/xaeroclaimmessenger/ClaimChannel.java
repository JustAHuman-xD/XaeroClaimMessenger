package me.justahuman.xaeroclaimmessenger;

import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class ClaimChannel implements Listener {
    private static final int CHANNEL_REGISTER_TIMEOUT = 20;
    private final Map<UUID, Boolean> subscribedPlayers = new HashMap<>();
    protected final Map<ChunkPos, List<UUID>> players = new HashMap<>();

    protected ClaimChannel() {
        final Plugin plugin = XaeroClaimMessenger.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(XaeroClaimMessenger.getInstance(), () -> {
            UUID playerId = player.getUniqueId();
            if (!subscribedPlayers.containsKey(playerId)) {
                for (ChunkPos pos : players.keySet()) {
                    List<UUID> playerList = players.get(pos);
                    if (playerList != null) {
                        playerList.remove(playerId);
                    }
                }
                subscribedPlayers.put(playerId, false);
                return;
            }

            for (ChunkPos pos : players.keySet()) {
                List<UUID> playerList = players.get(pos);
                if (playerList != null && playerList.contains(playerId)) {
                    for (Claim claim : getClaims(pos)) {
                        sendClaim(playerId, claim);
                    }
                }
            }
        }, CHANNEL_REGISTER_TIMEOUT);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRegister(PlayerRegisterChannelEvent event) {
        if (event.getChannel().equals(XaeroClaimMessenger.CLAIM_CHANNEL)) {
            this.subscribedPlayers.put(event.getPlayer().getUniqueId(), true);
        } else if (event.getChannel().equals(XaeroClaimMessenger.DELETE_CHANNEL)) {
            this.subscribedPlayers.put(event.getPlayer().getUniqueId(), true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLoadChunk(PlayerChunkLoadEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        Boolean subscribed = this.subscribedPlayers.get(playerId);
        if (subscribed != null && !subscribed) {
            return;
        }

        ChunkPos pos = new ChunkPos(event.getChunk().getWorld().getKey(), event.getChunk().getX(), event.getChunk().getZ());
        List<UUID> players = this.players.getOrDefault(pos, new ArrayList<>());
        players.add(playerId);
        this.players.put(pos, players);
        if (subscribed == null) {
            return;
        }

        for (Claim claim : getClaims(pos)) {
            sendClaim(playerId, claim);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUnloadChunk(PlayerChunkLoadEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        Boolean subscribed = this.subscribedPlayers.get(playerId);
        if (subscribed != null && !subscribed) {
            return;
        }

        ChunkPos pos = new ChunkPos(event.getChunk().getWorld().getKey(), event.getChunk().getX(), event.getChunk().getZ());
        List<UUID> players = this.players.get(pos);
        if (players != null) {
            players.remove(playerId);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        this.subscribedPlayers.remove(playerId);
        for (List<UUID> playerList : this.players.values()) {
            playerList.remove(playerId);
        }
    }

    protected void delay(Runnable runnable) {
        Bukkit.getScheduler().runTaskLater(XaeroClaimMessenger.getInstance(), runnable, 1L);
    }

    protected abstract List<Claim> getClaims(ChunkPos chunk);

    public void notifyWithin(Claim claim, UUID... exclude) {
        notifyWithin(claim, false, exclude);
    }

    public void deleteWithin(Claim claim, UUID... exclude) {
        notifyWithin(claim, true, exclude);
    }

    public void notifyWithin(Claim claim, boolean delete, UUID... exclude) {
        List<UUID> excluded = Arrays.asList(exclude);
        for (ChunkPos chunk : claim.chunks()) {
            List<UUID> players = this.players.get(chunk);
            if (players != null) {
                for (UUID playerId : players) {
                    if (!excluded.contains(playerId)) {
                        if (delete) {
                            deleteClaim(playerId, claim);
                        } else {
                            sendClaim(playerId, claim);
                        }
                    }
                }
            }
        }
    }

    public void deleteClaim(@Nullable UUID playerId, @NotNull Claim claim) {
        if (playerId != null) {
            deleteClaim(Bukkit.getPlayer(playerId), claim);
        }
    }

    public void deleteClaim(@Nullable Player player, @NotNull Claim claim) {
        if (player != null && subscribedPlayers.getOrDefault(player.getUniqueId(), false)) {
            player.sendPluginMessage(XaeroClaimMessenger.getInstance(), XaeroClaimMessenger.DELETE_CHANNEL, claim.serializeDeletion());
        }
    }

    public void sendClaim(@Nullable UUID playerId, @NotNull Claim claim) {
        if (playerId != null) {
            sendClaim(Bukkit.getPlayer(playerId), claim);
        }
    }

    public void sendClaim(@Nullable Player player, @NotNull Claim claim) {
        if (player != null && subscribedPlayers.getOrDefault(player.getUniqueId(), false)) {
            player.sendPluginMessage(XaeroClaimMessenger.getInstance(), XaeroClaimMessenger.CLAIM_CHANNEL, claim.serialize());
        }
    }
}
