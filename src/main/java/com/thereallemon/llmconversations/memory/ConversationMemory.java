package com.thereallemon.llmconversations.memory;

import com.minecolonies.api.colony.ICitizenData;
import com.thereallemon.llmconversations.config.LLMConfig;
import com.thereallemon.llmconversations.util.DebugLogger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores conversation history and summaries for citizens
 */
public class ConversationMemory {
    private static final String NBT_KEY = "llmconversations_memory";
    private static final String NBT_SUMMARIES = "summaries";
    
    // Runtime memory cache: maps citizen ID to their conversation memory
    // This persists during a game session but is lost on server restart
    private static final Map<Integer, ConversationMemory> MEMORY_CACHE = new HashMap<>();

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
     * This retrieves memory from the runtime cache
     * @param citizen The citizen to get memory for
     * @return ConversationMemory instance
     */
    public static ConversationMemory get(ICitizenData citizen) {
        int citizenId = citizen.getId();

        // Get from cache or create new
        ConversationMemory memory = MEMORY_CACHE.computeIfAbsent(citizenId, k -> {
            DebugLogger.debug("Creating new memory for citizen {} ({})", citizen.getName(), citizenId);
            return new ConversationMemory();
        });

        DebugLogger.debug("Retrieved memory for citizen {} ({}): {} summaries",
            citizen.getName(), citizenId, memory.getSummaries().size());

        return memory;
    }
    
    /**
     * Save memory to citizen's data
     * Currently stores in runtime cache (persists during game session)
     * @param citizen The citizen to save memory for
     * @param memory The memory to save
     */
    public static void save(ICitizenData citizen, ConversationMemory memory) {
        int citizenId = citizen.getId();
        MEMORY_CACHE.put(citizenId, memory);

        DebugLogger.debug("Saved memory for citizen {} ({}): {} summaries",
            citizen.getName(), citizenId, memory.getSummaries().size());

        // TODO: Implement proper NBT persistence to survive server restarts
        // This would require hooking into Minecolonies' data storage system
        // For now, memories persist during a game session but are lost on restart
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
