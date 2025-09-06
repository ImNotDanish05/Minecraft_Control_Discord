package com.example.discordcontrol.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.MinecraftClient;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "discord_control.json";
    private static Config CONFIG = new Config();

    private ConfigManager() {}

    public static Config get() {
        return CONFIG;
    }

    public static void load() {
        Path path = getPath();
        if (Files.exists(path)) {
            try {
                CONFIG = GSON.fromJson(Files.readString(path), Config.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            save();
        }
    }

    public static void save() {
        Path path = getPath();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(CONFIG));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Path getPath() {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve("config").resolve(FILE_NAME);
    }
}
