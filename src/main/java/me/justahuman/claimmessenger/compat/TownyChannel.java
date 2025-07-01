package me.justahuman.claimmessenger.compat;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.NewTownEvent;
import com.palmergames.bukkit.towny.event.PreDeleteTownEvent;
import com.palmergames.bukkit.towny.event.RenameTownEvent;
import com.palmergames.bukkit.towny.event.TownClaimEvent;
import com.palmergames.bukkit.towny.event.town.TownMayorChangedEvent;
import com.palmergames.bukkit.towny.event.town.TownUnclaimEvent;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import me.justahuman.claimmessenger.ChunkPos;
import me.justahuman.claimmessenger.Claim;
import me.justahuman.claimmessenger.ClaimChannel;
import me.justahuman.claimmessenger.ClaimMessenger;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TownyChannel extends ClaimChannel {
    private final TownyAPI api = TownyAPI.getInstance();
    private final int side = WorldCoord.getCellSize() / 16;
    // TODO: this breaks if side is anything more than 1, fix that at some point

    public TownyChannel() {
        int cellSize = WorldCoord.getCellSize();
        if (cellSize < 16 || cellSize % 16 != 0) {
            throw new IllegalStateException("Townys cell size must be a product of 16 for ClaimMessenger!");
        }
    }

    @Override
    public boolean runsAsync() {
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNewTown(NewTownEvent event) {
        notifyTown(event.getTown());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMayorChanged(TownMayorChangedEvent event) {
        notifyTown(event.getTown());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRename(RenameTownEvent event) {
        notifyTown(event.getTown());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClaim(TownClaimEvent event) {
        notifyTown(event.getTown());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUnclaim(TownUnclaimEvent event) {
        notifyTown(event.getTown());
    }

    protected void notifyTown(Town town) {
        if (town != null) {
            List<UUID> players = new ArrayList<>();
            for (Resident resident : town.getResidents()) {
                players.add(resident.getUUID());
            }
            notifyAll(from(town), players);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTownDelete(PreDeleteTownEvent event) {
        deleteAll(Claim.deletion(event.getTown().getUUID().hashCode(), event.getTown().getWorld().getKey()));
    }

    @Override
    protected List<Claim> getClaims(ChunkPos chunk) {
        World world = chunk.getWorld();
        if (world == null) {
            return NONE;
        }
        Town town = api.getTown(new Location(world, chunk.x() << 4, 0, chunk.z() << 4));
        return town == null ? NONE : Collections.singletonList(from(town));
    }

    public Claim from(Town town) {
        NamespacedKey worldKey = town.getWorld().getKey();
        Color townColor = town.getMapColor();
        Claim claim = new Claim(
                town.getUUID().hashCode(),
                town.getMayor().getUUID(),
                town.getFormattedName(),
                worldKey,
                townColor == null ? ClaimMessenger.locatorBarColor(town.getMayor().getUUID()) : townColor.getRGB()
        );

        for (TownBlock townBlock : town.getTownBlocks()) {
            WorldCoord coord = townBlock.getWorldCoord();
            for (int x = 0; x < side; ++x) {
                for (int z = 0; z < side; ++z) {
                    claim.chunks().add(new ChunkPos(worldKey, x + coord.getX(), z + coord.getZ()));
                }
            }
        }

        return claim;
    }
}
