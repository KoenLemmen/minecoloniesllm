# Memory Persistence Fix

## Problem
Memories were working during gameplay but were lost when closing and reopening the game or save. The system was only using a runtime HashMap cache that didn't persist to disk.

## Solution
Implemented a proper NBT-based persistence system using Minecraft's `SavedData` API. This ensures conversation memories survive across game sessions.

## Changes Made

### 1. Created `MemorySavedData.java`
A new class that extends `SavedData` to manage persistent storage of all citizen memories:
- Stores a map of citizen IDs to their conversation memories
- Automatically serializes to/from NBT
- Saves to world data directory (survives game restarts)
- Uses Minecraft's built-in save/load system

**Location:** `src/main/java/com/thereallemon/llmconversations/memory/MemorySavedData.java`

### 2. Updated `ConversationMemory.java`
Modified the `get()` and `save()` methods to use `MemorySavedData`:

**Before:**
- Only stored memories in a HashMap (`MEMORY_CACHE`)
- Lost all data when game closed
- Had NBT serialization methods but never used them

**After:**
- Still uses HashMap for fast runtime access
- On first load, checks SavedData for persisted memories
- On save, writes to both cache AND SavedData
- Automatically persists to disk

### 3. Added `DebugLogger.error()` overload
Added a method to log errors with formatted arguments to match the debug method pattern.

**Location:** `src/main/java/com/thereallemon/llmconversations/util/DebugLogger.java`

## How It Works

### Memory Flow:

1. **First conversation with an NPC:**
   - System checks cache (empty)
   - Loads from SavedData (empty, first time)
   - Creates new ConversationMemory
   - Conversation happens, summary generated
   - Save() writes to both cache and SavedData
   - Data written to disk automatically

2. **Close and reopen game:**
   - Cache is cleared (it's in RAM)
   - Next conversation with same NPC
   - System checks cache (empty)
   - Loads from SavedData (SUCCESS! Data restored)
   - NPC remembers previous conversations

3. **Subsequent conversations (same session):**
   - Fast cache lookup (no disk I/O)
   - Updates saved on conversation end

## File Location
Memories are stored in:
```
<world_folder>/data/llmconversations_memories.dat
```

This is a standard Minecraft NBT file that gets saved automatically when the world saves.

## Testing Instructions

### Test 1: Basic Persistence
1. Start the game and load your world
2. Have a conversation with a citizen (e.g., "I need help building a house")
3. Say goodbye to trigger memory save
4. Check logs for: `"Saved memory to SavedData for citizen..."`
5. **Save and exit to main menu**
6. **Reload the same world**
7. Talk to the SAME citizen again
8. Check logs for: `"Loaded memory from SavedData for citizen... X summaries"`
9. The citizen should reference your previous conversation

### Test 2: Multiple Citizens
1. Have conversations with 3 different citizens
2. Save and reload
3. Talk to each one - all should remember their specific conversations

### Test 3: Complete Game Restart
1. Have a conversation and save
2. **Completely close Minecraft**
3. **Restart Minecraft**
4. Load world and talk to the same citizen
5. Memory should still be there

### Test 4: Long-term Memory
1. Have multiple conversations over several sessions
2. Memories accumulate up to the configured limit
3. Check that old memories are preserved across restarts

## Debug Logging

Enable debug logging in the config to see detailed memory operations:

Look for these log messages:
- `"Creating new memory entry for citizen ID X"`
- `"Saving X citizen memories to NBT"`
- `"Loading X citizen memories from NBT"`
- `"Loaded memory for citizen ID X: Y summaries"`
- `"Retrieved cached memory for citizen..."`
- `"Loaded memory from SavedData for citizen..."`
- `"Saved memory to SavedData for citizen..."`

## Potential Issues & Solutions

### Issue: "Server not available" message
**Cause:** Trying to access memories on client side
**Solution:** Memories are server-side only (this is correct). Client sees citizen responses but server manages memory.

### Issue: Memories still lost
**Possible causes:**
1. World not saving properly (check world save settings)
2. Citizen ID changing (shouldn't happen in Minecolonies)
3. Exception during save (check logs)
4. World data corrupted

**Debug steps:**
1. Check for `llmconversations_memories.dat` in world data folder
2. Look for exceptions in logs
3. Verify citizen ID stays the same across sessions

### Issue: Performance concerns
**Solution:** System uses two-tier caching:
- Fast in-memory cache for active conversations
- Disk-based SavedData for persistence
- Only disk I/O on first load per citizen per session
- World saves happen automatically (no extra overhead)

## Technical Details

### SavedData System
- Part of vanilla Minecraft's world save system
- Automatically handles file I/O
- Thread-safe
- Integrated with world save events
- NBT format (compact and efficient)

### Citizen ID Persistence
- Minecolonies assigns permanent IDs to citizens
- IDs persist across sessions (not entity IDs)
- Safe to use as permanent keys

### Memory Structure in NBT
```
llmconversations_memories.dat
├─ memories (ListTag)
   ├─ citizenId: 1 (int)
   │  └─ memory (CompoundTag)
   │     └─ summaries (ListTag of StringTag)
   │        ├─ "Player requested house near town hall"
   │        └─ "Discussed building materials needed"
   └─ citizenId: 2 (int)
      └─ memory (CompoundTag)
         └─ summaries (ListTag of StringTag)
            └─ "Player asked about job satisfaction"
```

## Configuration
The max number of memories per citizen is controlled by:
```toml
[config/llm-conversations-client.toml]
maxConversationHistoryLength = 5
```

This affects both runtime and persisted memory.

## Future Enhancements

Possible improvements:
1. Memory trimming based on age/relevance
2. Colony-wide shared memories (events everyone knows about)
3. Memory export/import for debugging
4. Admin commands to view/edit memories
5. Memory forgetting over time (simulate real forgetting)

## Summary

✅ **Fixed:** Memories now persist across game restarts
✅ **Method:** Using Minecraft's SavedData system
✅ **Performance:** Minimal overhead with smart caching
✅ **Reliability:** Automatic saves with world saves
✅ **Testing:** Ready to test immediately

