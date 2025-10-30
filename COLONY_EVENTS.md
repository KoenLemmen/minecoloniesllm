# Colony Events Context System

## 🌟 Overview

NPCs in your colony are now **aware of recent events**! They can naturally reference births, deaths, constructions, and other happenings during conversations.

## How It Works

### Automatic Context Injection

When a conversation starts, the system automatically:

1. **Fetches recent colony events** (last 8 events by default)
2. **Formats them naturally** for the LLM
3. **Adds them to the system prompt**
4. **NPCs can reference these events** in their responses

### Event Types Tracked

**Citizen Events:**
- 👶 **Births** - "Did you hear? Sarah had a baby!"
- 💀 **Deaths** - "I'm so sad about John passing away"
- 🚶 **New arrivals** - "We have a new citizen, Emma just joined!"
- 👨 **Growing up** - "Little Tommy is all grown up now"
- 🏨 **Visitors** - "There's a visitor at the tavern"

**Building Events:**
- 🏗️ **New constructions** - "I saw they finished the new bakery"
- ⬆️ **Upgrades** - "The town hall just got upgraded to level 3!"
- 🔧 **Repairs** - "Finally fixed the damaged guard tower"
- 💥 **Removals** - "They tore down the old warehouse"

## Example Conversations

### Without Events (Old Way):
```
Player: "Hi John! How are you?"
John: "I'm doing well, just working on building projects."
```

### With Events (New Way):
```
Player: "Hi John! How are you?"
John: "I'm doing well! Did you hear Sarah had a baby? The colony is growing! 
       Also excited about the new bakery being built - fresh bread soon!"
```

---

### Example: Builder Awareness
```
Player: "What's new around here?"
Builder: "Quite a lot actually! We just upgraded the Town Hall to level 3, 
          and there's talk of building a new guard tower. 
          Keeps me busy, but I love seeing the colony grow!"
```

### Example: Guard Showing Emotion
```
Player: "How are things?"
Guard: "Honestly, a bit somber. We lost Mike the miner yesterday. 
        He was a good man. But life goes on - we have new citizens 
        joining, and the colony needs protection more than ever."
```

### Example: Farmer Celebrating Growth
```
Player: "Good morning!"
Farmer: "Good morning! What a week! Little Emma was born, and we 
         finished upgrading the farm. More mouths to feed, but 
         thankfully more crops too!"
```

## Technical Details

### Event Fetch Limit

Default: **8 most recent events**

Why 8?
- Keeps context concise (saves tokens)
- Recent enough to be relevant
- Old enough to avoid spam

Configurable in code:
```java
// In PromptBuilder.java, line ~132
int maxEvents = 8; // Change this number
```

### Event Formatting

Events are formatted naturally:

**Raw Event:** `CitizenBornEvent: citizen_123`  
**Formatted:** `- Sarah was born in the colony`

**Raw Event:** `BuildingUpgradedEvent: townhall, level 3`  
**Formatted:** `- The Town Hall was upgraded to level 3`

### Context Injection Point

Events are appended to the system prompt:

```
[Base System Prompt]
Your name is John, a builder in Oakville colony...

[Events Context - Auto-Added]
Recent colony events you're aware of:
- Sarah was born in the colony
- The Town Hall was upgraded to level 3
- Mike the miner passed away
- A new Bakery was built

You can naturally mention these events in conversation if relevant.
```

## Token Usage

### Cost Impact

Each event adds ~10-15 tokens to the system prompt:

- **Without events**: ~100 tokens system prompt
- **With 8 events**: ~180 tokens system prompt
- **Per conversation cost increase**: ~$0.00002 (negligible)

The extra context is **worth it** for the immersion boost!

## Configuration

### Adjusting Event Count

Want more or fewer events? Edit `PromptBuilder.java`:

```java
// Line ~132
int maxEvents = 8; // Default

// More awareness (costs more tokens):
int maxEvents = 15;

// Less context (saves tokens):
int maxEvents = 5;

// No events:
int maxEvents = 0; // Or return "" early
```

### Filtering Events

Want to only show certain event types? Add filtering:

```java
// In buildColonyEventsContext()
for (int i = startIndex; i < events.size(); i++) {
    IColonyEventDescription event = events.get(i);
    
    // Example: Only show births and constructions
    if (event instanceof ICitizenEventDescription) {
        ICitizenEventDescription ce = (ICitizenEventDescription) event;
        if (!ce.getName().toLowerCase().contains("born")) {
            continue; // Skip non-birth citizen events
        }
    }
    // ... rest of code
}
```

## Benefits

### 1. **Immersion** 🎭
NPCs feel like they live in the colony, not just exist as dialogue boxes

### 2. **Dynamic Conversations** 🔄
Same NPC, different day = different conversation based on events

### 3. **Emotional Depth** 💝
NPCs can express joy, sadness, excitement about colony happenings

### 4. **Natural Storytelling** 📖
The colony's story unfolds through NPC dialogue

### 5. **Player Feedback** 📢
"What's new?" becomes a genuine question with real answers

## Example Scenarios

### New Player Welcome
```
Player (new to colony): "Hi! I'm new here."
Villager: "Welcome! You picked a good time - we just had a baby born 
           and finished building a new farm. The colony is thriving!"
```

### Checking In After Time Away
```
Player (returns after days): "What did I miss?"
Guard: "Quite a bit! We lost old Thomas, but two new citizens joined. 
        Also, the blacksmith was upgraded. Colony keeps changing!"
```

### Tragedy Response
```
Player: "How are you holding up?"
NPC: "Not great... we lost Emma yesterday. She was such a kind soul. 
      The whole colony is mourning. We planted flowers by the town hall."
```

### Celebration
```
Player: "Everyone seems happy today!"
NPC: "Of course! We just completed the grand library, there were two 
      babies born this week, and we upgraded the tavern. Good times!"
```

## Limitations & Notes

### What NPCs Know

✅ NPCs know about:
- Events that are logged in colony event system
- Recent events (last ~8 by default)
- Colony-wide happenings

❌ NPCs don't automatically know about:
- Player's inventory
- Private conversations
- Events from other dimensions
- Events older than the stored limit

### Memory vs Events

**Events** (this system):
- Recent happenings (last few days)
- Colony-wide knowledge
- Automatically tracked

**Memory** (previous system):
- Past conversations with this NPC
- Personal interaction history
- Manually summarized

Both work together for full context!

## Customization Ideas

### Time-Based Awareness
```java
// Only mention very recent events (last hour in-game)
long currentTime = colony.getWorld().getDayTime();
// Filter events by timestamp
```

### NPC-Specific Relevance
```java
// Builders care more about construction
if (citizen.getJob().toString().contains("builder")) {
    // Prioritize building events
}
```

### Emotional Responses
```java
// Add emotion to event descriptions
if (eventName.contains("died")) {
    context.append("- ").append(name).append(" tragically passed away 😢\n");
}
```

## Troubleshooting

### "NPC doesn't mention events"

**Causes:**
1. No recent events in colony
2. LLM chose not to mention them (not relevant to conversation)
3. Events list empty (new colony)

**Solution:** Events are context, not commands. NPCs mention them when natural.

### "Too many event references"

**Solution:** Reduce `maxEvents` from 8 to 5 or 3.

### "Events from long ago"

**Solution:** Minecolonies has a limited event buffer. Old events automatically get removed.

## Performance Impact

- **Negligible** - Events are fetched once per conversation start
- **No lag** - Simple list iteration
- **Cached** - Colony keeps events in memory
- **Async-safe** - No threading issues

## Future Enhancements

Possible additions:
- **Weather events** - "It's been raining a lot"
- **Resource events** - "We're low on wood"
- **Raid events** - "We just defended against raiders!"
- **Trade events** - "A merchant visited yesterday"
- **Festival events** - "The harvest festival is tomorrow!"

---

**Colony Events make NPCs feel alive and aware!** They're not just workers - they're **living members of a dynamic community** that changes and grows. 🏘️✨
