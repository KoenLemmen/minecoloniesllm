package com.thereallemon.llmconversations.memory;

import com.thereallemon.llmconversations.util.DebugLogger;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;

/**
 * SavedData for persisting conversation memories across game restarts
 */
public class MemorySavedData extends SavedData {
    private static final String DATA_NAME = "llmconversations_memories";

    // Maps citizen ID to their memory data
    private final Map<Integer, ConversationMemory> memories = new HashMap<>();

    public MemorySavedData() {
        super();
    }

    /**
     * Load memory for a specific citizen
     */
    public ConversationMemory getMemory(int citizenId) {
        return memories.computeIfAbsent(citizenId, k -> {
            DebugLogger.debug("Creating new memory entry for citizen ID {}", k);
            return new ConversationMemory();
        });
    }

    /**
     * Save memory for a specific citizen
     */
    public void saveMemory(int citizenId, ConversationMemory memory) {
        memories.put(citizenId, memory);
        setDirty(); // Mark as needing to be saved
        DebugLogger.debug("Marked memory data as dirty for citizen ID {}", citizenId);
    }

    /**
     * Serialize all memories to NBT
     */
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        DebugLogger.debug("Saving {} citizen memories to NBT", memories.size());

        ListTag memoriesList = new ListTag();
        for (Map.Entry<Integer, ConversationMemory> entry : memories.entrySet()) {
            CompoundTag citizenEntry = new CompoundTag();
            citizenEntry.putInt("citizenId", entry.getKey());
            citizenEntry.put("memory", entry.getValue().serializeNBT(provider));
            memoriesList.add(citizenEntry);
        }

        tag.put("memories", memoriesList);
        return tag;
    }

    /**
     * Deserialize all memories from NBT
     */
    public static MemorySavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        MemorySavedData data = new MemorySavedData();

        if (tag.contains("memories")) {
            ListTag memoriesList = tag.getList("memories", Tag.TAG_COMPOUND);
            DebugLogger.debug("Loading {} citizen memories from NBT", memoriesList.size());

            for (int i = 0; i < memoriesList.size(); i++) {
                CompoundTag citizenEntry = memoriesList.getCompound(i);
                int citizenId = citizenEntry.getInt("citizenId");

                ConversationMemory memory = new ConversationMemory();
                memory.deserializeNBT(provider, citizenEntry.getCompound("memory"));

                data.memories.put(citizenId, memory);
                DebugLogger.debug("Loaded memory for citizen ID {}: {} summaries",
                    citizenId, memory.getSummaries().size());
            }
        }

        return data;
    }

    /**
     * Get or create the SavedData instance for a server
     */
    public static MemorySavedData get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
            new SavedData.Factory<>(
                MemorySavedData::new,
                MemorySavedData::load
            ),
            DATA_NAME
        );
    }
}

