package com.example.discordcontrol.discord;

import com.example.discordcontrol.config.Config;
import com.example.discordcontrol.config.ConfigManager;
import com.example.discordcontrol.discord.SlashHandlers;
import com.example.discordcontrol.features.GagFilter;
import com.example.discordcontrol.features.BlindfoldRenderer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import javax.annotation.Nonnull;
import java.nio.file.Path;

public final class DiscordManager extends ListenerAdapter {
    private static net.dv8tion.jda.api.JDA JDA;

    private DiscordManager() {}

    public static void init() {
        Config cfg = ConfigManager.get();
        try {
            JDA = JDABuilder.createLight(cfg.discordToken).addEventListeners(new DiscordManager()).build().awaitReady();
            if (!cfg.guildId.isEmpty()) {
                Guild guild = JDA.getGuildById(cfg.guildId);
                if (guild != null) {
                    guild.updateCommands().addCommands(
                            Commands.slash("ballgag", "Toggle gag"),
                            Commands.slash("blindfold", "Toggle blindfold")
                    ).queue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        Config cfg = ConfigManager.get();
        if (!cfg.controlRoleId.isEmpty() && event.getMember() != null &&
                event.getMember().getRoles().stream().noneMatch(r -> r.getId().equals(cfg.controlRoleId))) {
            event.reply("not allowed").setEphemeral(true).queue();
            return;
        }
        switch (event.getName()) {
            case "ballgag" -> {
                SlashHandlers.toggleGag();
                event.replyEmbeds(new EmbedBuilder().setTitle("Gag")
                        .setDescription("Gag is now " + (GagFilter.gagged ? "ON" : "OFF")).build()).queue();
            }
            case "blindfold" -> {
                SlashHandlers.toggleBlindfold();
                event.replyEmbeds(new EmbedBuilder().setTitle("Blindfold")
                        .setDescription("Blindfold is now " + (BlindfoldRenderer.isBlindfolded() ? "ON" : "OFF")).build()).queue();
            }
        }
    }

    public static void sendImageToChannel(Path file, String caption) {
        Config cfg = ConfigManager.get();
        if (JDA == null) return;
        var channel = JDA.getTextChannelById(cfg.channelId);
        if (channel != null) {
            channel.sendMessage(caption).addFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(file.toFile())).queue();
        }
    }
}
