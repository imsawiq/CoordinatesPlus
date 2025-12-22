package org.sawiq.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.fabricmc.loader.api.FabricLoader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ConfigScreen {
    private static final File CONFIG_FILE = new File(
            FabricLoader.getInstance().getConfigDir().toFile(),
            "coordinatesplus.json"
    );
    private static ConfigData configData = new ConfigData();

    static {
        loadConfig();
    }

    public static Screen createConfigScreen() {
        ConfigBuilder builder = ConfigBuilder.create()
                .setTitle(Text.translatable("text.coordinatesplus.config.title"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        builder.getOrCreateCategory(Text.translatable("text.coordinatesplus.config.category.display"))
                .addEntry(entryBuilder.startBooleanToggle(
                                Text.translatable("text.coordinatesplus.config.hud_enabled"),
                                configData.isHudEnabled)
                        .setDefaultValue(true)
                        .setTooltip(Text.translatable("text.coordinatesplus.config.hud_enabled.tooltip"))
                        .setSaveConsumer(newValue -> configData.isHudEnabled = newValue)
                        .build())
                .addEntry(entryBuilder.startBooleanToggle(
                                Text.translatable("text.coordinatesplus.config.background_enabled"),
                                configData.isBackgroundEnabled)
                        .setDefaultValue(true)
                        .setTooltip(Text.translatable("text.coordinatesplus.config.background_enabled.tooltip"))
                        .setSaveConsumer(newValue -> configData.isBackgroundEnabled = newValue)
                        .build())
                .addEntry(entryBuilder.startBooleanToggle(
                                Text.translatable("text.coordinatesplus.config.group_background_enabled"),
                                configData.isGroupBackgroundEnabled) //!!!
                        .setDefaultValue(true)
                        .setTooltip(Text.translatable("text.coordinatesplus.config.group_background_enabled.tooltip"))
                        .setSaveConsumer(newValue -> configData.isGroupBackgroundEnabled = newValue)
                        .build())
                .addEntry(entryBuilder.startFloatField(
                                Text.translatable("text.coordinatesplus.config.text_scale"),
                                configData.textScale)
                        .setDefaultValue(1.0F)
                        .setMin(0.5F)
                        .setMax(3.0F)
                        .setTooltip(Text.translatable("text.coordinatesplus.config.text_scale.tooltip"))
                        .setSaveConsumer(newValue -> configData.textScale = newValue)
                        .build());

        builder.getOrCreateCategory(Text.translatable("text.coordinatesplus.config.category.keybinds"))
                .addEntry(entryBuilder.startBooleanToggle(
                                Text.translatable("text.coordinatesplus.config.command_prefix"),
                                configData.useCommandPrefix)
                        .setDefaultValue(false)
                        .setTooltip(Text.translatable("text.coordinatesplus.config.command_prefix.tooltip"))
                        .setSaveConsumer(newValue -> configData.useCommandPrefix = newValue)
                        .build());

        builder.getOrCreateCategory(Text.translatable("text.coordinatesplus.config.category.position"))
                .addEntry(entryBuilder.startFloatField(
                                Text.translatable("text.coordinatesplus.config.hud_x"),
                                configData.hudX)
                        .setDefaultValue(10.0F)
                        .setTooltip(Text.translatable("text.coordinatesplus.config.hud_x.tooltip"))
                        .setSaveConsumer(newValue -> configData.hudX = newValue)
                        .build())
                .addEntry(entryBuilder.startFloatField(
                                Text.translatable("text.coordinatesplus.config.hud_y"),
                                configData.hudY)
                        .setDefaultValue(10.0F)
                        .setTooltip(Text.translatable("text.coordinatesplus.config.hud_y.tooltip"))
                        .setSaveConsumer(newValue -> configData.hudY = newValue)
                        .build());

        builder.getOrCreateCategory(Text.translatable("text.coordinatesplus.config.category.position"))
                .addEntry(entryBuilder.startFloatField(
                                Text.translatable("text.coordinatesplus.config.group_hud_x"),
                                configData.groupHudX)
                        .setDefaultValue(10.0F)
                        .setTooltip(Text.translatable("text.coordinatesplus.config.group_hud_x.tooltip"))
                        .setSaveConsumer(newValue -> configData.groupHudX = newValue)
                        .build())
                .addEntry(entryBuilder.startFloatField(
                                Text.translatable("text.coordinatesplus.config.group_hud_y"),
                                configData.groupHudY)
                        .setDefaultValue(60.0F)
                        .setTooltip(Text.translatable("text.coordinatesplus.config.group_hud_y.tooltip"))
                        .setSaveConsumer(newValue -> configData.groupHudY = newValue)
                        .build());

        builder.setSavingRunnable(() -> {
            saveConfig();
        });

        return builder.build();
    }

    public static boolean isHudEnabled() {
        return configData.isHudEnabled;
    }

    public static boolean isBackgroundEnabled() {
        return configData.isBackgroundEnabled;
    }

    public static boolean isGroupBackgroundEnabled() {
        return configData.isGroupBackgroundEnabled;
    }

    public static boolean useCommandPrefix() {
        return configData.useCommandPrefix;
    }

    public static float getTextScale() {
        return configData.textScale;
    }

    public static float getHudX() {
        return configData.hudX;
    }

    public static float getHudY() {
        return configData.hudY;
    }

    public static float getGroupHudX() {
        return configData.groupHudX;
    }

    public static float getGroupHudY() {
        return configData.groupHudY;
    }

    public static void setHudPosition(float x, float y) {
        configData.hudX = x;
        configData.hudY = y;
    }

    public static void setGroupHudPosition(float x, float y) {
        configData.groupHudX = x;
        configData.groupHudY = y;
    }

    public static void savePosition() {
        saveConfig();
    }

    private static void loadConfig() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                Gson gson = new Gson();
                ConfigData loaded = gson.fromJson(reader, ConfigData.class);
                if (loaded != null) {
                    configData = loaded;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (configData == null) {
            configData = new ConfigData();
            saveConfig();
        }
    }

    private static void saveConfig() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(configData, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ConfigData {
        private boolean isHudEnabled = true;
        private boolean isBackgroundEnabled = true;
        private boolean isGroupBackgroundEnabled = true;
        private boolean useCommandPrefix = false;
        private float textScale = 1.0F;
        private float hudX = 10.0F;
        private float hudY = 10.0F;
        private float groupHudX = 10.0F;
        private float groupHudY = 60.0F;
    }
}
