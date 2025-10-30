# Memory System Test Guide

## Quick Test Procedure

### Test 1: Basic Memory Recall
1. **Start Game** and approach a citizen
2. **Start Conversation** (press L key while looking at citizen)
3. **Share Information:**
   ```
   Player: "My name is John and I'm 28 years old"
   ```
4. **End Conversation** (press ESC)
   - Watch for debug message: "[Debug] Summary saved: ..."
5. **Start New Conversation** with the same citizen
6. **Test Recall:**
   ```
   Player: "Do you remember my age?"
   ```
   Expected: Citizen should say something like "Yes, you're 28 years old!"

### Test 2: Multiple Memories
1. **First Conversation:**
   ```
   Player: "I love building with oak wood"
   ```
   End conversation
   
2. **Second Conversation:**
   ```
   Player: "My favorite color is blue"
   ```
   End conversation
   
3. **Third Conversation:**
   ```
   Player: "What do you know about me?"
   ```
   Expected: Citizen should reference both oak wood preference and blue color

### Test 3: Memory Limits
The system keeps the 8 most recent memories. Test this by having 10+ short conversations and verify that older memories are forgotten.

## Viewing Debug Information

The system outputs detailed debug info to the logs:

1. **When Starting Conversation:**
   ```
   [LLMConversations] ========== Generated System Prompt ==========
   [LLMConversations] Added X memories to system prompt
   ```

2. **When Ending Conversation:**
   ```
   [LLMConversations] === STARTING SUMMARIZATION FOR [Citizen Name] ===
   [LLMConversations] Summary: [Generated summary]
   [LLMConversations] ✓ Summary successfully saved to memory!
   ```

3. **In Game:**
   - Gray chat message: "[Debug] Summary saved: ..."

## Expected System Prompt Structure

When you have memories, the system prompt should look like:

```
You are [Citizen Name], a [job] in the colony of [Colony Name]. 
You are speaking with [Player Name]. 
Your happiness is X/10. 
Be friendly, stay in character, and keep responses brief (1-3 sentences).

Your memories of past conversations with [Player Name]:
- User disclosed they are 28 years old; assistant responded with informal chat
- Player mentioned preference for oak wood construction materials
- Player shared that their favorite color is blue

Use these memories to maintain continuity in your conversation. 
If the player asks you something you should know from a previous conversation, refer to your memories.

Recent colony events you're aware of:
- [Event 1]
- [Event 2]
...
```

## Troubleshooting

### Citizen doesn't remember
**Check:**
1. Did you see the "[Debug] Summary saved: ..." message?
2. Check logs for "Added X memories to system prompt"
3. Make sure you're talking to the same citizen

### Summary not being saved
**Check:**
1. API key is configured correctly
2. Check logs for "LLM SUMMARIZATION FAILED"
3. Verify LLM service is responding

### Empty memories
**Check:**
1. Did you have an actual conversation (not just starting/ending immediately)?
2. Check logs for "No conversation messages to summarize"

## Advanced: Viewing Raw Data

Memories are stored in the world save under:
```
saves/[World Name]/data/llmconversations_memory_[citizen_id].dat
```

You can examine these NBT files to see stored memories directly.

## Performance Notes

- Memories are loaded fresh each conversation (no caching issues)
- Max 8 memories kept per citizen
- Each summary is 10-20 words (compact)
- Negligible performance impact

## Success Indicators

✓ Debug message shows summary saved after each conversation
✓ Debug message shows "Added X memories to system prompt" when starting new conversation
✓ Citizen references past conversation details naturally
✓ Conversation feels continuous across multiple sessions

