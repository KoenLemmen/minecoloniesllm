package com.thereallemon.llmconversations.memory;

import com.minecolonies.api.colony.ICitizenData;
import com.thereallemon.llmconversations.config.LLMConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores conversation history and summaries for citizens
 */
public class ConversationMemory {
    private static final String NBT_KEY = "llmconversations_memory";
    private static final String NBT_SUMMARIES = "summaries";
    
    private final List<String> conversationSummaries;
    
    public ConversationMemory() {
        this.conversationSummaries = new ArrayList<>();
    }
    
    /**
     * Add a conversation summary to memory
     * @param summary Summary of the conversation
     */
    public void addSummary(String summary) {
        conversationSummaries.add(0, summary); // Add to front (most recent first)
        
        // Keep only recent summaries based on config
        int maxSummaries = LLMConfig.CLIENT.maxConversationHistoryLength.get();
        while (conversationSummaries.size() > maxSummaries) {
            conversationSummaries.remove(conversationSummaries.size() - 1);
        }
    }
    
    /**
     * Get all conversation summaries
     * @return List of summaries (most recent first)
     */
    public List<String> getSummaries() {
        return new ArrayList<>(conversationSummaries);
    }
    
    /**
     * Clear all conversation memory
     */
    public void clearMemory() {
        conversationSummaries.clear();
    }
    
    /**
     * Serialize memory to NBT
     * @param provider The registry lookup provider (unused for simple string data)
     * @return NBT tag containing memory data
     */
    public CompoundTag serializeNBT(net.minecraft.core.HolderLookup.Provider provider) {
        CompoundTag nbt = new CompoundTag();
        ListTag summariesList = new ListTag();
        
        for (String summary : conversationSummaries) {
            summariesList.add(StringTag.valueOf(summary));
        }
        
        nbt.put(NBT_SUMMARIES, summariesList);
        return nbt;
    }
    
    /**
     * Deserialize memory from NBT
     * @param provider The registry lookup provider (unused for simple string data)
     * @param nbt NBT tag containing memory data
     */
    public void deserializeNBT(net.minecraft.core.HolderLookup.Provider provider, CompoundTag nbt) {
        conversationSummaries.clear();
        
        if (nbt.contains(NBT_SUMMARIES)) {
            ListTag summariesList = nbt.getList(NBT_SUMMARIES, Tag.TAG_STRING);
            for (int i = 0; i < summariesList.size(); i++) {
                conversationSummaries.add(summariesList.getString(i));
            }
        }
    }
    
    /**
     * Get or create memory for a citizen
     * This retrieves memory from the citizen's additional data
     * @param citizen The citizen to get memory for
     * @return ConversationMemory instance
     */
    public static ConversationMemory get(ICitizenData citizen) {
        ConversationMemory memory = new ConversationMemory();
        
        // TODO: Implement proper NBT persistence with Minecolonies API
        // For now, memory is runtime-only and will be lost on server restart
        // This can be improved by hooking into Minecolonies' data storage system
        
        return memory;
    }
    
    /**
     * Save memory to citizen's data
     * @param citizen The citizen to save memory for
     * @param memory The memory to save
     */
    public static void save(ICitizenData citizen, ConversationMemory memory) {
        // TODO: Implement proper NBT persistence with Minecolonies API
        // For now, memory is runtime-only and will be lost on server restart
        //
        // The actual implementation would need to:
        // 1. Access Minecolonies' citizen data storage system
        // 2. Call serializeNBT with proper HolderLookup.Provider parameter
        // 3. Call markDirty with the appropriate int parameter (dirty flag)
        //
        // Example (once proper API is understood):
        // HolderLookup.Provider provider = citizen.getColony().getWorld().registryAccess();
        // CompoundTag citizenData = citizen.serializeNBT(provider);
        // citizenData.put(NBT_KEY, memory.serializeNBT());
        // citizen.markDirty(0); // or appropriate flag value
    }
    
    /**
     * Get a summary of recent conversations for context
     * @return A formatted string of recent conversation topics
     */
    public String getRecentContext() {
        if (conversationSummaries.isEmpty()) {
            return "This is our first conversation.";
        }
        
        StringBuilder context = new StringBuilder("Recent conversations: ");
        int count = Math.min(3, conversationSummaries.size());
        for (int i = 0; i < count; i++) {
            if (i > 0) context.append("; ");
            context.append(conversationSummaries.get(i));
        }
        
        return context.toString();
    }
}
