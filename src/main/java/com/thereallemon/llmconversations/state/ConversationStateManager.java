package com.thereallemon.llmconversations.state;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.thereallemon.llmconversations.config.LLMConfig;
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

    // citizenId -> AbstractEntityCitizen mapping (for movement/look control)
    private static final Map<Integer, AbstractEntityCitizen> conversationEntities = new HashMap<>();

    // citizenId -> player UUID for look tracking
    private static final Map<Integer, UUID> lookAtPlayers = new HashMap<>();

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

            // Stop movement and make citizen look at player
            stopMovementAndLookAtPlayer(citizen, player);
        }
    }

    /**
     * End a conversation
     * @param citizenId The citizen's ID
     */
    public static void endConversation(int citizenId) {
        // Resume normal movement
        resumeMovement(citizenId);

        activeConversations.remove(citizenId);
        handlers.remove(citizenId);
        frozenSaturation.remove(citizenId);
        conversationEntities.remove(citizenId);
        lookAtPlayers.remove(citizenId);
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

            // Maintain look at player
            maintainLookAtPlayer(citizen);
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
        conversationEntities.clear();
        lookAtPlayers.clear();
    }

    /**
     * Stop citizen movement and make them look at the player
     * @param citizen The citizen entity
     * @param player The player they're talking to
     */
    private static void stopMovementAndLookAtPlayer(AbstractEntityCitizen citizen, ServerPlayer player) {
        // Stop navigation/pathfinding
        citizen.getNavigation().stop();

        // Store references for maintenance
        conversationEntities.put(citizen.getCivilianID(), citizen);
        lookAtPlayers.put(citizen.getCivilianID(), player.getUUID());

        // Make citizen look at player
        if (citizen.getLookControl() != null) {
            citizen.getLookControl().setLookAt(player, 30.0F, 30.0F);
        }
    }

    /**
     * Maintain the citizen looking at the player during conversation
     * Called every tick to keep them facing the player
     * Also checks if player is too far away
     * @param citizenData The citizen data
     */
    private static void maintainLookAtPlayer(ICitizenData citizenData) {
        AbstractEntityCitizen citizen = conversationEntities.get(citizenData.getId());
        UUID playerUUID = lookAtPlayers.get(citizenData.getId());

        if (citizen != null && playerUUID != null && !citizen.isRemoved()) {
            // Find the player
            ServerPlayer player = citizen.getServer() != null ?
                citizen.getServer().getPlayerList().getPlayer(playerUUID) : null;

            if (player != null) {
                // Check distance - if too far, end conversation
                double maxDistance = LLMConfig.CLIENT.maxConversationDistance.get();
                if (maxDistance > 0) { // Only check if distance checking is enabled
                    double distance = citizen.distanceTo(player);
                    if (distance > maxDistance) {
                        // End conversation due to distance
                        endConversationDueToDistance(citizenData, player);
                        return;
                    }
                }

                // Stop any movement that might have started
                if (citizen.getNavigation().isInProgress()) {
                    citizen.getNavigation().stop();
                }

                // Continuously update look direction
                if (citizen.getLookControl() != null) {
                    citizen.getLookControl().setLookAt(player, 30.0F, 30.0F);
                }
            } else {
                // Player not found (logged out, changed dimension, etc.)
                endConversationDueToPlayerGone(citizenData);
            }
        }
    }

    /**
     * End conversation because player walked too far away
     * @param citizenData The citizen data
     * @param player The player
     */
    private static void endConversationDueToDistance(ICitizenData citizenData, ServerPlayer player) {
        // Get handler to properly end conversation
        LLMConversationHandler handler = handlers.get(citizenData.getId());

        // Notify player
        player.sendSystemMessage(
            net.minecraft.network.chat.Component.translatable("llmconversations.message.too_far",
                citizenData.getName())
        );

        // End conversation properly through handler if available
        if (handler != null) {
            handler.endConversation(player);
        } else {
            // Fallback cleanup
            unfreezeStats(citizenData);
            endConversation(citizenData.getId());

            if (citizenData.getJob() != null) {
                citizenData.getJob().onWakeUp();
                citizenData.getJob().resetAI();
            }

            // Sync to client
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player,
                new com.thereallemon.llmconversations.network.SyncConversationStatePacket(
                    citizenData.getId(), false)
            );
        }
    }

    /**
     * End conversation because player is gone (logged out, dimension change, etc.)
     * @param citizenData The citizen data
     */
    private static void endConversationDueToPlayerGone(ICitizenData citizenData) {
        // Clean up without trying to message the player
        LLMConversationHandler handler = handlers.get(citizenData.getId());

        if (handler != null) {
            // Can't call endConversation(player) since player is gone
            // Do manual cleanup
            unfreezeStats(citizenData);
            endConversation(citizenData.getId());

            if (citizenData.getJob() != null) {
                citizenData.getJob().onWakeUp();
                citizenData.getJob().resetAI();
            }
        } else {
            // Handler already gone, just clean up state
            unfreezeStats(citizenData);
            endConversation(citizenData.getId());
        }
    }

    /**
     * Resume normal movement after conversation ends
     * @param citizenId The citizen's ID
     */
    private static void resumeMovement(int citizenId) {
        AbstractEntityCitizen citizen = conversationEntities.get(citizenId);
        if (citizen != null && !citizen.isRemoved()) {
            // Navigation will resume naturally when the AI state machine continues
            // The look control will also return to normal behavior
            // No explicit action needed - just removing from our tracking maps is sufficient
        }
    }
}
