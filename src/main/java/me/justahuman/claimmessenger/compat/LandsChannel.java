package me.justahuman.claimmessenger.compat;

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
import me.justahuman.claimmessenger.ChunkPos;
import me.justahuman.claimmessenger.Claim;
import me.justahuman.claimmessenger.ClaimChannel;
import me.justahuman.claimmessenger.ClaimMessenger;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class LandsChannel extends ClaimChannel {
    private final LandsIntegration api = LandsIntegration.of(ClaimMessenger.getInstance());

    @Override
    public boolean runsAsync() {
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLandCreate(LandCreateEvent event) {
        delay(() -> notifyLand(event.getLand()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOwnerChange(LandOwnerChangeEvent event) {
        delay(() -> notifyLand(event.getLand()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRename(LandRenameEvent event) {
        delay(() -> notifyLand(event.getLand()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkClaim(ChunkPostClaimEvent event) {
        notifyLand(event.getLand());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnclaim(ChunkDeleteEvent event) {
        delay(() -> notifyLand(event.getLand()));
    }

    protected void notifyLand(Land land) {
        for (Container container : land.getContainers()) {
            notifyAll(from(container, land), land.getTrustedPlayers());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLandDelete(LandDeleteEvent event) {
        for (Container container : event.getLand().getContainers()) {
            deleteAll(Claim.deletion(event.getLand().getULID().hashCode(), container.getWorld().getWorld().getKey()));
        }
    }

    @Override
    protected List<Claim> getClaims(ChunkPos chunk) {
        World world = chunk.getWorld();
        if (world == null) {
            return NONE;
        }
        Land land = api.getLandByChunk(world, chunk.x(), chunk.z());
        return land != null ? Collections.singletonList(from(world, land)) : NONE;
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
                land.getWebMapFillColor() == null ? ClaimMessenger.locatorBarColor(land.getOwnerUID()) : land.getWebMapFillColor()
        );
        if (container != null) {
            for (ChunkCoordinate coordinate : container.getChunks()) {
                claim.chunks().add(new ChunkPos(world.getKey(), coordinate.getX(), coordinate.getZ()));
            }
        }
        return claim;
    }
}
