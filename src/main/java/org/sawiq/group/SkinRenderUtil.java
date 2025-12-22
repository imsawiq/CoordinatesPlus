package org.sawiq.group;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;

import java.util.UUID;

public final class SkinRenderUtil {
    private SkinRenderUtil() {
    }

    public static void tryDrawHead(DrawContext context, MinecraftClient mc, UUID uuid, int x, int y, int size) {
        if (mc == null || mc.getNetworkHandler() == null || uuid == null) return;

        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(uuid);
        if (entry == null) {
            drawPlaceholder(context, x, y, size);
            return;
        }

        PlayerSkinDrawer.draw(context, entry.getSkinTextures(), x, y, size);
    }

    private static void drawPlaceholder(DrawContext context, int x, int y, int size) {
        int bg = 0xAA0F3460;
        int border = 0xFF00D9FF;
        context.fill(x, y, x + size, y + size, bg);
        context.fill(x, y, x + size, y + 1, border);
        context.fill(x, y + size - 1, x + size, y + size, border);
        context.fill(x, y, x + 1, y + size, border);
        context.fill(x + size - 1, y, x + size, y + size, border);
    }
}
