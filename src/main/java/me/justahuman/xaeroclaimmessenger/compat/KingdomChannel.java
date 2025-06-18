package me.justahuman.xaeroclaimmessenger.compat;

import me.justahuman.xaeroclaimmessenger.ChunkPos;
import me.justahuman.xaeroclaimmessenger.Claim;
import me.justahuman.xaeroclaimmessenger.ClaimChannel;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.kingdoms.constants.group.Kingdom;
import org.kingdoms.constants.land.Land;
import org.kingdoms.constants.land.location.SimpleChunkLocation;
import org.kingdoms.constants.namespace.Namespace;
import org.kingdoms.server.location.World;

import java.util.HashSet;
import java.util.List;

public class KingdomChannel extends ClaimChannel {
    @Override
    protected List<Claim> getClaims(ChunkPos chunk) {
        Location location = chunk.toLocation();
        if (location == null) {
            return List.of();
        }
        SimpleChunkLocation chunkLocation = SimpleChunkLocation.of(location);
        Land land = chunkLocation.getLand();
        if (land == null || !land.isClaimed()) {
            return List.of();
        }
        return List.of(from(chunk.world(), land.getKingdom()));
    }

    private Claim from(NamespacedKey worldKey, Kingdom kingdom) {
        
        Claim claim = new Claim(
                kingdom.getId().hashCode(),
                kingdom.getOwnerId(),
                kingdom.getName(),
                worldKey,
                new HashSet<>(),
                kingdom.getColors().get(kingdom.getGroup().getNexus())
        )
    }
}
