package me.justahuman.claimmessenger;

import me.justahuman.claimmessenger.compat.GriefPreventionChannel;
import me.justahuman.claimmessenger.compat.HuskTownsChannel;
import me.justahuman.claimmessenger.compat.LandsChannel;
import me.justahuman.claimmessenger.compat.ResidenceChannel;
import me.justahuman.claimmessenger.compat.TownyChannel;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.*;
import java.util.UUID;
import java.util.function.Supplier;

public final class ClaimMessenger extends JavaPlugin {
    public static final String GP = "GriefPrevention";
    public static final String HUSK_TOWNS = "HuskTowns";
    public static final String LANDS = "Lands";
    public static final String TOWNY = "Towny";
    public static final String RESIDENCE = "Residence";

    public static final String CLAIM_CHANNEL = channel("claim");
    public static final String NO_CLAIMS_CHANNEL = channel("no_claims");
    public static final String DELETE_CHANNEL = channel("delete_claim");
    private static ClaimMessenger instance;

    @Override
    public void onEnable() {
        instance = this;
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, CLAIM_CHANNEL);
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, NO_CLAIMS_CHANNEL);
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, DELETE_CHANNEL);

        this.tryRegisterChannel(
                new PluginChannel(HUSK_TOWNS, () -> HuskTownsChannel::new),
                new PluginChannel(LANDS, () -> LandsChannel::new),
                new PluginChannel(GP, () -> GriefPreventionChannel::new),
                new PluginChannel(TOWNY, () -> TownyChannel::new),
                new PluginChannel(RESIDENCE, () -> ResidenceChannel::new)
        );
    }

    private void tryRegisterChannel(PluginChannel... channels) {
        String registered = null;
        PluginManager pluginManager = this.getServer().getPluginManager();
        for (PluginChannel channel : channels) {
            if (pluginManager.isPluginEnabled(channel.name)) {
                if (registered != null) {
                    getSLF4JLogger().warn("Tried to register {} but {} is already registered!", channel.name, channel.name);
                    continue;
                }

                try {
                    channel.channelSupplier.get().get();
                    registered = channel.name;
                } catch (Throwable e) {
                    getSLF4JLogger().error("Failed to register channel for {}", channel.name, e);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static int locatorBarColor(UUID uuid) {
        if (uuid == null) {
            return Color.RED.getRGB();
        }
        int hash = uuid.hashCode();
        return new Color(hash >> 16 & 255, hash >> 8 & 255, hash & 255).getRGB();
    }

    public static String channel(String path) {
        return "pluginclaims:" + path;
    }

    public static ClaimMessenger getInstance() {
        return instance;
    }

    record PluginChannel(String name, Supplier<Supplier<? extends ClaimChannel>> channelSupplier) {}
}
