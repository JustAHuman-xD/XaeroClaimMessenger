package me.justahuman.claimmessenger.compat;

import me.justahuman.claimmessenger.ChunkPos;
import me.justahuman.claimmessenger.Claim;
import me.justahuman.claimmessenger.ClaimChannel;
import me.justahuman.claimmessenger.ClaimMessenger;
import net.william278.huskclaims.api.BukkitHuskClaimsAPI;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.event.BukkitDeleteClaimEvent;
import net.william278.huskclaims.event.BukkitPostCreateClaimEvent;
import net.william278.huskclaims.event.BukkitResizeClaimEvent;
import net.william278.huskclaims.event.BukkitTransferClaimEvent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class HuskClaimsChannel extends ClaimChannel {
    private final BukkitHuskClaimsAPI api;

    public HuskClaimsChannel() {
        this.api = BukkitHuskClaimsAPI.getInstance();
    }

    @Override
    public boolean runsAsync() {
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCreate(BukkitPostCreateClaimEvent event) {
        notifyAll(from(event.getClaimWorld(), event.getClaim()), event.getClaim().getTrustedUsers().keySet());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResize(BukkitResizeClaimEvent event) {
        delay(() -> notifyAll(from(event.getClaimWorld(), event.getClaim()), event.getClaim().getTrustedUsers().keySet()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTransfer(BukkitTransferClaimEvent event) {
        deleteAll(Claim.deletion(id(event.getClaim()), worldKey(event.getClaimWorld())));
        delay(() -> notifyAll(from(event.getClaimWorld(), event.getClaim()), event.getClaim().getTrustedUsers().keySet()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDelete(BukkitDeleteClaimEvent event) {
        deleteAll(Claim.deletion(id(event.getClaim()), worldKey(event.getClaimWorld())));
    }

    public Claim from(ClaimWorld world, net.william278.huskclaims.claim.Claim huskClaim) {
        return from(worldKey(world), huskClaim);
    }

    public Claim from(NamespacedKey world, net.william278.huskclaims.claim.Claim huskClaim) {
        Claim claim = new Claim(
                id(huskClaim),
                huskClaim.getOwner().orElse(null),
                "",
                world
        );

        for (int[] chunk : huskClaim.getRegion().getChunks()) {
            claim.chunks().add(new ChunkPos(world, chunk[0], chunk[1]));
        }
        return claim;
    }

    public NamespacedKey worldKey(ClaimWorld world) {
        return Bukkit.getWorld(world.getName(api.getPlugin())).getKey();
    }

    public long id(net.william278.huskclaims.claim.Claim huskClaim) {
        return ClaimMessenger.pack(huskClaim.getOwner().map(UUID::hashCode).orElse(0),
                huskClaim.getCreationTime().map(OffsetDateTime::toInstant).map(Instant::toEpochMilli).map(millis -> (int) (millis & 0xFFFFFFFFL)).orElse(0));
    }

    @Override
    protected List<Claim> getClaims(ChunkPos chunk) {
        return api.getClaimAt(api.getPosition(chunk.toLocation()))
                .map(claim -> Collections.singletonList(from(chunk.world(), claim)))
                .orElse(NONE);
    }
}
