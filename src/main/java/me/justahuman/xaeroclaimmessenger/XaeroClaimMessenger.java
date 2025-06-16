package me.justahuman.xaeroclaimmessenger;

import me.justahuman.xaeroclaimmessenger.compat.GriefPreventionChannel;
import me.justahuman.xaeroclaimmessenger.compat.HuskTownsChannel;
import me.justahuman.xaeroclaimmessenger.compat.LandsChannel;
import me.justahuman.xaeroclaimmessenger.compat.TownyChannel;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.*;
import java.util.UUID;
import java.util.function.Supplier;

public final class XaeroClaimMessenger extends JavaPlugin {
    public static final String GP = "GriefPrevention";
    public static final String HUSK_TOWNS = "HuskTowns";
    public static final String LANDS = "Lands";
    public static final String TOWNY = "Towny";

    public static final String CLAIM_CHANNEL = channel("claim");
    public static final String DELETE_CHANNEL = channel("delete_claim");
    private static XaeroClaimMessenger instance;

    @Override
    public void onEnable() {
        instance = this;
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, CLAIM_CHANNEL);
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, DELETE_CHANNEL);

        this.tryRegisterChannel(
                new PluginChannel(HUSK_TOWNS, () -> HuskTownsChannel::new),
                new PluginChannel(LANDS, () -> LandsChannel::new),
                new PluginChannel(GP, () -> GriefPreventionChannel::new),
                new PluginChannel(TOWNY, () -> TownyChannel::new)
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
        return "xaeropluginclaims:" + path;
    }

    public static XaeroClaimMessenger getInstance() {
        return instance;
    }

    record PluginChannel(String name, Supplier<Supplier<? extends ClaimChannel>> channelSupplier) {}
}
