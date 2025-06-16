package me.justahuman.xaeroclaimmessenger;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;

public record ChunkPos(NamespacedKey world, int x, int z) {
    public World getWorld() {
        return Bukkit.getWorld(world);
    }

    public int regionX() {
        return x >> 5;
    }

    public int regionZ() {
        return z >> 5;
    }
}
