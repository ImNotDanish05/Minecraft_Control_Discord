package com.example.discordcontrol;

import com.example.discordcontrol.commands.ClientCommands;
import com.example.discordcontrol.config.ConfigManager;
import com.example.discordcontrol.discord.DiscordManager;
import com.example.discordcontrol.features.BlindfoldRenderer;
import com.example.discordcontrol.features.FollowController;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class DiscordControlMod implements ClientModInitializer {
    public static final String MOD_ID = "discord-control";
    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        new Thread(DiscordManager::init, "Discord-Bot").start();
        ClientCommands.register();
        WorldRenderEvents.END.register(BlindfoldRenderer::render);
        FollowController.init();
    }
}
