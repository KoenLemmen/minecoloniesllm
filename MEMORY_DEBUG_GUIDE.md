# Memory System - Debug Guide

## What to Look For in Logs

### When Starting a Conversation (First Time with a Citizen)
```
[LLMConversations] Creating new memory for citizen Jaelyn O. Culpepper (1)
[LLMConversations] Retrieved memory for citizen Jaelyn O. Culpepper (1): 0 summaries
[LLMConversations] ========== Generated System Prompt ==========
[LLMConversations] Full Prompt: You are Jaelyn O. Culpepper...
```
**Expected:** No memories section in prompt (first conversation)

### When Ending a Conversation
```
[LLMConversations] ========== Ending Conversation ==========
[LLMConversations] Starting LLM summarization for conversation with Jaelyn O. Culpepper (2 messages)
[LLMConversations] === LLM RESPONSE RECEIVED ===
[LLMConversations] Summary: User disclosed being 28 years old...
[LLMConversations] Executing on server thread - saving summary
[LLMConversations] Saved memory for citizen Jaelyn O. Culpepper (1): 1 summaries
[LLMConversations] ✓ Summary successfully saved to memory!
```
**Expected:** "Saved memory" message with count of summaries

### When Starting a NEW Conversation (Second Time)
```
[LLMConversations] Retrieved memory for citizen Jaelyn O. Culpepper (1): 1 summaries
[LLMConversations] ========== Generated System Prompt ==========
[LLMConversations] Added 1 memories to system prompt
[LLMConversations] Full Prompt: You are Jaelyn O. Culpepper...

Your memories of past conversations with TheRealLemon:
- User disclosed being 28 years old...

Use these memories to maintain continuity in your conversation...

Recent colony events you're aware of:
...
```
**Expected:** 
- "Retrieved memory" shows > 0 summaries
- "Added X memories to system prompt" 
- Memory section visible in Full Prompt

## Testing the Complete Flow

### Test 1: Basic Memory Creation
1. Start conversation with citizen
2. Say: "I'm 28 years old"
3. End conversation (ESC)
4. **Check logs for:**
   - "Saved memory for citizen X (ID): 1 summaries" ✓
   - Debug message in chat: "[Debug] Summary saved: User disclosed being 28..."

### Test 2: Memory Retrieval
1. Start NEW conversation with SAME citizen
2. **Check logs for:**
   - "Retrieved memory for citizen X (ID): 1 summaries" ✓
   - "Added 1 memories to system prompt" ✓
   - System prompt contains "Your memories of past conversations"
3. Say: "How old am I?"
4. **Expected response:** Citizen should reference "28" from memory

### Test 3: Multiple Memories
1. Have 3 separate conversations with same citizen
2. Each time, share different information
3. On 4th conversation, check logs:
   - "Retrieved memory for citizen X (ID): 3 summaries" ✓
   - "Added 3 memories to system prompt" ✓
   - All 3 memories visible in system prompt

## Common Issues & Solutions

### Issue: "Retrieved memory: 0 summaries" on second conversation
**Cause:** Memory not being saved properly
**Check:** Look for "Saved memory" message after first conversation
**Solution:** Verify the build includes latest ConversationMemory.java changes

### Issue: Memories in system prompt but citizen doesn't remember
**Cause:** LLM not following instructions or summary too vague
**Check:** Look at the actual summary text in the system prompt
**Solution:** 
- Ensure summaries are specific (e.g., "User is 28 years old" not "casual chat")
- Check that prompt includes "Use these memories for continuity"

### Issue: No "Added X memories" debug message
**Cause:** buildMemoryContext() not logging or returning empty string
**Check:** Verify PromptBuilder.java has the debug logging
**Solution:** Ensure buildMemoryContext() method is being called

## Memory Cache Behavior

### During Game Session
- Memories stored in `MEMORY_CACHE` HashMap
- Key: Citizen ID (integer)
- Value: ConversationMemory object with list of summaries
- Persists until server stops

### On Server Restart
- Cache is cleared (HashMap is static but not serialized)
- All memories lost
- This is acceptable for now (noted in TODO comments)
- Future improvement: Serialize to NBT files

## Debug Commands (Not implemented yet, but useful ideas)

Potential future additions:
- `/llm memory list <citizen>` - Show all memories for a citizen
- `/llm memory clear <citizen>` - Clear memories for testing
- `/llm memory test` - Run automated memory test sequence

## Performance Notes

- HashMap lookup is O(1) - very fast
- No file I/O during conversations - instant
- Memory per citizen: ~100-500 bytes (8 summaries × ~50 chars each)
- 100 citizens with memories = ~50KB total (negligible)

