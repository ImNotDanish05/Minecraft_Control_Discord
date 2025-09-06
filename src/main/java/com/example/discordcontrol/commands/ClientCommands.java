package com.example.discordcontrol.commands;

import com.example.discordcontrol.features.FollowController;
import com.example.discordcontrol.features.ScreenshotUtil;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public final class ClientCommands {
    private ClientCommands() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("ss").executes(ctx -> { ScreenshotUtil.takeAndSend(); return 1; }));
            dispatcher.register(literal("leash")
                    .then(argument("player", StringArgumentType.word())
                            .executes(ctx -> {
                                FollowController.start(StringArgumentType.getString(ctx, "player"));
                                return 1;
                            })));
            dispatcher.register(literal("unleash").executes(ctx -> { FollowController.stop(); return 1; }));
        });
    }
}
