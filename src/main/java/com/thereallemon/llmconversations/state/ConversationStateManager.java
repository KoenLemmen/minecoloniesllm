package com.thereallemon.llmconversations.state;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.thereallemon.llmconversations.interaction.LLMConversationHandler;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the state of active conversations and frozen citizen stats
 */
public class ConversationStateManager {
    
    // citizenId -> playerUUID mapping
    private static final Map<Integer, UUID> activeConversations = new HashMap<>();

    // citizenId -> handler mapping
    private static final Map<Integer, LLMConversationHandler> handlers = new HashMap<>();

    // citizenId -> frozen saturation value
    private static final Map<Integer, Double> frozenSaturation = new HashMap<>();

    /**
     * Start a conversation between a citizen and player
     * @param citizenId The citizen's ID
     * @param playerUUID The player's UUID
     */
    public static void startConversation(int citizenId, UUID playerUUID) {
        activeConversations.put(citizenId, playerUUID);
    }

    /**
     * Start a conversation between a citizen entity and player (convenience method)
     * @param player The player
     * @param citizen The citizen entity
     */
    public static void startConversation(ServerPlayer player, AbstractEntityCitizen citizen) {
        ICitizenData citizenData = citizen.getCitizenData();
        if (citizenData != null) {
            startConversation(citizenData.getId(), player.getUUID());

            // Create and register LLM conversation handler
            LLMConversationHandler handler = new LLMConversationHandler(citizenData);
            registerHandler(citizenData.getId(), handler);

            // Trigger the interaction in Minecolonies system
            citizenData.triggerInteraction(handler);

            // Freeze citizen stats
            freezeStats(citizenData);
        }
    }

    /**
     * End a conversation
     * @param citizenId The citizen's ID
     */
    public static void endConversation(int citizenId) {
        activeConversations.remove(citizenId);
        handlers.remove(citizenId);
        frozenSaturation.remove(citizenId);
    }
    
    /**
     * Check if a citizen is currently in a conversation
     * @param citizenId The citizen's ID
     * @return True if in conversation
     */
    public static boolean isInConversation(int citizenId) {
        return activeConversations.containsKey(citizenId);
    }
    
    /**
     * Check if a player is currently in a conversation
     * @param player The player
     * @return True if in conversation
     */
    public static boolean isInConversation(ServerPlayer player) {
        return activeConversations.containsValue(player.getUUID());
    }

    /**
     * Get the citizen ID that a player is conversing with
     * @param playerUUID The player's UUID
     * @return The citizen's ID, or null if not in conversation
     */
    public static Integer getConversationPartner(UUID playerUUID) {
        return activeConversations.entrySet().stream()
            .filter(e -> e.getValue().equals(playerUUID))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Register a conversation handler for a citizen
     * @param citizenId The citizen's ID
     * @param handler The conversation handler
     */
    public static void registerHandler(int citizenId, LLMConversationHandler handler) {
        handlers.put(citizenId, handler);
    }
    
    /**
     * Get the conversation handler for a citizen
     * @param citizenId The citizen's ID
     * @return The handler, or null if not found
     */
    public static LLMConversationHandler getHandler(int citizenId) {
        return handlers.get(citizenId);
    }
    
    /**
     * Freeze a citizen's stats during conversation
     * @param citizen The citizen data
     */
    public static void freezeStats(ICitizenData citizen) {
        frozenSaturation.put(citizen.getId(), citizen.getSaturation());
    }
    
    /**
     * Unfreeze a citizen's stats after conversation
     * @param citizen The citizen data
     */
    public static void unfreezeStats(ICitizenData citizen) {
        frozenSaturation.remove(citizen.getId());
    }
    
    /**
     * Maintain frozen stats for a citizen in conversation
     * Called on tick to prevent stats from decreasing
     * @param citizen The citizen data
     */
    public static void maintainFrozenStats(ICitizenData citizen) {
        if (isInConversation(citizen.getId())) {
            Double frozenValue = frozenSaturation.get(citizen.getId());
            if (frozenValue != null) {
                // Restore the frozen saturation value
                citizen.setSaturation(frozenValue);
            }
        }
    }
    
    /**
     * Get all active conversations
     * @return Map of citizen IDs to player UUIDs
     */
    public static Map<Integer, UUID> getActiveConversations() {
        return new HashMap<>(activeConversations);
    }
    
    /**
     * Clear all conversation state (useful for cleanup)
     */
    public static void clearAll() {
        activeConversations.clear();
        handlers.clear();
        frozenSaturation.clear();
    }
}
