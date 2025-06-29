package me.justahuman.claimmessenger;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public record Claim(long id, @Nullable UUID owner, String customName, NamespacedKey worldKey, Set<ChunkPos> chunks, int color) {
    public byte[] serialize() {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
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
        output.writeUTF(worldKey.toString());
        output.writeLong(id);
        return output.toByteArray();
    }

    public static Claim deletion(long id, NamespacedKey worldKey) {
        return new Claim(id, null, "", worldKey, Set.of(), 0);
    }
}
