package com.thereallemon.llmconversations.client;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.thereallemon.llmconversations.network.StartConversationPacket;
import com.thereallemon.llmconversations.util.DebugLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "llmconversations", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class KeyInputHandler {
    private static boolean wasStartKeyDown = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null || mc.level == null) {
            return;
        }

        // Handle START CONVERSATION key
        boolean isStartKeyDown = KeyBindings.START_CONVERSATION.isDown();
        if (isStartKeyDown && !wasStartKeyDown) {
            handleStartConversation(mc, player);
        }
        wasStartKeyDown = isStartKeyDown;
    }

    private static void handleStartConversation(Minecraft mc, LocalPlayer player) {
        HitResult hitResult = mc.hitResult;

        if (player == null) {
            return;
        }

        if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hitResult;
            Entity target = entityHit.getEntity();

            if (target instanceof AbstractEntityCitizen citizen) {
                DebugLogger.log("Key pressed: Starting conversation with citizen " + citizen.getId());

                // Send packet to server to start conversation
                PacketDistributor.sendToServer(new StartConversationPacket(citizen.getId()));
            }
        }
    }
}

