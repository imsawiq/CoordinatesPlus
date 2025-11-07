package org.sawiq;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.sawiq.config.ConfigScreen;

public class CoordinatesPlus implements ClientModInitializer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static boolean isDragging = false;
    private static double dragOffsetX = 0;
    private static double dragOffsetY = 0;

    private static KeyBinding editModeKey;
    private static KeyBinding sendOverworldCoordsKey;
    private static KeyBinding sendNetherCoordsKey;
    private static boolean editMode = false;

    @Override
    public void onInitializeClient() {
        editModeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.coordinatesplus.edit_mode",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_ALT,
                "category.coordinatesplus"
        ));

        sendOverworldCoordsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.coordinatesplus.send_overworld",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.coordinatesplus"
        ));

        sendNetherCoordsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.coordinatesplus.send_nether",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "category.coordinatesplus"
        ));

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            renderHud(drawContext);
        });

        System.out.println("Coordinates Plus initialized");
    }

    private void renderHud(DrawContext context) {
        if (!ConfigScreen.isHudEnabled() || mc.player == null) return;

        if (mc.options.hudHidden || mc.getDebugHud().shouldShowDebugHud()) {
            return;
        }

        while (editModeKey.wasPressed()) {
            editMode = !editMode;
            if (!editMode) {
                ConfigScreen.savePosition();
            }
        }

        while (sendOverworldCoordsKey.wasPressed()) {
            if (mc.player != null) {
                String prefix = ConfigScreen.useCommandPrefix() ? "!" : "";
                mc.player.networkHandler.sendChatMessage(prefix + "+loco");
            }
        }

        while (sendNetherCoordsKey.wasPressed()) {
            if (mc.player != null) {
                String prefix = ConfigScreen.useCommandPrefix() ? "!" : "";
                mc.player.networkHandler.sendChatMessage(prefix + "+locn");
            }
        }

        // обработка перетаскивания
        handleDragging();

        double rawX = mc.player.getX();
        double rawY = mc.player.getY();
        double rawZ = mc.player.getZ();

        String dimensionKey = mc.player.getWorld().getRegistryKey().getValue().toString();
        boolean isInNether = dimensionKey.equals("minecraft:the_nether");
        boolean isInEnd = dimensionKey.equals("minecraft:the_end");

        int otherX, otherZ;
        Text dimensionLabel;
        int dimensionColor;

        if (isInNether) {
            otherX = (int) Math.round(rawX * 8.0);
            otherZ = (int) Math.round(rawZ * 8.0);
            dimensionLabel = Text.translatable("text.coordinatesplus.dimension.overworld");
            dimensionColor = 0xFF55FF55; // ARGB
        } else if (isInEnd) {
            otherX = (int) Math.round(rawX);
            otherZ = (int) Math.round(rawZ);
            dimensionLabel = Text.translatable("text.coordinatesplus.dimension.end");
            dimensionColor = 0xFF888888; // ARGB
        } else {
            otherX = (int) Math.round(rawX / 8.0);
            otherZ = (int) Math.round(rawZ / 8.0);
            dimensionLabel = Text.translatable("text.coordinatesplus.dimension.nether");
            dimensionColor = 0xFFFF5555; // ARGB
        }

        int posX = (int) Math.round(rawX);
        int posY = (int) Math.round(rawY);
        int posZ = (int) Math.round(rawZ);

        float hudX = ConfigScreen.getHudX();
        float hudY = ConfigScreen.getHudY();
        float scale = ConfigScreen.getTextScale();

        // фикс шакала
        hudX = Math.round(hudX);
        hudY = Math.round(hudY);

        //  matrix3x2fstack
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(hudX, hudY);
        matrices.scale(scale, scale);

        String coordText = String.format("X: %d  Y: %d  Z: %d", posX, posY, posZ);
        String otherText = String.format("%s: [%d, %d]", dimensionLabel.getString(), otherX, otherZ);

        int coordWidth = mc.textRenderer.getWidth(coordText);
        int otherWidth = mc.textRenderer.getWidth(otherText);
        int maxWidth = Math.max(coordWidth, otherWidth);

        int padding = 6;
        int totalWidth = maxWidth + padding * 2;
        int lineHeight = mc.textRenderer.fontHeight;
        int totalHeight = lineHeight * 2 + padding * 2 + 2;

        if (ConfigScreen.isBackgroundEnabled()) {
            int bgColor1, bgColor2, borderColor;

            if (isInNether) {
                if (editMode) {
                    bgColor1 = 0xDD2E0F0F;
                    bgColor2 = 0xDD5C1414;
                    borderColor = 0xFFFF6B6B;
                } else {
                    bgColor1 = 0xBB3D1616;
                    bgColor2 = 0xBB6B1F1F;
                    borderColor = 0xFFFF5555;
                }
            } else if (isInEnd) {
                if (editMode) {
                    bgColor1 = 0xDD1F0F2E;
                    bgColor2 = 0xDD3D1460;
                    borderColor = 0xFFD953E4;
                } else {
                    bgColor1 = 0xBB2B1642;
                    bgColor2 = 0xBB4A1F6B;
                    borderColor = 0xFFBF5FFF;
                }
            } else {
                if (editMode) {
                    bgColor1 = 0xDD1A1A2E;
                    bgColor2 = 0xDD0F3460;
                    borderColor = 0xFF53E4D4;
                } else {
                    bgColor1 = 0xBB16213E;
                    bgColor2 = 0xBB0F3460;
                    borderColor = 0xFF00D9FF;
                }
            }

            // основной фон
            context.fillGradient(0, 0, totalWidth, totalHeight, bgColor1, bgColor2);

            // обводка
            drawBorder(context, 0, 0, totalWidth, totalHeight, 1, borderColor);

            int lineY = lineHeight + padding + 1;
            int lineColor;
            if (isInNether) {
                lineColor = 0x44FF5555;
            } else if (isInEnd) {
                lineColor = 0x44BF5FFF;
            } else {
                lineColor = 0x4400D9FF;
            }
            context.fill(padding, lineY, totalWidth - padding, lineY + 1, lineColor);

            if (editMode) {
                String editIcon = "✥";
                int iconWidth = mc.textRenderer.getWidth(editIcon);
                int iconX = totalWidth - iconWidth - padding;
                int iconY = totalHeight - lineHeight - padding + 2;
                context.drawTextWithShadow(mc.textRenderer, editIcon, iconX, iconY, borderColor);
            }
        }

        // ARGB
        context.drawTextWithShadow(mc.textRenderer, coordText, padding, padding, 0xFFFFFFFF);
        context.drawTextWithShadow(mc.textRenderer, otherText, padding, padding + lineHeight + 4, dimensionColor);

        matrices.popMatrix();
    }

    private void handleDragging() {
        if (!editMode || mc.getWindow() == null) {
            isDragging = false;
            return;
        }

        double mouseX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / (double) mc.getWindow().getWidth();
        double mouseY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / (double) mc.getWindow().getHeight();

        float hudX = ConfigScreen.getHudX();
        float hudY = ConfigScreen.getHudY();
        float scale = ConfigScreen.getTextScale();

        if (mc.player != null) {
            String dimensionKey = mc.player.getWorld().getRegistryKey().getValue().toString();
            boolean isInNether = dimensionKey.equals("minecraft:the_nether");
            boolean isInEnd = dimensionKey.equals("minecraft:the_end");

            Text dimensionLabel;
            if (isInNether) {
                dimensionLabel = Text.translatable("text.coordinatesplus.dimension.overworld");
            } else if (isInEnd) {
                dimensionLabel = Text.translatable("text.coordinatesplus.dimension.end");
            } else {
                dimensionLabel = Text.translatable("text.coordinatesplus.dimension.nether");
            }

            int posX = (int) Math.round(mc.player.getX());
            int posY = (int) Math.round(mc.player.getY());
            int posZ = (int) Math.round(mc.player.getZ());
            int otherX = isInNether ? posX * 8 : posX / 8;
            int otherZ = isInNether ? posZ * 8 : posZ / 8;

            String coordText = String.format("X: %d  Y: %d  Z: %d", posX, posY, posZ);
            String otherText = String.format("%s: [%d, %d]", dimensionLabel.getString(), otherX, otherZ);

            int coordWidth = mc.textRenderer.getWidth(coordText);
            int otherWidth = mc.textRenderer.getWidth(otherText);
            int maxWidth = Math.max(coordWidth, otherWidth);

            int padding = 6;
            int hudWidth = (int) ((maxWidth + padding * 2) * scale);
            int lineHeight = mc.textRenderer.fontHeight;
            int hudHeight = (int) ((lineHeight * 2 + padding * 2 + 2) * scale);

            boolean isMouseOver = mouseX >= hudX && mouseX <= hudX + hudWidth &&
                    mouseY >= hudY && mouseY <= hudY + hudHeight;

            if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
                if (!isDragging && isMouseOver) {
                    isDragging = true;
                    dragOffsetX = mouseX - hudX;
                    dragOffsetY = mouseY - hudY;
                }

                if (isDragging) {
                    float newX = (float) (mouseX - dragOffsetX);
                    float newY = (float) (mouseY - dragOffsetY);

                    newX = Math.max(0, Math.min(newX, mc.getWindow().getScaledWidth() - hudWidth));
                    newY = Math.max(0, Math.min(newY, mc.getWindow().getScaledHeight() - hudHeight));

                    ConfigScreen.setHudPosition(newX, newY);
                }
            } else {
                isDragging = false;
            }
        }
    }

    private void drawBorder(DrawContext context, int x, int y, int width, int height, int thickness, int color) {
        context.fill(x, y, x + width, y + thickness, color);
        context.fill(x, y + height - thickness, x + width, y + height, color);
        context.fill(x, y, x + thickness, y + height, color);
        context.fill(x + width - thickness, y, x + width, y + height, color);
    }
}
