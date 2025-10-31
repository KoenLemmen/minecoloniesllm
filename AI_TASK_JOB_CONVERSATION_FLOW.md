# AI, Task, and Job Management During Conversations

## Technical Summary: What Happens When Conversations Start and End

This document details the technical flow of how citizen AI, tasks, and jobs are managed during LLM conversations in the mod.

---

## 🟢 STARTING A CONVERSATION

### Entry Point
**Method**: `ConversationStateManager.startConversation(ServerPlayer player, AbstractEntityCitizen citizen)`
- **Location**: `ConversationStateManager.java:40`

### Step-by-Step Process

#### 1. **Register Conversation State**
```
ConversationStateManager.startConversation(citizenId, playerUUID)
```
- Adds mapping: `citizenId -> playerUUID` to `activeConversations` HashMap
- This marks the citizen as "in conversation" for the entire system

#### 2. **Create LLM Handler**
```
LLMConversationHandler handler = new LLMConversationHandler(citizenData)
ConversationStateManager.registerHandler(citizenId, handler)
```
- Creates conversation handler instance
- Stores in `handlers` HashMap (`citizenId -> LLMConversationHandler`)
- Handler initializes:
  - OpenRouterClient for LLM communication
  - Conversation history (empty ArrayList of ChatMessages)
  - Unique conversation UUID for tracking
  - Sets initial priority: `ChatPriority.CHITCHAT`

#### 3. **Trigger Minecolonies Interaction System**
```
citizenData.triggerInteraction(handler)
```
- Hooks into Minecolonies' interaction system
- The handler extends `AbstractInteractionResponseHandler`
- This integration allows the mod to work within Minecolonies' existing framework

#### 4. **Freeze Citizen Stats**
```
ConversationStateManager.freezeStats(citizenData)
```
- **Method**: `ConversationStateManager.freezeStats(ICitizenData citizen)`
- **Location**: `ConversationStateManager.java:137`
- Captures current saturation value: `citizen.getSaturation()`
- Stores in `frozenSaturation` HashMap (`citizenId -> Double`)
- **Purpose**: Prevents hunger/saturation from decreasing during conversation

#### 5. **Stop Movement and Look at Player**
```
ConversationStateManager.stopMovementAndLookAtPlayer(citizen, player)
```
- **Method**: `ConversationStateManager.stopMovementAndLookAtPlayer(AbstractEntityCitizen, ServerPlayer)`
- **Location**: `ConversationStateManager.java:188`
- **Actions**:
  1. `citizen.getNavigation().stop()` - Stops pathfinding/navigation
  2. Stores citizen entity in `conversationEntities` HashMap (`citizenId -> AbstractEntityCitizen`)
  3. Stores player UUID in `lookAtPlayers` HashMap (`citizenId -> UUID`)
  4. `citizen.getLookControl().setLookAt(player, 30.0F, 30.0F)` - Makes citizen face player
- **Result**: Citizen stops walking and turns to face the player

#### 6. **Stat and Movement Maintenance Loop** (Ongoing)
```
StatMaintenanceHandler.onServerTick(ServerTickEvent.Post event)
```
- **Location**: `StatMaintenanceHandler.java:20`
- **Frequency**: Every 20 ticks (1 second)
- **Process**:
  1. Iterates through all colonies and citizens
  2. Calls `ConversationStateManager.maintainFrozenStats(citizen)` for each
  3. If citizen is in conversation (`isInConversation(citizenId)`):
     - Retrieves frozen saturation value from HashMap
     - Restores it: `citizen.setSaturation(frozenValue)`
     - Calls `maintainLookAtPlayer(citizen)` to keep them facing player
  
**`maintainLookAtPlayer(ICitizenData)` Details**:
- **Location**: `ConversationStateManager.java:206`
- **Actions every tick**:
  1. Retrieves citizen entity from `conversationEntities` map
  2. Retrieves player UUID from `lookAtPlayers` map
  3. Checks if navigation started: `citizen.getNavigation().isInProgress()`
  4. If yes, stops it again: `citizen.getNavigation().stop()`
  5. Finds the player in the server player list
  6. Updates look direction: `citizen.getLookControl().setLookAt(player, 30.0F, 30.0F)`
- **Result**: Citizen remains frozen in place, constantly facing the player

### What IS Actively Controlled
- **Navigation/Movement**: Stopped when conversation starts, continuously prevented during conversation
- **Look Direction**: Continuously updated to face the player (every tick)
- **Saturation**: Frozen and restored every second

### What is NOT Paused
- **AI State Machine**: Not explicitly frozen at conversation start
- **Job Tasks**: Not explicitly cancelled (but can't execute without movement)
- **Entity Rendering**: Citizen continues to render and animate

---

## 🔴 ENDING A CONVERSATION

### Entry Points
Multiple paths can trigger conversation end:

1. **Player types exit word** (e.g., "bye", "goodbye")
   - `ChatInterceptHandler.onPlayerChat()` → `isExitWord()` → `endConversation()`
   
2. **Player presses ESC key** 
   - `EndConversationPacket` sent from client → server handles
   
3. **Citizen no longer exists**
   - `ChatInterceptHandler.onPlayerChat()` detects null citizen → cleanup

### Main End Method
**Method**: `LLMConversationHandler.endConversation(Player player)`
- **Location**: `LLMConversationHandler.java:210`

### Step-by-Step Process

#### 1. **LLM Summarization** (Async)
```
summarizeConversationWithLLM(player)
```
- **Only if** conversation has actual messages (user/assistant roles)
- Sends conversation history to LLM for intelligent summary
- Summary stored in `ConversationMemory` for future reference
- **Non-blocking**: Happens asynchronously via `CompletableFuture`

#### 2. **Unfreeze Stats** (CRITICAL - Must happen first)
```
ConversationStateManager.unfreezeStats(citizenData)
```
- **Method**: `ConversationStateManager.unfreezeStats(ICitizenData citizen)`
- **Location**: `ConversationStateManager.java:128`
- Removes citizen from `frozenSaturation` HashMap
- **Important**: Done BEFORE resuming work to allow natural stat decay

#### 3. **Resume Job and Reset AI**
```java
if (citizenData.getJob() != null) {
    citizenData.getJob().onWakeUp();
    citizenData.getJob().resetAI();
}
```

**a) `citizenData.getJob().onWakeUp()`**
- **Purpose**: Resets job-specific state flags
- **Example flags reset**:
  - `searchedForFoodToday = false` (for workers who eat)
  - Other job-specific daily/session flags
- **Effect**: Citizen treats it as a fresh start, re-evaluates needs

**b) `citizenData.getJob().resetAI()`**
- **Purpose**: Resets the AI state machine to `IDLE`
- **Result**: On next tick, CitizenAI transitions automatically to `WORK` state
- **Benefit**: Clean state transition, no lingering conversation behaviors

#### 4. **Resume Movement**
```
ConversationStateManager.resumeMovement(citizenId)
```
- **Method**: `ConversationStateManager.resumeMovement(int citizenId)`
- **Location**: `ConversationStateManager.java:232`
- Retrieves citizen entity from `conversationEntities` map
- **Note**: No explicit "resume" command needed
- Navigation and look control will automatically return to normal AI behavior
- Simply removing from tracking maps allows AI to take over again

#### 5. **Clear Conversation State**
```
ConversationStateManager.endConversation(citizenData.getId())
```
- **Method**: `ConversationStateManager.endConversation(int citizenId)`
- **Location**: `ConversationStateManager.java:68`
- Removes from 5 HashMaps:
  1. `activeConversations.remove(citizenId)` - conversation tracking
  2. `handlers.remove(citizenId)` - handler cleanup
  3. `frozenSaturation.remove(citizenId)` - stat tracking
  4. `conversationEntities.remove(citizenId)` - entity reference cleanup
  5. `lookAtPlayers.remove(citizenId)` - look target cleanup

#### 5. **Notify Player**
```
player.sendSystemMessage(Component.translatable("llmconversations.message.conversation_ended", citizenData.getName()))
```
- Sends localized chat message to player

#### 6. **Sync to Client**
```
PacketDistributor.sendToPlayer(serverPlayer, 
    new SyncConversationStatePacket(citizenId, false))
```
- Updates client-side state
- Client UI can react (e.g., hide conversation indicators)

---

## 📋 State Management Data Structures

### Active Conversations Map
```java
Map<Integer, UUID> activeConversations
```
- **Key**: Citizen ID (Integer)
- **Value**: Player UUID
- **Purpose**: Track which citizen is talking to which player

### Handler Map
```java
Map<Integer, LLMConversationHandler> handlers
```
- **Key**: Citizen ID (Integer)
- **Value**: LLMConversationHandler instance
- **Purpose**: Store active conversation handlers with history and context

### Frozen Saturation Map
```java
Map<Integer, Double> frozenSaturation
```
- **Key**: Citizen ID (Integer)
- **Value**: Saturation value (Double)
- **Purpose**: Store original saturation to restore each tick

### Conversation Entities Map
```java
Map<Integer, AbstractEntityCitizen> conversationEntities
```
- **Key**: Citizen ID (Integer)
- **Value**: AbstractEntityCitizen entity reference
- **Purpose**: Keep reference to citizen entity for movement/look control

### Look At Players Map
```java
Map<Integer, UUID> lookAtPlayers
```
- **Key**: Citizen ID (Integer)
- **Value**: Player UUID
- **Purpose**: Track which player the citizen should be looking at

---

## 🔄 AI State Machine Flow

### Normal Operation (No Conversation)
```
IDLE → WORK → [Job-specific tasks] → WORK → IDLE → ...
```

### During Conversation
```
WORK → [conversation starts]
  ↓
Stats frozen (saturation locked)
  ↓
AI continues ticking (no explicit pause)
  ↓
[conversation ends] → onWakeUp() → resetAI()
  ↓
IDLE → WORK → [Job-specific tasks resumes]
```

### Critical Methods
1. **`onWakeUp()`**: Clears job session state (e.g., "already searched for food today")
2. **`resetAI()`**: Forces AI back to IDLE, which naturally transitions to WORK
3. **Result**: Clean slate for citizen to resume normal behavior

---

## 🎯 Key Design Principles

### 1. **Movement and Look Control via Active Maintenance**
- Navigation is stopped when conversation starts
- Every tick (via `maintainLookAtPlayer()`), the system:
  - Checks if navigation has restarted and stops it again
  - Updates the citizen's look direction to face the player
- This "active maintenance" approach prevents AI from taking control
- When conversation ends, simply removing from tracking maps lets AI resume naturally

### 2. **Stats Frozen, Not AI**
- Saturation is actively restored every second
- AI state machine continues ticking (but can't move citizen)
- Citizens still render, animate, and process logic

### 3. **Graceful Resume**
- `onWakeUp()` + `resetAI()` ensures clean state
- No lingering "conversation mode" flags
- Citizen immediately returns to work duties
- Movement and look control naturally return to AI

### 4. **Memory Persistence**
- Conversation summary stored asynchronously
- Doesn't block conversation ending
- Available for future conversations (context building)

### 5. **Multi-Path Safety**
- Conversation can end via chat, keybind, or error
- All paths converge to same cleanup logic
- Fallback cleanup in `ChatInterceptHandler.endConversation()` if handler missing

---

## 🐛 Edge Cases Handled

### Citizen Disappears During Conversation
**Location**: `ChatInterceptHandler.onPlayerChat()`
```java
ICitizenData citizenData = findCitizen(player, citizenId);
if (citizenData == null) {
    ConversationStateManager.endConversation(citizenId);
    player.sendSystemMessage(Component.translatable("llmconversations.message.citizen_left"));
    return;
}
```

### Handler Missing During End
**Location**: `ChatInterceptHandler.endConversation()`
```java
if (handler != null) {
    handler.endConversation(player); // Proper end with LLM summary
} else {
    // Fallback: Still unfreezes stats, resets AI, cleans up state
    ConversationStateManager.unfreezeStats(citizenData);
    ConversationStateManager.endConversation(citizenData.getId());
    if (citizenData.getJob() != null) {
        citizenData.getJob().onWakeUp();
        citizenData.getJob().resetAI();
    }
}
```

---

## 📊 Summary Table

| Phase | Method | Effect on AI | Effect on Movement | Effect on Look | Effect on Stats |
|-------|--------|--------------|-------------------|----------------|-----------------|
| **Start** | `startConversation()` | AI continues | Navigation stopped | Faces player | Stats frozen |
| | `freezeStats()` | No change | No change | No change | Saturation captured |
| | `triggerInteraction()` | Hooks into Minecolonies | No change | No change | No change |
| | `stopMovementAndLookAtPlayer()` | No change | **STOPPED** | **Set to player** | No change |
| **During** | `maintainFrozenStats()` | No change | Re-stopped if started | Updated to player | Restored every 20 ticks |
| | (every 1 second) | | **Continuously frozen** | **Continuously updated** | |
| | `maintainLookAtPlayer()` | No change | Stops navigation if active | Updates every tick | No change |
| **End** | `resumeMovement()` | No change | Clears entity ref | Clears look target | No change |
| | `unfreezeStats()` | No change | No change | No change | Unfrozen |
| | `onWakeUp()` | No change | No change | No change | Job flags reset |
| | `resetAI()` | State → IDLE → WORK | AI resumes pathfinding | AI resumes look control | No change |
| | `endConversation()` | No change | Returns to normal | Returns to normal | Stats decay normally |

---

## 🔧 Technical Notes

### Why Unfreeze Before Resume?
```java
ConversationStateManager.unfreezeStats(citizenData); // FIRST
// ...then...
citizenData.getJob().onWakeUp(); // Job resume
citizenData.getJob().resetAI();  // AI resume
```
**Reason**: If stats are still frozen when job resumes, the maintenance loop could interfere with natural stat updates. Unfreezing first ensures clean transition.

### Why Both onWakeUp() AND resetAI()?
- **`onWakeUp()`**: Job-level state reset (task-specific flags)
- **`resetAI()`**: AI-level state reset (state machine)
- They operate at different abstraction levels, both needed for complete reset

### Async Summary
The LLM summarization happens asynchronously to avoid blocking:
```java
CompletableFuture<String> summaryFuture = llmClient.generateSummary(...);
summaryFuture.thenAccept(summary -> {
    ConversationMemory.storeMemory(...);
});
```
The conversation ends immediately; summary is saved when LLM responds.

---

*Generated: 2025-10-31*
*Based on code analysis of minecoloniesllm mod*

