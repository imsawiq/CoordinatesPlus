package org.sawiq.group;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.sawiq.config.ConfigScreen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GroupHudRenderer {
    private final MinecraftClient mc;
    private final GroupManager groupManager;

    private int lastWidth;
    private int lastHeight;

    public GroupHudRenderer(MinecraftClient mc, GroupManager groupManager) {
        this.mc = mc;
        this.groupManager = groupManager;
    }

    public void render(DrawContext context, int startX, int startY, float scale, boolean editMode, String themeDimensionKey) {
        if (!groupManager.isInGroup() || mc.player == null) return;

        List<GroupMemberState> members = new ArrayList<>(groupManager.getMembers());
        if (members.isEmpty()) return;

        members.removeIf(m -> m == null || m.uuid == null || m.uuid.equals(mc.player.getUuid()));
        if (members.isEmpty()) return;

        members.sort(Comparator.comparing(a -> a.name == null ? "" : a.name));

        int padding = 6;
        int lineHeight = mc.textRenderer.fontHeight;
        int headSize = 14;
        int rowHeight = Math.max(lineHeight * 2 + 2, headSize) + 2;

        int maxTextWidth = 0;
        List<String> line1 = new ArrayList<>(members.size());
        List<String> line2 = new ArrayList<>(members.size());
        for (GroupMemberState m : members) {
            String name = m.name == null ? "?" : m.name;
            String prefix = formatRadarPrefix(m);
            line1.add(prefix + name);
            String coords = formatCoordsLine(m);
            line2.add(coords);
            maxTextWidth = Math.max(maxTextWidth, mc.textRenderer.getWidth(prefix + name));
            maxTextWidth = Math.max(maxTextWidth, getCoordsWidth(m));
        }

        if (line1.isEmpty()) return;

        int width = padding * 2 + headSize + 6 + maxTextWidth;
        int height = padding * 2 + rowHeight * members.size();

        lastWidth = (int) (width * scale);
        lastHeight = (int) (height * scale);

        int bg1;
        int bg2;
        int border;

        boolean isInNether = "minecraft:the_nether".equals(themeDimensionKey);
        boolean isInEnd = "minecraft:the_end".equals(themeDimensionKey);

        if (isInNether) {
            if (editMode) {
                bg1 = 0xDD2E0F0F;
                bg2 = 0xDD5C1414;
                border = 0xFFFF6B6B;
            } else {
                bg1 = 0xBB3D1616;
                bg2 = 0xBB6B1F1F;
                border = 0xFFFF5555;
            }
        } else if (isInEnd) {
            if (editMode) {
                bg1 = 0xDD1F0F2E;
                bg2 = 0xDD3D1460;
                border = 0xFFD953E4;
            } else {
                bg1 = 0xBB2B1642;
                bg2 = 0xBB4A1F6B;
                border = 0xFFBF5FFF;
            }
        } else {
            if (editMode) {
                bg1 = 0xDD1A1A2E;
                bg2 = 0xDD0F3460;
                border = 0xFF53E4D4;
            } else {
                bg1 = 0xBB16213E;
                bg2 = 0xBB0F3460;
                border = 0xFF00D9FF;
            }
        }

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(startX, startY);
        context.getMatrices().scale(scale, scale);

        if (ConfigScreen.isGroupBackgroundEnabled()) {
            context.fillGradient(0, 0, width, height, bg1, bg2);
            context.fill(0, 0, width, 1, border);
            context.fill(0, height - 1, width, height, border);
            context.fill(0, 0, 1, height, border);
            context.fill(width - 1, 0, width, height, border);

            if (editMode) {
                String editIcon = "✥";
                int iconWidth = mc.textRenderer.getWidth(editIcon);
                int iconX = width - iconWidth - padding;
                int iconY = height - lineHeight - padding + 1;
                context.drawTextWithShadow(mc.textRenderer, editIcon, iconX, iconY, border);
            }
        }

        int y = padding;
        for (int i = 0; i < members.size(); i++) {
            GroupMemberState m = members.get(i);

            int headX = padding;
            int headY = y + 1;
            SkinRenderUtil.tryDrawHead(context, mc, m.uuid, headX, headY, headSize);

            int textX = padding + headSize + 6;
            int textY1 = y;
            int textY2 = y + lineHeight + 2;

            context.drawTextWithShadow(mc.textRenderer, line1.get(i), textX, textY1, 0xFFFFFFFF);
            drawCoordsColored(context, m, textX, textY2);
            y += rowHeight;
        }

        context.getMatrices().popMatrix();
    }

    private String formatCoordsLine(GroupMemberState m) {
        String dim = m.lastDimensionKey == null ? "" : m.lastDimensionKey;
        boolean inNether = "minecraft:the_nether".equals(dim);
        boolean inEnd = "minecraft:the_end".equals(dim);

        if (inNether) {
            return "N: " + m.netherX + " " + m.netherY + " " + m.netherZ + "  OW: " + m.overworldX + " " + m.overworldZ;
        }
        if (inEnd) {
            return "End: " + m.overworldX + " " + m.overworldY + " " + m.overworldZ;
        }
        return "OW: " + m.overworldX + " " + m.overworldY + " " + m.overworldZ + "  N: " + m.netherX + " " + m.netherZ;
    }

    private String formatRadarPrefix(GroupMemberState m) {
        if (mc.player == null) return "";

        String selfDim = mc.player.getWorld().getRegistryKey().getValue().toString();
        boolean selfNether = "minecraft:the_nether".equals(selfDim);

        double dx;
        double dz;
        if (selfNether) {
            int selfX = (int) Math.round(mc.player.getX());
            int selfZ = (int) Math.round(mc.player.getZ());
            dx = (double) m.netherX - selfX;
            dz = (double) m.netherZ - selfZ;
        } else {
            int selfX = (int) Math.round(mc.player.getX());
            int selfZ = (int) Math.round(mc.player.getZ());
            dx = (double) m.overworldX - selfX;
            dz = (double) m.overworldZ - selfZ;
        }

        int d = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        String arrow = getDirectionArrow(dx, dz);
        return arrow + " " + d + "m ";
    }

    private String getDirectionArrow(double dx, double dz) {
        if (mc.player == null) return "";
        if (dx == 0 && dz == 0) return "•";

        // World coords: +X = East, +Z = South.
        // Minecraft yaw: 0 = South (+Z), 90 = West (-X), -90 = East (+X).
        double yawRad = Math.toRadians(mc.player.getYaw());
        double fx = -Math.sin(yawRad);
        double fz = Math.cos(yawRad);
        // Right vector = forward rotated 90° to the right in Minecraft's coordinate system.
        // (rx, rz) = (-fz, fx)
        double rx = -fz;
        double rz = fx;

        double forward = dx * fx + dz * fz;
        double right = dx * rx + dz * rz;

        // 0 = forward, +90 = right, -90 = left
        double rel = Math.atan2(right, forward);
        double deg = Math.toDegrees(rel);
        if (deg >= -22.5 && deg < 22.5) return "↑";
        if (deg >= 22.5 && deg < 67.5) return "↗";
        if (deg >= 67.5 && deg < 112.5) return "→";
        if (deg >= 112.5 && deg < 157.5) return "↘";
        if (deg >= 157.5 || deg < -157.5) return "↓";
        if (deg >= -157.5 && deg < -112.5) return "↙";
        if (deg >= -112.5 && deg < -67.5) return "←";
        return "↖";
    }

    private void drawCoordsColored(DrawContext context, GroupMemberState m, int x, int y) {
        String dim = m.lastDimensionKey == null ? "" : m.lastDimensionKey;
        boolean inNether = "minecraft:the_nether".equals(dim);
        boolean inEnd = "minecraft:the_end".equals(dim);

        if (inEnd) {
            String end = "End: " + m.overworldX + " " + m.overworldY + " " + m.overworldZ;
            context.drawTextWithShadow(mc.textRenderer, end, x, y, 0xFFBF5FFF);
            return;
        }

        String ow = "OW: " + m.overworldX + " " + m.overworldY + " " + m.overworldZ;
        int gap = 6;
        String ne = "N: " + m.netherX + " " + m.netherY + " " + m.netherZ;

        if (inNether) {
            context.drawTextWithShadow(mc.textRenderer, ne, x, y, 0xFFFF5555);
            int x2 = x + mc.textRenderer.getWidth(ne) + gap;
            context.drawTextWithShadow(mc.textRenderer, ow, x2, y, 0xFF55FF55);
        } else {
            context.drawTextWithShadow(mc.textRenderer, ow, x, y, 0xFF55FF55);
            int x2 = x + mc.textRenderer.getWidth(ow) + gap;
            context.drawTextWithShadow(mc.textRenderer, ne, x2, y, 0xFFFF5555);
        }
    }

    private int getCoordsWidth(GroupMemberState m) {
        String dim = m.lastDimensionKey == null ? "" : m.lastDimensionKey;
        boolean inNether = "minecraft:the_nether".equals(dim);
        boolean inEnd = "minecraft:the_end".equals(dim);

        if (inEnd) {
            String end = "End: " + m.overworldX + " " + m.overworldY + " " + m.overworldZ;
            return mc.textRenderer.getWidth(end);
        }

        String ow = "OW: " + m.overworldX + " " + m.overworldY + " " + m.overworldZ;
        String ne = "N: " + m.netherX + " " + m.netherY + " " + m.netherZ;
        int gap = 6;

        if (inNether) {
            return mc.textRenderer.getWidth(ne) + gap + mc.textRenderer.getWidth(ow);
        }
        return mc.textRenderer.getWidth(ow) + gap + mc.textRenderer.getWidth(ne);
    }

    public int getLastWidth() {
        return lastWidth;
    }

    public int getLastHeight() {
        return lastHeight;
    }
}
