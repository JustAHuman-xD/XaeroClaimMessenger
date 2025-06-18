package me.justahuman.xaeroclaimmessenger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;

public record ChunkPos(NamespacedKey world, int x, int z) {
    public World getWorld() {
        return Bukkit.getWorld(world);
    }

    public Location toLocation() {
        World world = getWorld();
        return world == null ? null : new Location(world, x * 16, 0, z * 16);
    }
}
