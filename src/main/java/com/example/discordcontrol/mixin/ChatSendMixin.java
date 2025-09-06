package com.example.discordcontrol.mixin;

import com.example.discordcontrol.features.GagFilter;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ClientPlayerEntity.class)
public class ChatSendMixin {
    @ModifyArg(method = "sendChatMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/c2s/play/ChatMessageC2SPacket;<init>(Ljava/lang/String;)V"))
    private String discordcontrol$gag(String message) {
        return GagFilter.gagged ? GagFilter.gagify(message) : message;
    }
}
