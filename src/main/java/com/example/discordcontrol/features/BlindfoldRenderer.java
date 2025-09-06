package com.example.discordcontrol.features;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

public final class BlindfoldRenderer {
    private static boolean blindfolded = false;

    private BlindfoldRenderer() {}

    public static void toggle() {
        blindfolded = !blindfolded;
    }

    public static boolean isBlindfolded() {
        return blindfolded;
    }

    public static void render(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext ctx) {
        if (!blindfolded) return;
        MinecraftClient mc = ctx.gameRenderer().getClient();
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        RenderSystem.disableDepthTest();
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        b.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        b.vertex(0, h, 0).color(0,0,0,255).next();
        b.vertex(w, h, 0).color(0,0,0,255).next();
        b.vertex(w, 0, 0).color(0,0,0,255).next();
        b.vertex(0, 0, 0).color(0,0,0,255).next();
        BufferRenderer.drawWithGlobalProgram(b.end());
        RenderSystem.enableDepthTest();
    }
}
