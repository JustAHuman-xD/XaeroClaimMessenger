package me.justahuman.claimmessenger;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUnloadChunk;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class ClaimChannel implements PacketListener, Listener {
    private static final int CHANNEL_REGISTER_TIMEOUT = 20 * 3;
    protected static final List<Claim> NONE = Collections.emptyList();

    protected final Map<UUID, Boolean> subscribedPlayers = new HashMap<>();
    protected final Map<ChunkPos, List<UUID>> players = new HashMap<>();

    protected ClaimChannel() {
        final Plugin plugin = ClaimMessenger.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    protected abstract List<Claim> getClaims(ChunkPos chunk);
    public abstract boolean runsAsync();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        delay(() -> {
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
        if (event.getChannel().equals(ClaimMessenger.CLAIM_CHANNEL)) {
            this.subscribedPlayers.put(event.getPlayer().getUniqueId(), true);
        } else if (event.getChannel().equals(ClaimMessenger.DELETE_CHANNEL)) {
            this.subscribedPlayers.put(event.getPlayer().getUniqueId(), true);
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        PacketTypeCommon type = event.getPacketType();
        if (type == PacketType.Play.Server.CHUNK_DATA) {
            onLoadChunk(event.getPlayer(), new WrapperPlayServerChunkData(event));
        } else if (type == PacketType.Play.Server.UNLOAD_CHUNK) {
            onUnloadChunk(event.getPlayer(), new WrapperPlayServerUnloadChunk(event));
        }
    }

    public void onLoadChunk(Player player, WrapperPlayServerChunkData wrapper) {
        UUID playerId = player.getUniqueId();
        Boolean subscribed = this.subscribedPlayers.get(playerId);
        if (subscribed != null && !subscribed) {
            return;
        }

        ChunkPos pos = new ChunkPos(player.getWorld().getKey(), wrapper.getColumn().getX(), wrapper.getColumn().getZ());
        List<UUID> players = this.players.getOrDefault(pos, new ArrayList<>());
        players.add(playerId);
        this.players.put(pos, players);
        if (subscribed == null) {
            return;
        }

        run(() -> {
            List<Claim> claims = getClaims(pos);
            if (claims.isEmpty()) {
                sendNoClaims(player, pos);
            } else {
                for (Claim claim : getClaims(pos)) {
                    sendClaim(player, claim);
                }
            }
        });
    }

    public void onUnloadChunk(Player player, WrapperPlayServerUnloadChunk wrapper) {
        UUID playerId = player.getUniqueId();
        Boolean subscribed = this.subscribedPlayers.get(playerId);
        if (subscribed != null && !subscribed) {
            return;
        }

        ChunkPos pos = new ChunkPos(player.getWorld().getKey(), wrapper.getChunkX(), wrapper.getChunkZ());
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

    protected void run(Runnable runnable) {
        if (runsAsync()) {
            Bukkit.getScheduler().runTaskAsynchronously(ClaimMessenger.getInstance(), runnable);
        } else {
            if (Bukkit.isPrimaryThread()) {
                runnable.run();
            } else {
                Bukkit.getScheduler().runTask(ClaimMessenger.getInstance(), runnable);
            }
        }
    }

    protected void delay(Runnable runnable) {
        delay(runnable, 0);
    }

    protected void delay(Runnable runnable, int delay) {
        if (runsAsync()) {
            if (delay > 0) {
                Bukkit.getScheduler().runTaskLaterAsynchronously(ClaimMessenger.getInstance(), runnable, delay);
            } else {
                Bukkit.getScheduler().runTaskAsynchronously(ClaimMessenger.getInstance(), runnable);
            }
        } else {
            if (delay > 0) {
                Bukkit.getScheduler().runTaskLater(ClaimMessenger.getInstance(), runnable, delay);
            } else {
                Bukkit.getScheduler().runTask(ClaimMessenger.getInstance(), runnable);
            }
        }
    }

    public void notifyAll(Claim claim, UUID... explicit) {
        notifyAll(claim, Arrays.asList(explicit));
    }

    public void notifyAll(Claim claim, Collection<UUID> explicit) {
        if (claim == null) {
            return;
        }

        for (UUID playerId : explicit) {
            sendClaim(playerId, claim);
        }

        for (ChunkPos chunk : claim.chunks()) {
            List<UUID> players = this.players.get(chunk);
            if (players != null) {
                for (UUID playerId : players) {
                    if (!explicit.contains(playerId)) {
                        sendClaim(playerId, claim);
                    }
                }
            }
        }
    }

    public void deleteAll(Claim claim) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (subscribedPlayers.getOrDefault(player.getUniqueId(), false)) {
                deleteClaim(player, claim);
            }
        }
    }

    public void sendClaim(@Nullable UUID playerId, @Nonnull Claim claim) {
        if (playerId != null) {
            sendClaim(Bukkit.getPlayer(playerId), claim);
        }
    }

    public void sendClaim(@Nullable Player player, @Nonnull Claim claim) {
        if (player != null && subscribedPlayers.getOrDefault(player.getUniqueId(), false)) {
            player.sendPluginMessage(ClaimMessenger.getInstance(), ClaimMessenger.CLAIM_CHANNEL, claim.serialize());
        }
    }

    public void sendNoClaims(@Nullable UUID playerId, @Nonnull ChunkPos pos) {
        if (playerId != null) {
            sendNoClaims(Bukkit.getPlayer(playerId), pos);
        }
    }

    public void sendNoClaims(@Nullable Player player, @Nonnull ChunkPos pos) {
        if (player != null && subscribedPlayers.getOrDefault(player.getUniqueId(), false)) {
            player.sendPluginMessage(ClaimMessenger.getInstance(), ClaimMessenger.NO_CLAIMS_CHANNEL, pos.serializeNoClaims());
        }
    }

    public void deleteClaim(@Nullable UUID playerId, @Nonnull Claim claim) {
        if (playerId != null) {
            deleteClaim(Bukkit.getPlayer(playerId), claim);
        }
    }

    public void deleteClaim(@Nullable Player player, @Nonnull Claim claim) {
        if (player != null && subscribedPlayers.getOrDefault(player.getUniqueId(), false)) {
            player.sendPluginMessage(ClaimMessenger.getInstance(), ClaimMessenger.DELETE_CHANNEL, claim.serializeDeletion());
        }
    }
}
