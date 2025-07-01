package me.justahuman.claimmessenger.compat;

import me.justahuman.claimmessenger.ChunkPos;
import me.justahuman.claimmessenger.Claim;
import me.justahuman.claimmessenger.ClaimChannel;
import me.justahuman.claimmessenger.ClaimMessenger;
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

import java.util.Collections;
import java.util.List;

public class HuskTownsChannel extends ClaimChannel {
    private final BukkitHuskTowns huskTowns;
    private final BukkitHuskTownsAPI api = BukkitHuskTownsAPI.getInstance();

    public HuskTownsChannel() {
        this.huskTowns = (BukkitHuskTowns) Bukkit.getPluginManager().getPlugin(ClaimMessenger.HUSK_TOWNS);
    }

    @Override
    public boolean runsAsync() {
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEditTown(TownUpdateEvent event) {
        delay(() -> {
            for (World world : Bukkit.getWorlds()) {
                notifyAll(from(world, event.getTown()), event.getTown().getMembers().keySet());
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTownDeleted(TownDisbandEvent event) {
        delay(() -> {
            for (World world : Bukkit.getWorlds()) {
                deleteAll(Claim.deletion(event.getTown().getId(), world.getKey()));
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
            return NONE;
        }
        TownClaim townClaim = api.getClaimAt(location).orElse(null);
        if (townClaim == null) {
            return NONE;
        }
        Claim claim = from(location.getWorld(), townClaim.town());
        return claim == null ? NONE : Collections.singletonList(claim);
    }
}
