package com.thereallemon.llmconversations.client;

/**
 * Tracks conversation state on the client side
 */
public class ClientConversationState {
    private static Integer currentConversationCitizenId = null;

    /**
     * Set the current conversation partner
     * @param citizenId The citizen ID, or null if not in conversation
     */
    public static void setConversationPartner(Integer citizenId) {
        currentConversationCitizenId = citizenId;
    }

    /**
     * Get the current conversation partner
     * @return The citizen ID, or null if not in conversation
     */
    public static Integer getConversationPartner() {
        return currentConversationCitizenId;
    }

    /**
     * Check if the player is currently in a conversation
     * @return true if in conversation
     */
    public static boolean isInConversation() {
        return currentConversationCitizenId != null;
    }

    /**
     * Clear the conversation state
     */
    public static void clearConversation() {
        currentConversationCitizenId = null;
    }
}

