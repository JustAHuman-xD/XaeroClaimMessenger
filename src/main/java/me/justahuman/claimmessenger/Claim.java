package me.justahuman.claimmessenger;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.NamespacedKey;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
public record Claim(long id, @Nullable UUID owner, String customName, NamespacedKey worldKey, Set<ChunkPos> chunks, int color, boolean deletion) {
    public Claim(long id, @Nullable UUID owner, String customName, NamespacedKey worldKey) {
        this(id, owner, customName, worldKey, ClaimMessenger.locatorBarColor(owner));
    }

    public Claim(long id, @Nullable UUID owner, String customName, NamespacedKey worldKey, int color) {
        this(id, owner, customName, worldKey, new HashSet<>(), color);
    }

    public Claim(long id, @Nullable UUID owner, String customName, NamespacedKey worldKey, Set<ChunkPos> chunks, int color) {
        this(id, owner, customName, worldKey, chunks, color, false);
    }

    public byte[] serialize() {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeInt(ClaimMessenger.DATA_VERSION);
        output.writeLong(id);
        output.writeLong(owner == null ? -1 : owner.getMostSignificantBits());
        output.writeLong(owner == null ? - 1 : owner.getLeastSignificantBits());
        output.writeUTF(customName);
        output.writeUTF(worldKey.toString());
        output.writeInt(chunks.size());
        for (ChunkPos chunk : chunks) {
            output.writeInt(chunk.x());
            output.writeInt(chunk.z());
        }
        output.writeInt(color);
        return output.toByteArray();
    }

    public byte[] serializeDeletion() {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeInt(ClaimMessenger.DATA_VERSION);
        output.writeUTF(worldKey.toString());
        output.writeLong(id);
        return output.toByteArray();
    }

    public static Claim deletion(long id, NamespacedKey worldKey) {
        return new Claim(id, null, "", worldKey, Collections.emptySet(), 0, true);
    }
}
