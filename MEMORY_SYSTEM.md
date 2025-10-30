# LLM-Powered Memory System

## 🧠 How It Works

Each NPC in your colony has **their own persistent memory** of conversations with players. The memory system uses the LLM itself to create intelligent summaries.

## Memory Flow

### 1. **Starting a Conversation**
```
Player shift+right-clicks NPC
↓
System loads NPC's past conversation summaries
↓
Summaries are added to conversation context
↓
LLM receives: "Previous conversation summary: [summaries]"
```

### 2. **During Conversation**
```
Each message exchange is stored in conversation history
↓
Full context maintained for current conversation
↓
NPC can reference what was just discussed
```

### 3. **Ending a Conversation**
```
Player says "goodbye"
↓
Entire conversation sent to LLM for summarization
↓
LLM creates 10-20 word summary focusing on:
  - Key topics discussed
  - Requests made
  - Information shared
  - Any commitments
↓
Summary saved to NPC's memory
↓
Old summaries retained (configurable limit)
```

## Example Memory Progression

### First Conversation:
```
Player: "Hey John, can you build me a house?"
John: "Of course! I'll need oak planks and stone. Where should I build it?"
Player: "Near the town hall, thanks!"
```
**LLM-Generated Summary:** "Player requested house near town hall; needs oak planks and stone"

### Second Conversation (Days Later):
```
System context: "Previous conversation summary: Player requested house near town hall; needs oak planks and stone"

Player: "How's the house coming along?"
John: "Ah yes, the house you wanted near the town hall! I haven't started yet - still waiting for materials."
```
**New Summary:** "Player checking on house progress; builder still needs materials"

### Third Conversation:
```
System context: 
  "Player requested house near town hall; needs oak planks and stone"
  "Player checking on house progress; builder still needs materials"

Player: "I brought the materials!"
John: "Perfect! I remember you wanted it near the town hall. I'll get started today!"
```

## Benefits of LLM Summarization

### ✅ **Intelligent Compression**
- 20-message conversation → 15-word summary
- Captures meaning, not just keywords
- Focuses on what matters for future reference

### ✅ **Context-Aware**
- Understands player requests vs casual chat
- Identifies commitments and promises
- Recognizes important information

### ✅ **Long-Term Continuity**
- NPCs remember past interactions
- Conversations feel connected over time
- Players don't need to repeat themselves

### ✅ **Efficient Token Usage**
- Summaries are much shorter than full conversations
- More memories fit in context window
- Lower API costs per conversation

## Configuration

In `config/llm-conversations-client.toml`:

```toml
[memory]
    # Number of past conversation summaries to remember (0-20)
    max_history_length = 5
```

**Recommended settings:**
- **5 summaries** (default) - Good balance, covers ~1 week of interactions
- **10 summaries** - Better memory, slightly higher token usage
- **3 summaries** - Minimal, only very recent conversations
- **20 summaries** - Maximum continuity, higher API costs

## Memory Per NPC

**Important:** Each NPC has their own separate memory!

- **John the Builder** remembers conversations with him
- **Sarah the Farmer** has her own memories
- **Mike the Guard** doesn't know what you told John
- Memories persist across:
  - Game restarts
  - World saves/loads
  - Server restarts

## Technical Details

### Storage
- Memories stored in citizen NBT data
- Saved with world save
- Backed up with world backups

### Summarization
- Uses same LLM model as conversations
- Special system prompt for summarization:
  ```
  "You are a memory assistant for [NPC Name], a [Job].
   Create brief, factual summaries that help remember key conversation points.
   Focus on: requests made, information shared, topics discussed, and any commitments."
  ```

### Fallback
- If LLM summarization fails (API error, network issue)
- Falls back to simple "Talked about X topics" summary
- Conversation still saved

## Cost Implications

### Per Conversation:
- **Main conversation**: ~150 tokens per response (configurable)
- **Summarization**: ~50-100 tokens (one-time at end)
- **Loading memory**: ~50 tokens per conversation (5 summaries × 10 words)

### Example Cost (using Claude Haiku at $0.25/1M tokens):
- 10-message conversation: ~1,500 tokens = $0.000375 (less than a penny)
- With summarization: +100 tokens = $0.000025
- Loading 5 summaries: +50 tokens = $0.000013

**Total: ~$0.0004 per conversation** (fractions of a cent)

## Tips for Best Memory

### For Players:
1. **Be specific** - Clear requests are better summarized
2. **Use names** - "Build near the tavern" vs "build over there"
3. **Mention commitments** - "I'll bring materials tomorrow"
4. **Reference past talks** - Test if NPC remembers

### For Configuration:
1. **Longer conversations** - Increase `max_tokens` if NPCs forget mid-conversation
2. **More history** - Increase `max_history_length` for better long-term memory
3. **Better summaries** - Use a more capable model (gpt-4o-mini vs haiku)

## Example Conversation with Memory

```
=== Week 1, Day 1 ===
Player: "Hi Emma! I need help setting up a farm."
Emma: "I'd be happy to help! What crops are you thinking about?"
Player: "Wheat and carrots mainly."
Emma: "Good choices! I can get started tomorrow if you have seeds."

Summary saved: "Player needs farm setup with wheat and carrots; Emma to start tomorrow"

=== Week 1, Day 3 ===
Player: "Emma, how's the farm?"
Emma: "The farm you wanted with wheat and carrots? I planted the wheat yesterday! Still need carrot seeds though."

=== Week 2, Day 1 ===
Player: "I brought carrot seeds!"
Emma: "Perfect timing! I'll add them to the farm we started last week. The wheat is already growing well!"
```

## Advanced: Custom Summary Prompts

Want to customize how summaries are created? Edit `LLMConversationHandler.java`:

```java
// Current prompt (line ~228):
String summaryPrompt = "Summarize the key points and topics from this conversation...";

// Customize to:
String summaryPrompt = "As [NPC Name], what should I remember from this conversation? " +
                      "Include any promises I made and what the player needs.";
```

## Memory Privacy

- Memories are stored **locally** in your world save
- Conversations are sent to OpenRouter for summarization
- Each player's conversations are separate
- No cross-player memory (by default)

---

**The memory system makes NPCs feel alive and continuous!** Each conversation builds on the last, creating a genuine relationship between players and citizens. 🧠✨
