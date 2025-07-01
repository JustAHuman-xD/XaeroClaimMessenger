package me.justahuman.claimmessenger;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import me.justahuman.claimmessenger.compat.*;
import me.justahuman.claimmessenger.compat.TownyChannel;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.function.Supplier;

public final class ClaimMessenger extends JavaPlugin {
    public static final int DATA_VERSION = 1;

    public static final String GP = "GriefPrevention";
    public static final String HUSK_TOWNS = "HuskTowns";
    public static final String HUSK_CLAIMS = "HuskClaims";
    public static final String LANDS = "Lands";
    public static final String TOWNY = "Towny";
    public static final String RESIDENCE = "Residence";
    public static final String KINGDOMS = "Kingdoms";

    public static final String CLAIM_CHANNEL = channel("claim");
    public static final String NO_CLAIMS_CHANNEL = channel("no_claims");
    public static final String DELETE_CHANNEL = channel("delete_claim");

    private static ClaimMessenger instance;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        instance = this;

        PacketEvents.getAPI().init();

        this.getServer().getMessenger().registerOutgoingPluginChannel(this, CLAIM_CHANNEL);
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, NO_CLAIMS_CHANNEL);
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, DELETE_CHANNEL);

        this.tryRegisterChannel(
                new PluginChannel(HUSK_CLAIMS, () -> HuskClaimsChannel::new),
                new PluginChannel(HUSK_TOWNS, () -> HuskTownsChannel::new),
                new PluginChannel(LANDS, () -> LandsChannel::new),
                new PluginChannel(GP, () -> GriefPreventionChannel::new),
                new PluginChannel(TOWNY, () -> TownyChannel::new),
                new PluginChannel(RESIDENCE, () -> ResidenceChannel::new),
                new PluginChannel(KINGDOMS, () -> KingdomsChannel::new)
        );
    }

    private void tryRegisterChannel(PluginChannel... channels) {
        String registered = null;
        PluginManager pluginManager = this.getServer().getPluginManager();
        for (PluginChannel channel : channels) {
            if (pluginManager.isPluginEnabled(channel.name)) {
                if (registered != null) {
                    getLogger().warning(String.format("Tried to register %s but %s is already registered!", channel.name, registered));
                    continue;
                }

                try {
                    channel.channelSupplier.get().get();
                    registered = channel.name;
                } catch (Throwable e) {
                    getLogger().severe(String.format("Failed to register channel for %s: %s", channel.name, e));
                }
            }
        }
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
    }

    public static long pack(int x, int z) {
        return x & 4294967295L | (z & 4294967295L) << 32;
    }

    public static int[] unpack(long packed) {
        return new int[] { (int) packed, (int) (packed >> 32) };
    }

    public static int locatorBarColor(UUID uuid) {
        if (uuid == null) {
            return 0xFF0000;
        }
        int hash = uuid.hashCode();
        int r = (hash >> 16) & 0xFF;
        int g = (hash >> 8) & 0xFF;
        int b = hash & 0xFF;
        return (r << 16) | (g << 8) | b;
    }

    public static String channel(String path) {
        return "pluginclaims:" + path;
    }

    public static ClaimMessenger getInstance() {
        return instance;
    }

    record PluginChannel(String name, Supplier<Supplier<? extends ClaimChannel>> channelSupplier) {}
}
