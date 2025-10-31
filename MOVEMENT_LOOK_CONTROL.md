# Movement and Look Control During Conversations

## Feature Overview

NPCs now stop walking and maintain eye contact with the player throughout conversations. This creates a more immersive and natural interaction experience.

---

## Implementation Summary

### Key Methods Added

#### 1. `stopMovementAndLookAtPlayer()`
**Location**: `ConversationStateManager.java:188`

Called when conversation starts. Does three things:
1. Stops citizen navigation: `citizen.getNavigation().stop()`
2. Stores citizen entity reference for maintenance
3. Makes citizen look at player: `citizen.getLookControl().setLookAt(player, 30.0F, 30.0F)`

#### 2. `maintainLookAtPlayer()`
**Location**: `ConversationStateManager.java:206`

Called every tick (every 50ms) during conversation. Ensures:
1. Navigation stays stopped (re-stops if AI tries to start it)
2. Citizen continuously faces the player
3. Handles edge cases (citizen removed, player disconnected)

#### 3. `resumeMovement()`
**Location**: `ConversationStateManager.java:232`

Called when conversation ends. Simply clears entity references - AI naturally resumes control.

---

## How It Works

### Start Conversation Flow
```
Player right-clicks citizen
  ↓
StartConversationPacket sent to server
  ↓
ConversationStateManager.startConversation() called
  ↓
stopMovementAndLookAtPlayer() executed:
  - citizen.getNavigation().stop()
  - Store citizen in conversationEntities map
  - Store player UUID in lookAtPlayers map
  - citizen.getLookControl().setLookAt(player)
  ↓
Citizen stops walking and turns to face player
```

### During Conversation (Every Tick)
```
StatMaintenanceHandler.onServerTick() [every 20 ticks]
  ↓
ConversationStateManager.maintainFrozenStats() called
  ↓
maintainLookAtPlayer() executed:
  - Check if navigation restarted → stop it
  - Find player by UUID
  - Update look direction: setLookAt(player)
  ↓
Citizen remains frozen, constantly facing player
```

### End Conversation Flow
```
Player types "bye" or presses ESC
  ↓
LLMConversationHandler.endConversation() called
  ↓
ConversationStateManager.endConversation() called
  ↓
resumeMovement() executed:
  - Retrieves citizen reference
  - No explicit resume needed
  ↓
Remove from all tracking maps:
  - conversationEntities.remove()
  - lookAtPlayers.remove()
  ↓
Job.resetAI() called
  ↓
AI state machine resumes control of navigation and look
  ↓
Citizen returns to work
```

---

## Data Structures

### New Maps Added

```java
// Stores entity references for movement control
Map<Integer, AbstractEntityCitizen> conversationEntities

// Stores which player the citizen should look at
Map<Integer, UUID> lookAtPlayers
```

Both maps use citizen ID as key for quick lookup during maintenance.

---

## Technical Details

### Navigation Control
- **Stop Method**: `citizen.getNavigation().stop()`
  - Clears the pathfinding queue
  - Stops current movement
  - Does NOT prevent AI from starting new navigation
  
- **Maintenance Approach**: 
  - Every tick, check: `citizen.getNavigation().isInProgress()`
  - If true, call `stop()` again
  - This prevents AI from taking control during conversation

### Look Control
- **Set Method**: `citizen.getLookControl().setLookAt(player, 30.0F, 30.0F)`
  - First parameter: Target entity (the player)
  - Second parameter: Max yaw change per tick (30 degrees)
  - Third parameter: Max pitch change per tick (30 degrees)
  
- **Why Update Every Tick?**
  - Player can move around
  - Citizen should track player's position
  - Look control naturally decays without updates

### Resume Strategy
- **No explicit "resume" command**
- When conversation ends:
  1. Entity references removed from maps
  2. `maintainLookAtPlayer()` stops being called
  3. `Job.resetAI()` resets state machine to IDLE
  4. AI transitions to WORK on next tick
  5. Navigation and look control return to AI
  
- **Why This Works?**
  - Minecraft's AI systems are designed to take control when not interrupted
  - Simply stopping interference allows natural resumption
  - Clean, no lingering state issues

---

## Edge Cases Handled

### 1. Citizen Entity Removed
```java
if (citizen != null && !citizen.isRemoved()) {
    // Only control if citizen still exists
}
```

### 2. Player Disconnected
```java
ServerPlayer player = citizen.getServer().getPlayerList().getPlayer(playerUUID);
if (player != null && citizen.getLookControl() != null) {
    // Only update if player still exists
}
```

### 3. Navigation Check
```java
if (citizen.getNavigation().isInProgress()) {
    citizen.getNavigation().stop();
}
```
Only stops if navigation is active (avoids unnecessary calls).

---

## Performance Considerations

### Optimization: Check Interval
- `maintainLookAtPlayer()` called via `maintainFrozenStats()`
- `maintainFrozenStats()` runs every 20 ticks (1 second)
- **BUT** look direction updates every single tick via Minecraft's entity tick
- The 20-tick interval just ensures navigation stays stopped

### Memory Usage
- 2 additional HashMaps with minimal overhead
- Entity references cleared on conversation end
- No memory leaks (references removed from all maps)

### CPU Usage
- Minimal: Just hashmap lookups and method calls
- Only processes citizens actively in conversation
- No iteration over all citizens

---

## Testing Checklist

- [ ] Citizen stops walking when conversation starts
- [ ] Citizen turns to face player immediately
- [ ] Citizen tracks player movement (if player walks around)
- [ ] Citizen resumes walking/working after conversation ends
- [ ] Multiple conversations can occur simultaneously
- [ ] Works correctly if player disconnects during conversation
- [ ] Works correctly if citizen is removed during conversation
- [ ] No navigation issues after conversation ends
- [ ] Citizens properly return to their jobs

---

## Future Enhancements

### ✅ Implemented
- **Distance check**: ✅ DONE - Conversation auto-ends if player walks too far (configurable, see `DISTANCE_AUTO_END.md`)

### Possible Improvements
1. **Smooth head rotation**: Add interpolation for more natural turning
2. **Body rotation**: Make citizen's body rotate to face player (not just head)
3. **Animation**: Play "talking" animation during conversation
4. **Gesture system**: Citizens could gesture while talking

### Implementation Notes
- All enhancements should maintain the same cleanup pattern
- Keep performance in mind (these run every tick)
- Consider adding config options for behavior customization

---

## Code Locations

| Feature | File | Method | Line |
|---------|------|--------|------|
| Stop movement | `ConversationStateManager.java` | `stopMovementAndLookAtPlayer()` | 188 |
| Maintain look | `ConversationStateManager.java` | `maintainLookAtPlayer()` | 206 |
| Resume movement | `ConversationStateManager.java` | `resumeMovement()` | 232 |
| Tick handler | `StatMaintenanceHandler.java` | `onServerTick()` | 20 |

---

*Generated: 2025-10-31*
*Feature: Movement and look control during conversations*

