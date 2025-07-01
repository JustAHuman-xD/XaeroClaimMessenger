package me.justahuman.claimmessenger.compat;

import me.justahuman.claimmessenger.ChunkPos;
import me.justahuman.claimmessenger.Claim;
import me.justahuman.claimmessenger.ClaimChannel;
import me.justahuman.claimmessenger.ClaimMessenger;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.land.Land;
import org.kingdoms.constants.land.location.SimpleChunkLocation;
import org.kingdoms.constants.themes.MainTheme;
import org.kingdoms.events.general.KingdomCreateEvent;
import org.kingdoms.events.general.KingdomDisbandEvent;
import org.kingdoms.events.general.KingdomKingChangeEvent;
import org.kingdoms.events.lands.ClaimLandEvent;
import org.kingdoms.events.lands.UnclaimLandEvent;

import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class KingdomsChannel extends ClaimChannel {
    @Override
    public boolean runsAsync() {
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreate(KingdomCreateEvent event) {
        notifyKingdom(event.getKingdom());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClaim(ClaimLandEvent event) {
        notifyKingdom(event.getKingdom());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUnclaim(UnclaimLandEvent event) {
        notifyKingdom(event.getKingdom());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTransfer(KingdomKingChangeEvent event) {
        delay(() -> notifyKingdom(event.getKingdom()));
    }

    protected void handleClaims(Kingdom kingdom, Function<NamespacedKey, Claim> factory, Consumer<Claim> consumer) {
        if (kingdom != null) {
            Set<NamespacedKey> claims = new HashSet<>();
            for (SimpleChunkLocation chunk : kingdom.getLandLocations()) {
                NamespacedKey worldKey = chunk.getBukkitWorld().getKey();
                if (claims.add(worldKey)) {
                    consumer.accept(factory.apply(worldKey));
                }
            }
        }
    }

    protected void notifyKingdom(Kingdom kingdom) {
        handleClaims(kingdom, world -> from(world, kingdom), claim -> notifyAll(claim, kingdom.getMembers()));
    }

    protected void deleteKingdom(Kingdom kingdom) {
        handleClaims(kingdom, world -> Claim.deletion(kingdom.getId().hashCode(), world), this::deleteAll);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDelete(KingdomDisbandEvent event) {
        deleteKingdom(event.getKingdom());
    }

    @Override
    protected List<Claim> getClaims(ChunkPos chunk) {
        Location location = chunk.toLocation();
        if (location == null) {
            return NONE;
        }
        SimpleChunkLocation chunkLocation = SimpleChunkLocation.of(location);
        Land land = chunkLocation.getLand();
        if (land == null || !land.isClaimed()) {
            return NONE;
        }
        return Collections.singletonList(from(chunk.world(), land.getKingdom()));
    }

    protected Claim from(NamespacedKey worldKey, Kingdom kingdom) {
        Color color = kingdom.getColors().get(MainTheme.INSTANCE.getNamespace());
        Claim claim = new Claim(
                kingdom.getId().hashCode(),
                kingdom.getKingId(),
                kingdom.getName(),
                worldKey,
                color == null ? ClaimMessenger.locatorBarColor(kingdom.getOwnerId()) : color.getRGB()
        );
        for (SimpleChunkLocation chunk : kingdom.getLandLocations()) {
            if (chunk.getBukkitWorld().getKey().equals(worldKey)) {
                claim.chunks().add(new ChunkPos(worldKey, chunk.getX(), chunk.getZ()));
            }
        }
        return claim;
    }
}
