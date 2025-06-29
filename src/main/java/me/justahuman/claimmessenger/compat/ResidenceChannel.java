package me.justahuman.claimmessenger.compat;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.ResidencePlayer;
import com.bekvon.bukkit.residence.event.ResidenceDeleteEvent;
import com.bekvon.bukkit.residence.event.ResidenceEvent;
import com.bekvon.bukkit.residence.event.ResidenceRenameEvent;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.CuboidArea;
import com.bekvon.bukkit.residence.protection.ResidenceManager;
import me.justahuman.claimmessenger.ChunkPos;
import me.justahuman.claimmessenger.Claim;
import me.justahuman.claimmessenger.ClaimChannel;
import me.justahuman.claimmessenger.ClaimMessenger;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class ResidenceChannel extends ClaimChannel {
    private final ResidenceManager manager;

    public ResidenceChannel() {
        this.manager = Residence.getInstance().getResidenceManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResidenceEvent(ResidenceEvent event) {
        ClaimedResidence residence = event.getResidence();
        Claim claim = from(residence);
        UUID[] members = residence.getTrustedPlayers().stream()
                .map(ResidencePlayer::getUniqueId).toArray(UUID[]::new);

        if (event instanceof ResidenceDeleteEvent || event instanceof ResidenceRenameEvent) {
            for (UUID member : members) {
                deleteClaim(member, claim);
            }
            deleteWithin(claim, members);
            if (event instanceof ResidenceRenameEvent rename) {
                delay(() -> {
                    ClaimedResidence newResidence = manager.getByName(rename.getNewResidenceName());
                    Claim newClaim = from(newResidence);
                    for (UUID member : members) {
                        sendClaim(member, newClaim);
                    }
                    notifyWithin(newClaim, members);
                });
            }
        } else {
            for (UUID member : members) {
                sendClaim(member, claim);
            }
            notifyWithin(claim, members);
        }
    }

    @Override
    protected List<Claim> getClaims(ChunkPos chunk) {
        Location location = chunk.toLocation();
        if (location == null) {
            return List.of();
        }
        ClaimedResidence residence = Residence.getInstance().getResidenceManager().getByLoc(location);
        return residence == null ? List.of() : List.of(from(residence));
    }

    private Claim from(ClaimedResidence residence) {
        World world = residence.getPermissions().getBukkitWorld();
        if (world == null) {
            throw new IllegalStateException("Residence world is null for " + residence.getName());
        }

        NamespacedKey worldKey = world.getKey();
        Claim claim = new Claim(
                residence.getName().hashCode(),
                residence.getOwnerUUID(),
                residence.getName(),
                worldKey,
                new HashSet<>(),
                residence.getChannelColor() == null
                        ? ClaimMessenger.locatorBarColor(residence.getOwnerUUID())
                        : residence.getChannelColor().getJavaColor().getRGB()
        );

        for (CuboidArea area : residence.getAreaArray()) {
            for (ResidenceManager.ChunkRef ref : area.getChunks()) {
                claim.chunks().add(new ChunkPos(worldKey, ref.getX(), ref.getZ()));
            }
        }

        return claim;
    }
}
