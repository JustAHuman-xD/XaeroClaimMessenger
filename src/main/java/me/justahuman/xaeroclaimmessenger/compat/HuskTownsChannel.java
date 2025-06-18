package me.justahuman.xaeroclaimmessenger.compat;

import me.justahuman.xaeroclaimmessenger.ChunkPos;
import me.justahuman.xaeroclaimmessenger.Claim;
import me.justahuman.xaeroclaimmessenger.ClaimChannel;
import me.justahuman.xaeroclaimmessenger.XaeroClaimMessenger;
import net.william278.husktowns.BukkitHuskTowns;
import net.william278.husktowns.api.BukkitHuskTownsAPI;
import net.william278.husktowns.claim.Chunk;
import net.william278.husktowns.claim.TownClaim;
import net.william278.husktowns.events.TownDisbandEvent;
import net.william278.husktowns.events.TownUpdateEvent;
import net.william278.husktowns.town.Town;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class HuskTownsChannel extends ClaimChannel {
    private final BukkitHuskTowns huskTowns;
    private final BukkitHuskTownsAPI api = BukkitHuskTownsAPI.getInstance();

    public HuskTownsChannel() {
        this.huskTowns = (BukkitHuskTowns) Bukkit.getPluginManager().getPlugin(XaeroClaimMessenger.HUSK_TOWNS);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEditTown(TownUpdateEvent event) {
        delay(() -> {
            Town town = event.getTown();
            for (World world : Bukkit.getWorlds()) {
                Claim claim = from(world, town);
                if (claim != null) {
                    UUID[] members = town.getMembers().keySet().toArray(new UUID[0]);
                    for (UUID member : members) {
                        sendClaim(member, claim);
                    }
                    notifyWithin(claim, members);
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTownDeleted(TownDisbandEvent event) {
        delay(() -> {
            Town town = event.getTown();
            for (World world : Bukkit.getWorlds()) {
                Claim claim = Claim.deletion(town.getId(), world.getKey());
                UUID[] members = town.getMembers().keySet().toArray(new UUID[0]);
                for (UUID member : members) {
                    sendClaim(member, claim);
                }
                deleteWithin(claim, members);
            }
        });
    }

    public Claim from(World world, Town town) {
        return api.getClaimWorld(world).map(claimWorld -> {
            Claim claim = new Claim(
                town.getId(),
                town.getMayor(),
                town.getName(),
                world.getKey(),
                new HashSet<>(),
                town.getDisplayColor().value()
            );
            for (TownClaim townClaim : claimWorld.getTownClaims(town.getId(), huskTowns)) {
                Chunk chunk = townClaim.claim().getChunk();
                claim.chunks().add(new ChunkPos(world.getKey(), chunk.getX(), chunk.getZ()));
            }
            return claim;
        }).orElse(null);
    }


    @Override
    protected List<Claim> getClaims(ChunkPos chunk) {
        Location location = chunk.toLocation();
        if (location == null) {
            return List.of();
        }
        TownClaim townClaim = api.getClaimAt(location).orElse(null);
        if (townClaim == null) {
            return List.of();
        }
        Claim claim = from(location.getWorld(), townClaim.town());
        return claim == null ? List.of() : List.of(claim);
    }
}
