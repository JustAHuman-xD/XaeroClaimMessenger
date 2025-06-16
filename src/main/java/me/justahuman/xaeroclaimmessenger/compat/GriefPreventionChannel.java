package me.justahuman.xaeroclaimmessenger.compat;

import me.justahuman.xaeroclaimmessenger.ChunkPos;
import me.justahuman.xaeroclaimmessenger.Claim;
import me.justahuman.xaeroclaimmessenger.ClaimChannel;
import me.justahuman.xaeroclaimmessenger.XaeroClaimMessenger;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.events.ClaimChangeEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimCreatedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimDeletedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimTransferEvent;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class GriefPreventionChannel extends ClaimChannel {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClaimCreated(ClaimCreatedEvent event) {
        Claim claim = from(event.getClaim());
        if (event.getCreator() instanceof Player player) {
            sendClaim(player, claim);
            notifyWithin(claim, player.getUniqueId());
        } else {
            notifyWithin(claim);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClaimTransferred(ClaimTransferEvent event) {
        me.ryanhamshire.GriefPrevention.Claim gp = event.getClaim();
        Claim claim = from(gp, event.getNewOwner());
        sendClaim(gp.ownerID, claim);
        sendClaim(claim.owner(), claim);
        notifyWithin(claim, gp.ownerID, claim.owner());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClaimChanged(ClaimChangeEvent event) {
        Claim claim = from(event.getTo());
        sendClaim(event.getFrom().ownerID, claim);
        sendClaim(event.getTo().ownerID, claim);
        notifyWithin(claim, event.getFrom().ownerID, event.getTo().ownerID);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClaimDeleted(ClaimDeletedEvent event) {
        Claim claim = Claim.deletion(event.getClaim().getID(), event.getClaim().getLesserBoundaryCorner().getWorld().getKey());
        deleteClaim(claim.owner(), claim);
        deleteWithin(claim, claim.owner());
    }

    private List<Claim> from(Collection<me.ryanhamshire.GriefPrevention.Claim> gpClaims) {
        List<Claim> claims = new ArrayList<>();
        for (me.ryanhamshire.GriefPrevention.Claim gp : gpClaims) {
            claims.add(from(gp));
        }
        return claims;
    }

    private Claim from(me.ryanhamshire.GriefPrevention.Claim gp) {
        return from(gp, gp.ownerID);
    }

    private Claim from(me.ryanhamshire.GriefPrevention.Claim gp, UUID ownerID) {
        Claim claim = new Claim(
                gp.getID(),
                ownerID,
                "",
                gp.getLesserBoundaryCorner().getWorld().getKey(),
                new ArrayList<>(),
                XaeroClaimMessenger.locatorBarColor(ownerID)
        );
        Location min = gp.getLesserBoundaryCorner();
        Location max = gp.getGreaterBoundaryCorner();
        Chunk minChunk = min.getChunk();
        Chunk maxChunk = max.getChunk();
        NamespacedKey world = min.getWorld().getKey();
        for (int x = minChunk.getX(); x <= maxChunk.getX(); x++) {
            for (int z = minChunk.getZ(); z <= maxChunk.getZ(); z++) {
                if (x * 16 < min.getBlockX() || x * 16 + 15 > max.getBlockX() ||
                    z * 16 < min.getBlockZ() || z * 16 + 15 > max.getBlockZ()) {
                    continue;
                }
                claim.chunks().add(new ChunkPos(world, x, z));
            }
        }
        return claim;
    }

    @Override
    protected List<Claim> getClaims(ChunkPos chunk) {
        return from(GriefPrevention.instance.dataStore.getClaims(chunk.x(), chunk.z()));
    }
}
