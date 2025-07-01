package me.justahuman.claimmessenger;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public record ChunkPos(NamespacedKey world, int x, int z) {
    private static final Map<NamespacedKey, WeakReference<World>> WORLD_CACHE = new HashMap<>();

    public World getWorld() {
        return WORLD_CACHE.compute(world, (key, ref) -> {
            World world = ref == null ? null : ref.get();
            if (world == null) {
                for (World w : Bukkit.getWorlds()) {
                    if (w.getKey().equals(key)) {
                        return new WeakReference<>(w);
                    }
                }
                throw new IllegalStateException("World not found: " + key);
            }
            return ref;
        }).get();
    }

    public Location toLocation() {
        World world = getWorld();
        return world == null ? null : new Location(world, x * 16, 0, z * 16);
    }

    public byte[] serializeNoClaims() {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeInt(ClaimMessenger.DATA_VERSION);
        output.writeUTF(world.toString());
        output.writeInt(x);
        output.writeInt(z);
        return output.toByteArray();
    }
}
