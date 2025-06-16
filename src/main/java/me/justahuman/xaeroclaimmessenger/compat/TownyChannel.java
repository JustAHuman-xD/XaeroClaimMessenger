package me.justahuman.xaeroclaimmessenger.compat;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.DeleteTownEvent;
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
import me.justahuman.xaeroclaimmessenger.ChunkPos;
import me.justahuman.xaeroclaimmessenger.Claim;
import me.justahuman.xaeroclaimmessenger.ClaimChannel;
import me.justahuman.xaeroclaimmessenger.XaeroClaimMessenger;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TownyChannel extends ClaimChannel {
    private final TownyAPI api = TownyAPI.getInstance();
    private final int side = WorldCoord.getCellSize() / 16;
    // TODO: this breaks if side is anything more than 1, fix that at some point

    public TownyChannel() {
        int cellSize = WorldCoord.getCellSize();
        if (cellSize < 16 || cellSize % 16 != 0) {
            throw new IllegalStateException("Townys cell size must be a product of 16 for XaeroClaimMessenger!");
        }
    }

    protected void updateTown(Town town) {
        Claim claim = from(town);
        UUID[] residents = town.getResidents().stream().map(Resident::getUUID).toArray(UUID[]::new);
        for (UUID resident : residents) {
            sendClaim(resident, claim);
        }
        notifyWithin(claim, residents);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNewTown(NewTownEvent event) {
        updateTown(event.getTown());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMayorChanged(TownMayorChangedEvent event) {
        updateTown(event.getTown());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRename(RenameTownEvent event) {
        updateTown(event.getTown());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClaim(TownClaimEvent event) {
        updateTown(event.getTown());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUnclaim(TownUnclaimEvent event) {
        updateTown(event.getTown());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTownDelete(PreDeleteTownEvent event) {
        Town town = event.getTown();
        Claim claim = Claim.deletion(town.getUUID().hashCode(), town.getWorld().getKey());
        UUID[] residents = town.getResidents().stream().map(Resident::getUUID).toArray(UUID[]::new);
        for (UUID resident : residents) {
            deleteClaim(resident, claim);
        }
        deleteWithin(claim, residents);
    }

    @Override
    protected List<Claim> getClaims(ChunkPos chunk) {
        World world = chunk.getWorld();
        if (world == null) {
            return List.of();
        }
        Town town = api.getTown(new Location(world, chunk.x() << 4, 0, chunk.z() << 4));
        return town == null ? List.of() : List.of(from(town));
    }

    public Claim from(Town town) {
        NamespacedKey worldKey = town.getWorld().getKey();
        Color townColor = town.getMapColor();
        Claim claim = new Claim(
                town.getUUID().hashCode(),
                town.getMayor().getUUID(),
                town.getFormattedName(),
                worldKey,
                new ArrayList<>(),
                townColor == null ? XaeroClaimMessenger.locatorBarColor(town.getMayor().getUUID()) : townColor.getRGB()
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
