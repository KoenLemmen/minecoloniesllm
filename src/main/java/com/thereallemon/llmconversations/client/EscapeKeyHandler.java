package com.thereallemon.llmconversations.client;

import com.thereallemon.llmconversations.network.EndConversationPacket;
import com.thereallemon.llmconversations.util.DebugLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "llmconversations", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class EscapeKeyHandler {

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        // Check if the pause screen is being opened
        if (!(event.getNewScreen() instanceof PauseScreen)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null) {
            return;
        }

        // Check if player is in a conversation (client-side state)
        if (ClientConversationState.isInConversation()) {
            Integer citizenId = ClientConversationState.getConversationPartner();

            // Player is in conversation - cancel the conversation and prevent pause screen from opening
            DebugLogger.log("ESC pressed: Canceling conversation with citizen " + citizenId);

            // Clear client-side conversation state
            ClientConversationState.clearConversation();

            // Send packet to server to end conversation
            PacketDistributor.sendToServer(new EndConversationPacket());

            // Cancel the screen opening event to prevent pause menu
            event.setCanceled(true);
        }
    }
}

