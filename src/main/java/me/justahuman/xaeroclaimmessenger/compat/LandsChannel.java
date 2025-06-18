package me.justahuman.xaeroclaimmessenger.compat;

import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.events.ChunkDeleteEvent;
import me.angeschossen.lands.api.events.ChunkPostClaimEvent;
import me.angeschossen.lands.api.events.LandCreateEvent;
import me.angeschossen.lands.api.events.LandDeleteEvent;
import me.angeschossen.lands.api.events.LandOwnerChangeEvent;
import me.angeschossen.lands.api.events.LandRenameEvent;
import me.angeschossen.lands.api.land.ChunkCoordinate;
import me.angeschossen.lands.api.land.Container;
import me.angeschossen.lands.api.land.Land;
import me.justahuman.xaeroclaimmessenger.ChunkPos;
import me.justahuman.xaeroclaimmessenger.Claim;
import me.justahuman.xaeroclaimmessenger.ClaimChannel;
import me.justahuman.xaeroclaimmessenger.XaeroClaimMessenger;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class LandsChannel extends ClaimChannel {
    private final LandsIntegration api = LandsIntegration.of(XaeroClaimMessenger.getInstance());

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLandCreate(LandCreateEvent event) {
        delay(() -> {
            Land land = event.getLand();
            UUID player = event.getPlayerUUID();
            for (Container container : land.getContainers()) {
                Claim claim = from(container, land);
                sendClaim(player, claim);
                notifyWithin(claim, player);
            }
        });
    }

    protected void updateLand(Land land) {
        UUID[] players = land.getOnlinePlayers().stream()
                .map(Entity::getUniqueId).toArray(UUID[]::new);
        for (Container container : land.getContainers()) {
            Claim claim = from(container, land);
            for (UUID player : players) {
                sendClaim(player, claim);
            }
            notifyWithin(claim, players);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOwnerChange(LandOwnerChangeEvent event) {
        delay(() -> updateLand(event.getLand()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRename(LandRenameEvent event) {
        delay(() -> updateLand(event.getLand()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkClaim(ChunkPostClaimEvent event) {
        updateLand(event.getLand());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnclaim(ChunkDeleteEvent event) {
        delay(() -> updateLand(event.getLand()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLandDelete(LandDeleteEvent event) {
        Land land = event.getLand();
        UUID[] players = land.getOnlinePlayers().stream()
                .map(Entity::getUniqueId).toArray(UUID[]::new);
        for (Container container : land.getContainers()) {
            Claim claim = Claim.deletion(land.getULID().hashCode(), container.getWorld().getWorld().getKey());
            for (UUID player : players) {
                sendClaim(player, claim);
            }
            deleteWithin(claim, players);
        }
    }

    @Override
    protected List<Claim> getClaims(ChunkPos chunk) {
        World world = chunk.getWorld();
        if (world == null) {
            return List.of();
        }
        Land land = api.getLandByChunk(world, chunk.x(), chunk.z());
        return land != null ? List.of(from(world, land)) : List.of();
    }

    public Claim from(@NotNull World world, @NotNull Land land) {
        return from(world, land.getContainer(world), land);
    }

    public Claim from(@NotNull Container container, @NotNull Land land) {
        return from(container.getWorld().getWorld(), container, land);
    }

    public Claim from(@NotNull World world, @Nullable Container container, @NotNull Land land) {
        Claim claim = new Claim(
                land.getULID().hashCode(),
                land.getOwnerUID(),
                land.getColorName(),
                world.getKey(),
                new HashSet<>(),
                land.getWebMapFillColor() == null ? XaeroClaimMessenger.locatorBarColor(land.getOwnerUID()) : land.getWebMapFillColor()
        );
        if (container != null) {
            for (ChunkCoordinate coordinate : container.getChunks()) {
                claim.chunks().add(new ChunkPos(world.getKey(), coordinate.getX(), coordinate.getZ()));
            }
        }
        return claim;
    }
}
