package com.example.discordcontrol.features;

import com.example.discordcontrol.config.ConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;

public final class FollowController {
    private static String targetName = null;

    private FollowController() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    public static void start(String name) {
        targetName = name;
    }

    public static void stop() {
        targetName = null;
    }

    private static void tick() {
        if (targetName == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;
        PlayerEntity target = mc.world.getPlayers().stream()
                .filter(p -> p.getName().getString().equals(targetName))
                .findFirst().orElse(null);
        if (target == null) return;
        double dist = player.squaredDistanceTo(target);
        double stopDist = ConfigManager.get().followStopDistance * ConfigManager.get().followStopDistance;
        if (dist < stopDist) return;
        player.setYaw((float) (Math.toDegrees(Math.atan2(target.getZ() - player.getZ(), target.getX() - player.getX())) - 90));
        player.input.pressingForward = true;
        player.setSprinting(ConfigManager.get().followMaxSpeed > 0.9);
    }
}
