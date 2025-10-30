package com.thereallemon.llmconversations.events;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColonyManager;
import com.thereallemon.llmconversations.config.LLMConfig;
import com.thereallemon.llmconversations.interaction.LLMConversationHandler;
import com.thereallemon.llmconversations.state.ConversationStateManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.UUID;
import java.util.Arrays;

/**
 * Intercepts chat messages to handle conversation responses
 */
@EventBusSubscriber(modid = "llmconversations")
public class ChatInterceptHandler {
    
    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        UUID playerUUID = player.getUUID();
        
        // Check if player is in a conversation
        Integer citizenId = ConversationStateManager.getConversationPartner(playerUUID);
        if (citizenId == null) {
            return; // Not in conversation, let chat proceed normally
        }
        
        // Cancel normal chat - this message is for the citizen
        event.setCanceled(true);
        
        // Get citizen data
        ICitizenData citizenData = findCitizen(player, citizenId);
        if (citizenData == null) {
            // Citizen no longer exists, end conversation
            ConversationStateManager.endConversation(citizenId);
            player.sendSystemMessage(Component.translatable(
                "llmconversations.message.citizen_left"
            ));
            return;
        }
        
        String message = event.getRawText();
        
        // Check for exit commands (configurable list)
        if (isExitWord(message)) {
            endConversation(player, citizenData);
            return;
        }
        
        // Forward to LLM handler
        LLMConversationHandler handler = ConversationStateManager.getHandler(citizenId);
        if (handler != null) {
            // Echo the player's message in chat with proper color formatting
            player.sendSystemMessage(
                Component.literal(player.getName().getString())
                    .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GREEN))
                    .append(Component.literal(": " + message).withColor(net.minecraft.ChatFormatting.WHITE.getColor()))
            );

            // Process the message
            handler.handlePlayerMessage(message, player);
        } else {
            // Handler not found, end conversation
            ConversationStateManager.endConversation(citizenId);
            player.sendSystemMessage(Component.translatable(
                "llmconversations.message.handler_lost"
            ));
        }
    }
    
    /**
     * Check if a message is an exit word (case-insensitive)
     */
    private static boolean isExitWord(String message) {
        String[] exitWords = LLMConfig.ClientConfig.getExitWords();
        String lowerMessage = message.toLowerCase().trim();
        return Arrays.asList(exitWords).contains(lowerMessage);
    }
    
    /**
     * Find a citizen by ID across all colonies
     */
    private static ICitizenData findCitizen(ServerPlayer player, int citizenId) {
        return IColonyManager.getInstance()
            .getAllColonies()
            .stream()
            .flatMap(colony -> colony.getCitizenManager().getCitizens().stream())
            .filter(citizen -> citizen.getId() == citizenId)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * End a conversation properly and ensure citizen resumes work
     */
    private static void endConversation(ServerPlayer player, ICitizenData citizenData) {
        LLMConversationHandler handler = ConversationStateManager.getHandler(citizenData.getId());
        if (handler != null) {
            // Use the handler's proper end method (includes LLM summarization)
            handler.endConversation(player);
        } else {
            // Fallback if handler is missing - still need to clean up properly
            ConversationStateManager.unfreezeStats(citizenData);
            ConversationStateManager.endConversation(citizenData.getId());
            
            // Resume work
            if (citizenData.getJob() != null) {
                // Wake up the job - this resets job-specific state
                citizenData.getJob().onWakeUp();
                
                // Reset the AI state machine to IDLE
                // The CitizenAI will automatically transition to WORK state on next tick
                citizenData.getJob().resetAI();
            }
            
            player.sendSystemMessage(Component.translatable(
                "llmconversations.message.conversation_ended", citizenData.getName()
            ));
        }

        // Notify client that conversation has ended
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
            player,
            new com.thereallemon.llmconversations.network.SyncConversationStatePacket(citizenData.getId(), false)
        );
    }
}
