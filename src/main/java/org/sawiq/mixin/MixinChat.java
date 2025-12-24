package org.sawiq.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.sawiq.CoordinatesPlus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinChat {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        if (CoordinatesPlus.getGroupManager().handleOutgoingCommand(message)) {
            ci.cancel();
        }
    }
    @ModifyVariable(method = "sendChatCommand", at = @At("HEAD"), argsOnly = true)
    private String modifyOutgoingCommand(String command) {
        var player = MinecraftClient.getInstance().player;
        if (player != null && command != null && (command.contains("+locn") || command.contains("+loco"))) {
            String modified = processChatMessage(player, command);
            return modified != null ? modified : command;
        }
        return command;
    }
    @ModifyVariable(method = "sendChatMessage", at = @At("HEAD"), argsOnly = true)
    private String modifyOutgoingMessage(String message) {
        if (MinecraftClient.getInstance().player != null && message != null && (message.contains("+locn") || message.contains("+loco"))) {
            String modifiedMessage = processChatMessage(MinecraftClient.getInstance().player, message);
            if (modifiedMessage != null) {
                return modifiedMessage;
            }
        }
        return message;
    }

    private String processChatMessage(net.minecraft.client.network.ClientPlayerEntity player, String message) {
        if (player == null) return null;

        double rawX = player.getX();
        double rawZ = player.getZ();
        boolean isInNether = player.getWorld().getRegistryKey().getValue().toString().equals("minecraft:the_nether");

        int netherX, netherZ, overworldX, overworldZ;
        if (isInNether) {
            netherX = (int) Math.round(rawX);
            netherZ = (int) Math.round(rawZ);
            overworldX = (int) Math.round(rawX * 8.0);
            overworldZ = (int) Math.round(rawZ * 8.0);
        } else {
            overworldX = (int) Math.round(rawX);
            overworldZ = (int) Math.round(rawZ);
            netherX = (int) Math.round(rawX * 0.125);
            netherZ = (int) Math.round(rawZ * 0.125);
        } // wft

        if (message.contains("+locn")) {
            return message.replace("+locn", String.format("[%d, %d]", netherX, netherZ));
        } else if (message.contains("+loco")) {
            return message.replace("+loco", String.format("[%d, %d]", overworldX, overworldZ));
        }
        return null;
    }
}