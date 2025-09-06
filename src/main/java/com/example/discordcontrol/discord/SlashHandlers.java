package com.example.discordcontrol.discord;

import com.example.discordcontrol.features.BlindfoldRenderer;
import com.example.discordcontrol.features.GagFilter;

public final class SlashHandlers {
    private SlashHandlers() {}

    public static void toggleGag() {
        GagFilter.gagged = !GagFilter.gagged;
    }

    public static void toggleBlindfold() {
        BlindfoldRenderer.toggle();
    }
}
