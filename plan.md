# AI LLM Conversation Mod for Minecolonies - Implementation Plan

## 📌 Version Information

**This plan is for Minecraft 1.21.1 using NeoForge** (updated from 1.20.1/Forge)

The Minecolonies repository you're working with has multiple version branches:
- `version/main` - Minecraft 1.20.1 (Forge)
- `version/1.21` - Minecraft 1.21.1 (NeoForge) ← **You are here**

All code examples have been updated for NeoForge 1.21.1 compatibility.

## Project Overview

This document provides a comprehensive implementation plan for creating a companion mod to Minecolonies that adds AI-powered (LLM) conversations with NPCs. The mod will integrate with Minecolonies' existing interaction system and provide natural language conversations with citizens using OpenRouter API.

## Technical Stack

- **Minecraft Version**: 1.21.1
- **Mod Loader**: NeoForge 21.1.80 (minimum 21.1.0) - **⚠️ CRITICAL: 1.21+ uses NeoForge instead of Forge**
- **Java Version**: 21 (upgraded from 17)
- **Mappings**: Official 1.21.1
- **Minecolonies Version**: Latest compatible with 1.21.1
- **LLM Provider**: OpenRouter API
- **Required Dependencies**: 
  - Minecolonies (main mod)
  - HTTP client library (e.g., OkHttp or Apache HttpClient)
  - JSON parsing (Gson, already available in Minecraft/NeoForge)

## ⚠️ Important: Forge → NeoForge Migration

Starting with Minecraft 1.21, the modding ecosystem has split from Forge to **NeoForge**. This is a fork with API changes:

### Key Package Changes:
- `net.minecraftforge.*` → `net.neoforged.*`
- `@Mod.EventBusSubscriber` → `@EventBusSubscriber`
- `ForgeConfigSpec` → `ModConfigSpec`
- `ModLoadingContext` → Constructor injection with `ModContainer`
- Event classes have been reorganized (e.g., `TickEvent.ServerTickEvent` → `ServerTickEvent.Pre/Post`)

### All code examples in this document have been updated for NeoForge 1.21.1!

## Requirements Summary

### Core Features (Must Have)

1. **Conversation Initiation**: Players can right-click citizens to start AI conversations
2. **Task Pausing**: Citizens pause their current work when in conversation
3. **Stat Freezing**: Hunger and sleep don't decrease during conversations
4. **Memory System**: Citizens remember past conversations (summarized)
5. **Context Awareness**: LLM receives citizen stats, name, job, colony info
6. **Chat Integration**: Conversations happen in Minecraft's chat system
7. **Configuration UI**: In-game settings screen for:
   - OpenRouter API key
   - Model selection
   - Custom instruction prompt with placeholders
   
### Optional Features (Nice to Have)

8. **Idle Initiation**: Citizens sometimes ask players to chat when idle
9. **Configurable Probability**: Setting for how often citizens initiate conversations

## Minecolonies Integration Points Analysis

### 1. Interaction System Architecture

**Key Classes:**
- `IInteractionResponseHandler` - Base interface for all interactions
- `AbstractInteractionResponseHandler` - Abstract implementation with NBT serialization
- `ModInteractionResponseHandlers` - Registry of interaction types
- `StandardInteraction` - Simple interaction with pre-defined responses
- `QuestDialogueInteraction` - Complex dialogue tree system (good reference)

**How it Works:**
```java
// Interactions are triggered via:
citizenData.triggerInteraction(IInteractionResponseHandler handler);

// Handler provides:
- Component getInquiry() - The NPC's message
- List<Component> getPossibleResponses() - Player's response options
- void onServerResponseTriggered(int responseId, Player player, ICitizenData data)
- void onClientResponseTriggered(int responseId, Player player, ICitizenDataView data, BOWindow window)
```

**Priority System:**
- `IChatPriority` with levels: CHITCHAT, PENDING, IMPORTANT, BLOCKING
- Higher priority interactions show first
- Can be primary (speech bubble) or secondary interactions

**Registration:**
- Interactions are registered in `InteractionResponseHandlerEntry`
- Need to create custom handler entry for LLM interactions
- Must be serializable to NBT for persistence

### 2. Job/Task Pausing System

**Key Classes:**
- `IJob` - Interface for all citizen jobs
- `AbstractJob` - Base implementation
- `ITickingStateAI` - Worker AI interface

**Interruption Mechanism:**
```java
// Check if AI can be interrupted:
boolean canAIBeInterrupted() {
    if (getWorkerAI() != null) {
        return getWorkerAI().canBeInterrupted();
    }
    return true;
}

// Called when citizen wakes up or resumes work:
void onWakeUp() {
    searchedForFoodToday = false;
}
```

**Implementation Strategy:**
1. When conversation starts, set a flag that the citizen is in conversation
2. The worker AI checks this flag in `canBeInterrupted()` to return false
3. When conversation ends, clear the flag and call `onWakeUp()` to resume

**Task States:**
- Citizens use state machines (`ITickRateStateMachine`)
- States include: IDLE, WORKING, WANDERING, SLEEPING, SITTING, COMBAT
- Can add custom CONVERSING state

### 3. Citizen Data Access

**Key Interface: `ICitizenData`**

Available Data:
```java
// Identity
String getName()
UUID getID()
IColony getColony()

// Job & Building
IJob<?> getJob()
IBuilding getWorkBuilding()
IBuilding getHomeBuilding()

// Stats
double getSaturation() // 0-20, citizen needs food
ICitizenHappinessHandler getCitizenHappinessHandler()
ICitizenSkillHandler getCitizenSkillHandler()
ICitizenDiseaseHandler getCitizenDiseaseHandler()
boolean isAsleep()
boolean justAte()

// State
VisibleCitizenStatus getStatus()
JobStatus getJobStatus()
BlockPos getLastPosition()

// Interaction
void triggerInteraction(IInteractionResponseHandler handler)
void onResponseTriggered(Component key, int responseId, Player player)
```

**Stat Modification:**
```java
// To freeze stats during conversation:
void setSaturation(double saturation) // Prevent decrease
void setJustAte(boolean justAte) // Control eating behavior
void setAsleep(boolean asleep) // Control sleep state
```

**NBT Serialization:**
- All citizen data is saved/loaded via NBT
- Custom conversation history can be stored in citizen's NBT data
- Use `CompoundTag` for structured data

### 4. Networking System

**Key Classes:**
- `Network` - Main network handler wrapper
- `NetworkChannel` - Handles client-server communication
- `InteractionResponse` - Message for interaction responses

**Message Pattern:**
```java
public class YourMessage extends AbstractColonyServerMessage {
    // Constructor
    public YourMessage(int colonyId, int citizenId, 
                      ResourceKey<Level> dimension, String data) {
        super(dimension, colonyId);
        // Store data
    }
    
    // Serialization
    @Override
    public void fromBytesOverride(FriendlyByteBuf buf) {
        // Read from network
    }
    
    @Override
    public void toBytesOverride(FriendlyByteBuf buf) {
        // Write to network
    }
    
    // Execution
    @Override
    protected void onExecute(NetworkEvent.Context ctx, 
                            boolean isLogicalServer, IColony colony) {
        // Handle message
    }
}

// Sending messages:
Network.getNetwork().sendToServer(message); // Client -> Server
Network.getNetwork().sendToPlayer(message, player); // Server -> Client
```

**Chat Integration:**
```java
// Send chat messages to player:
player.sendSystemMessage(Component.literal("Message")
    .withStyle(ChatFormatting.BLUE));

// Player chat input is captured via ClientChatEvent
// Can be listened to and intercepted if needed
```

### 5. Configuration System

**Forge Config Classes:**
- `AbstractConfiguration` - Base for all configs
- `ClientConfiguration` - Client-side only (not synced)
- `CommonConfiguration` - Both sides (synced)
- `ServerConfiguration` - Server-side only
- `ForgeConfigSpec` - Spec builder

**Config Pattern:**
```java
public class LLMModConfiguration extends AbstractConfiguration {
    // Client configs (not synced, local to player)
    public final ForgeConfigSpec.ConfigValue<String> openRouterApiKey;
    public final ForgeConfigSpec.ConfigValue<String> selectedModel;
    public final ForgeConfigSpec.ConfigValue<String> instructionPrompt;
    public final ForgeConfigSpec.DoubleValue idleConversationChance;
    public final ForgeConfigSpec.BooleanValue enableIdleConversations;
    
    protected LLMModConfiguration(ForgeConfigSpec.Builder builder) {
        createCategory(builder, "llm_conversations");
        
        openRouterApiKey = defineString(builder, "api_key", "",
            "Your OpenRouter API key");
        
        selectedModel = defineString(builder, "model", 
            "anthropic/claude-3-haiku",
            "The LLM model to use for conversations");
        
        instructionPrompt = defineString(builder, "instruction_prompt",
            "You are {name}, a {job} in the colony of {colony_name}. " +
            "You are speaking with {player_name}. " +
            "Your current happiness is {happiness}/10. " +
            "Keep responses concise (1-3 sentences).",
            "System prompt template with placeholders");
        
        enableIdleConversations = defineBoolean(builder, 
            "enable_idle_conversations", false);
        
        idleConversationChance = defineDouble(builder, 
            "idle_conversation_chance", 0.1, 0.0, 1.0);
        
        finishCategory(builder);
    }
}
```

**GUI Config Screen:**
- Forge provides `ConfigScreenHandler` for in-game config editing
- Can use Cloth Config API for better UI (optional dependency)
- Or implement custom screen extending `Screen` class

## Detailed Implementation Guide

### Phase 1: Project Setup

**1.1 Create New Mod Structure**

```
llm-conversations/
├── build.gradle
├── gradle.properties
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── thereallemon/
│       │           └── llmconversations/
│       │               ├── LLMConversationsMod.java
│       │               ├── config/
│       │               ├── network/
│       │               ├── interaction/
│       │               ├── llm/
│       │               └── util/
│       └── resources/
│           ├── META-INF/
│           │   └── mods.toml
│           ├── pack.mcmeta
│           └── assets/
│               └── llmconversations/
│                   └── lang/
│                       └── en_us.json
```

**1.2 Gradle Configuration**

```gradle
// build.gradle
plugins {
    id 'eclipse'
    id 'idea'
    id 'maven-publish'
    id 'net.neoforged.gradle.userdev' version '7.0.+'
}

version = '1.0.0'
group = 'com.thereallemon.llmconversations'

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

minecraft {
    mappings channel: 'official', version: '1.21.1'
    
    runs {
        client {
            workingDirectory project.file('run')
            property 'forge.logging.console.level', 'debug'
            mods {
                llmconversations {
                    source sourceSets.main
                }
            }
        }
        server {
            workingDirectory project.file('run')
            property 'forge.logging.console.level', 'debug'
            mods {
                llmconversations {
                    source sourceSets.main
                }
            }
        }
    }
}

dependencies {
    implementation "net.neoforged:neoforge:21.1.80"
    
    // Minecolonies dependency (adjust version as needed)
    implementation "com.ldtteam:minecolonies:1.21.1-1.1.xxx-ALPHA"
    
    // HTTP client for API calls
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    
    // JSON parsing (Gson already in Minecraft, but explicit for clarity)
    compileOnly 'com.google.code.gson:gson:2.10.1'
}
```

**1.3 neoforge.mods.toml** (⚠️ Note: NeoForge uses `neoforge.mods.toml` instead of `mods.toml`)

```toml
# The name of the mod loader type to load
modLoader="javafml"

# A version range to match for said mod loader
loaderVersion="[4,)"

# The license for your mod
license="MIT"

# A URL to refer people to when problems occur with this mod
issueTrackerURL="https://github.com/thereallemon/llm-conversations/issues"

[[mods]]
modId="llmconversations"
version="${file.jarVersion}"
displayName="LLM Conversations for Minecolonies"
description='''
Adds AI-powered conversations with Minecolonies citizens using LLM APIs.
'''
authors="thereallemon"
displayURL="https://github.com/thereallemon/llm-conversations"
logoFile="logo.png"

[[dependencies.llmconversations]]
    modId="neoforge"
    type="required"
    versionRange="[21.1, )"
    ordering="NONE"
    side="BOTH"

[[dependencies.llmconversations]]
    modId="minecraft"
    type="required"
    versionRange="[1.21, 1.22)"
    ordering="NONE"
    side="BOTH"

[[dependencies.llmconversations]]
    modId="minecolonies"
    type="required"
    versionRange="[1.1,)"
    ordering="AFTER"
    side="BOTH"
```

### Phase 2: Configuration System

**2.1 Configuration Class**

```java
package com.thereallemon.llmconversations.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class LLMConfig {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ClientConfig CLIENT;
    
    static {
        ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
        CLIENT = new ClientConfig(clientBuilder);
        CLIENT_SPEC = clientBuilder.build();
    }
    
    public static class ClientConfig {
        public final ModConfigSpec.ConfigValue<String> apiKey;
        public final ModConfigSpec.ConfigValue<String> model;
        public final ModConfigSpec.ConfigValue<String> systemPrompt;
        public final ModConfigSpec.BooleanValue enableIdleConversations;
        public final ModConfigSpec.DoubleValue idleConversationChance;
        public final ModConfigSpec.IntValue maxConversationHistoryLength;
        public final ModConfigSpec.IntValue maxTokens;
        public final ModConfigSpec.DoubleValue temperature;
        
        public ClientConfig(ModConfigSpec.Builder builder) {
            builder.comment("LLM Conversations Configuration")
                   .push("llm_settings");
            
            apiKey = builder
                .comment("Your OpenRouter API key (get from https://openrouter.ai)")
                .define("api_key", "");
            
            model = builder
                .comment("Model to use for conversations",
                         "Examples: anthropic/claude-3-haiku, openai/gpt-4o-mini")
                .define("model", "anthropic/claude-3-haiku");
            
            systemPrompt = builder
                .comment("System prompt template. Placeholders:",
                         "{name} - Citizen name",
                         "{job} - Citizen job",
                         "{colony_name} - Colony name",
                         "{player_name} - Player name",
                         "{happiness} - Happiness level (0-10)",
                         "{saturation} - Hunger level (0-20)",
                         "{skills} - List of skills")
                .define("system_prompt",
                    "You are {name}, a {job} in the colony of {colony_name}. " +
                    "You are speaking with {player_name}. " +
                    "Your happiness is {happiness}/10. " +
                    "Be friendly, stay in character, and keep responses brief (1-3 sentences). " +
                    "Speak naturally about your work, the colony, and your feelings.");
            
            maxTokens = builder
                .comment("Maximum tokens per response")
                .defineInRange("max_tokens", 150, 50, 500);
            
            temperature = builder
                .comment("Temperature for response generation (0.0-2.0)")
                .defineInRange("temperature", 0.7, 0.0, 2.0);
            
            builder.pop();
            
            builder.comment("Idle Conversation Settings")
                   .push("idle_conversations");
            
            enableIdleConversations = builder
                .comment("Enable citizens to randomly start conversations when idle")
                .define("enable_idle_conversations", false);
            
            idleConversationChance = builder
                .comment("Chance (0.0-1.0) for idle citizen to initiate conversation per check")
                .defineInRange("idle_conversation_chance", 0.01, 0.0, 1.0);
            
            builder.pop();
            
            builder.comment("Memory Settings")
                   .push("memory");
            
            maxConversationHistoryLength = builder
                .comment("Maximum number of past conversation summaries to remember")
                .defineInRange("max_history_length", 5, 0, 20);
            
            builder.pop();
        }
    }
}
```

**2.2 Register Configuration**

```java
package com.thereallemon.llmconversations;

import com.thereallemon.llmconversations.config.LLMConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod("llmconversations")
public class LLMConversationsMod {
    public static final String MOD_ID = "llmconversations";
    
    public LLMConversationsMod(ModContainer container) {
        container.registerConfig(
            ModConfig.Type.CLIENT, 
            LLMConfig.CLIENT_SPEC, 
            "llm-conversations-client.toml"
        );
    }
}
```

### Phase 3: LLM API Integration

**3.1 OpenRouter Client**

```java
package com.thereallemon.llmconversations.llm;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.List;

public class OpenRouterClient {
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient client;
    private final Gson gson;
    
    public OpenRouterClient() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }
    
    /**
     * Send a chat request asynchronously
     */
    public CompletableFuture<String> sendChatRequest(
            String apiKey,
            String model,
            String systemPrompt,
            List<ChatMessage> conversationHistory,
            String userMessage) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject requestBody = buildRequestBody(
                    model, systemPrompt, conversationHistory, userMessage
                );
                
                Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("HTTP-Referer", "https://minecraft-minecolonies-llm")
                    .addHeader("X-Title", "Minecolonies LLM Conversations")
                    .post(RequestBody.create(gson.toJson(requestBody), JSON))
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("API Error: " + response.code() + 
                                             " - " + response.message());
                    }
                    
                    String responseBody = response.body().string();
                    return parseResponse(responseBody);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to get LLM response: " + 
                                         e.getMessage(), e);
            }
        });
    }
    
    private JsonObject buildRequestBody(
            String model,
            String systemPrompt,
            List<ChatMessage> history,
            String userMessage) {
        
        JsonObject request = new JsonObject();
        request.addProperty("model", model);
        
        JsonArray messages = new JsonArray();
        
        // Add system prompt
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);
        messages.add(systemMsg);
        
        // Add conversation history
        for (ChatMessage msg : history) {
            JsonObject historyMsg = new JsonObject();
            historyMsg.addProperty("role", msg.role);
            historyMsg.addProperty("content", msg.content);
            messages.add(historyMsg);
        }
        
        // Add current user message
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);
        
        request.add("messages", messages);
        
        // Add config from settings
        request.addProperty("max_tokens", 
            LLMConfig.CLIENT.maxTokens.get());
        request.addProperty("temperature", 
            LLMConfig.CLIENT.temperature.get());
        
        return request;
    }
    
    private String parseResponse(String responseBody) {
        JsonObject response = gson.fromJson(responseBody, JsonObject.class);
        
        if (response.has("error")) {
            throw new RuntimeException("API returned error: " + 
                response.get("error").getAsJsonObject().get("message").getAsString());
        }
        
        return response
            .getAsJsonArray("choices")
            .get(0).getAsJsonObject()
            .getAsJsonObject("message")
            .get("content").getAsString();
    }
    
    public static class ChatMessage {
        public final String role; // "user" or "assistant"
        public final String content;
        
        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
```

**3.2 Prompt Builder**

```java
package com.thereallemon.llmconversations.llm;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.thereallemon.llmconversations.config.LLMConfig;
import net.minecraft.world.entity.player.Player;

public class PromptBuilder {
    
    public static String buildSystemPrompt(ICitizenData citizen, Player player) {
        String template = LLMConfig.CLIENT.systemPrompt.get();
        IColony colony = citizen.getColony();
        
        // Replace placeholders
        return template
            .replace("{name}", citizen.getName())
            .replace("{job}", getJobName(citizen))
            .replace("{colony_name}", colony.getName())
            .replace("{player_name}", player.getName().getString())
            .replace("{happiness}", String.valueOf(getHappinessLevel(citizen)))
            .replace("{saturation}", String.format("%.1f", citizen.getSaturation()))
            .replace("{skills}", getSkillsSummary(citizen));
    }
    
    private static String getJobName(ICitizenData citizen) {
        if (citizen.getJob() == null) {
            return "unemployed resident";
        }
        return citizen.getJob().getJobRegistryEntry().getTranslationKey();
    }
    
    private static int getHappinessLevel(ICitizenData citizen) {
        double happiness = citizen.getCitizenHappinessHandler().getHappiness();
        return (int) Math.round(happiness * 10);
    }
    
    private static String getSkillsSummary(ICitizenData citizen) {
        // Get top 3 skills
        var skillHandler = citizen.getCitizenSkillHandler();
        StringBuilder skills = new StringBuilder();
        
        // This is simplified - adjust based on actual API
        skills.append("Skills: ");
        // Add actual skill extraction logic here
        
        return skills.toString();
    }
}
```

### Phase 4: Conversation Interaction Handler

**4.1 LLM Interaction Handler**

```java
package com.thereallemon.llmconversations.interaction;

import com.minecolonies.api.colony.ICitizen;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.interactionhandling.AbstractInteractionResponseHandler;
import com.minecolonies.api.colony.interactionhandling.IChatPriority;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.thereallemon.llmconversations.llm.OpenRouterClient;
import com.thereallemon.llmconversations.llm.PromptBuilder;
import com.thereallemon.llmconversations.memory.ConversationMemory;
import com.thereallemon.llmconversations.config.LLMConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LLMConversationHandler extends AbstractInteractionResponseHandler {
    
    private static final String TYPE_ID = "llmconversations:llm_chat";
    
    private final ICitizenData citizenData;
    private final OpenRouterClient llmClient;
    private List<OpenRouterClient.ChatMessage> conversationHistory;
    private String currentResponse = "";
    private UUID conversationId;
    private boolean isWaitingForResponse = false;
    
    /**
     * Constructor for new conversation
     */
    public LLMConversationHandler(ICitizenData citizenData) {
        super(
            Component.literal("..."), // Initial message
            true, // Primary interaction
            ChatPriority.CHITCHAT // Priority
        );
        
        this.citizenData = citizenData;
        this.llmClient = new OpenRouterClient();
        this.conversationHistory = new ArrayList<>();
        this.conversationId = UUID.randomUUID();
        
        // Load past conversation memory
        ConversationMemory memory = ConversationMemory.get(citizenData);
        if (memory != null && !memory.getSummaries().isEmpty()) {
            // Add context from past conversations
            String context = "Previous conversation summary: " + 
                           String.join(" ", memory.getSummaries());
            conversationHistory.add(
                new OpenRouterClient.ChatMessage("system", context)
            );
        }
    }
    
    /**
     * Constructor for loading from NBT
     */
    public LLMConversationHandler(ICitizen citizen) {
        super();
        this.citizenData = (ICitizenData) citizen;
        this.llmClient = new OpenRouterClient();
        this.conversationHistory = new ArrayList<>();
    }
    
    @Override
    public Component getInquiry() {
        if (isWaitingForResponse) {
            return Component.literal("*thinking...*");
        }
        return Component.literal(currentResponse.isEmpty() ? 
            "Hello! How can I help you?" : currentResponse);
    }
    
    @Override
    public List<Component> getPossibleResponses() {
        List<Component> responses = new ArrayList<>();
        
        if (!isWaitingForResponse) {
            // In actual implementation, you'd want to capture free text
            // For now, provide sample responses or use a custom GUI
            responses.add(Component.literal("[Type in chat to respond]"));
            responses.add(Component.literal("Goodbye"));
        }
        
        return responses;
    }
    
    @Override
    public void onServerResponseTriggered(
            int responseId, 
            Player player, 
            ICitizenData data) {
        
        if (responseId == 1) { // Goodbye
            endConversation(player, data);
            return;
        }
        
        // For actual player input, you'd intercept chat messages
        // This is handled in a separate event handler
    }
    
    /**
     * Process player's chat message during conversation
     */
    public void handlePlayerMessage(String message, Player player) {
        if (isWaitingForResponse) {
            return; // Already processing
        }
        
        isWaitingForResponse = true;
        conversationHistory.add(
            new OpenRouterClient.ChatMessage("user", message)
        );
        
        // Check API key
        String apiKey = LLMConfig.CLIENT.apiKey.get();
        if (apiKey.isEmpty()) {
            player.sendSystemMessage(
                Component.literal("§cError: No OpenRouter API key configured!")
            );
            isWaitingForResponse = false;
            return;
        }
        
        // Build system prompt with citizen context
        String systemPrompt = PromptBuilder.buildSystemPrompt(citizenData, player);
        String model = LLMConfig.CLIENT.model.get();
        
        // Make async LLM call
        llmClient.sendChatRequest(
            apiKey,
            model,
            systemPrompt,
            conversationHistory,
            message
        ).thenAccept(response -> {
            // Update on server thread
            citizenData.getColony().getWorld().getServer().execute(() -> {
                currentResponse = response;
                conversationHistory.add(
                    new OpenRouterClient.ChatMessage("assistant", response)
                );
                isWaitingForResponse = false;
                
                // Send response to player via chat
                player.sendSystemMessage(
                    Component.literal("§b" + citizenData.getName() + "§r: " + response)
                );
                
                // Mark colony dirty to save changes
                citizenData.getColony().markDirty();
            });
        }).exceptionally(throwable -> {
            // Handle error on server thread
            citizenData.getColony().getWorld().getServer().execute(() -> {
                isWaitingForResponse = false;
                player.sendSystemMessage(
                    Component.literal("§cError talking to " + 
                        citizenData.getName() + ": " + throwable.getMessage())
                );
            });
            return null;
        });
    }
    
    private void endConversation(Player player, ICitizenData data) {
        // Save conversation summary to memory
        if (!conversationHistory.isEmpty()) {
            String summary = summarizeConversation();
            ConversationMemory memory = ConversationMemory.get(data);
            if (memory != null) {
                memory.addSummary(summary);
            }
        }
        
        // Resume citizen's work
        if (data.getJob() != null) {
            data.getJob().onWakeUp();
        }
        
        player.sendSystemMessage(
            Component.literal("§7Conversation ended with " + data.getName())
        );
    }
    
    private String summarizeConversation() {
        // Simple summary - in production, could use LLM to summarize
        StringBuilder summary = new StringBuilder();
        summary.append("Talked about: ");
        
        // Extract key topics (simplified)
        int messageCount = conversationHistory.size();
        if (messageCount > 0) {
            summary.append(messageCount / 2).append(" topics");
        }
        
        return summary.toString();
    }
    
    @Override
    public boolean isVisible(Level world) {
        return !isWaitingForResponse;
    }
    
    @Override
    public boolean isValid(ICitizenData citizen) {
        return true;
    }
    
    @Override
    public String getType() {
        return TYPE_ID;
    }
    
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = super.serializeNBT();
        nbt.putString("currentResponse", currentResponse);
        nbt.putUUID("conversationId", conversationId);
        nbt.putBoolean("isWaiting", isWaitingForResponse);
        
        // Save conversation history
        CompoundTag historyTag = new CompoundTag();
        for (int i = 0; i < conversationHistory.size(); i++) {
            OpenRouterClient.ChatMessage msg = conversationHistory.get(i);
            CompoundTag msgTag = new CompoundTag();
            msgTag.putString("role", msg.role);
            msgTag.putString("content", msg.content);
            historyTag.put("msg_" + i, msgTag);
        }
        nbt.put("history", historyTag);
        
        return nbt;
    }
    
    @Override
    public void deserializeNBT(@NotNull CompoundTag nbt) {
        super.deserializeNBT(nbt);
        currentResponse = nbt.getString("currentResponse");
        conversationId = nbt.getUUID("conversationId");
        isWaitingForResponse = nbt.getBoolean("isWaiting");
        
        // Load conversation history
        conversationHistory.clear();
        CompoundTag historyTag = nbt.getCompound("history");
        for (String key : historyTag.getAllKeys()) {
            if (key.startsWith("msg_")) {
                CompoundTag msgTag = historyTag.getCompound(key);
                conversationHistory.add(new OpenRouterClient.ChatMessage(
                    msgTag.getString("role"),
                    msgTag.getString("content")
                ));
            }
        }
    }
}
```

### Phase 5: Conversation Memory System

**5.1 Conversation Memory Storage**

```java
package com.thereallemon.llmconversations.memory;

import com.minecolonies.api.colony.ICitizenData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public class ConversationMemory {
    private static final String NBT_KEY = "llmconversations_memory";
    private static final String NBT_SUMMARIES = "summaries";
    private static final int MAX_SUMMARIES = 5;
    
    private final List<String> conversationSummaries;
    
    public ConversationMemory() {
        this.conversationSummaries = new ArrayList<>();
    }
    
    public void addSummary(String summary) {
        conversationSummaries.add(0, summary); // Add to front
        
        // Keep only recent summaries
        while (conversationSummaries.size() > MAX_SUMMARIES) {
            conversationSummaries.remove(conversationSummaries.size() - 1);
        }
    }
    
    public List<String> getSummaries() {
        return new ArrayList<>(conversationSummaries);
    }
    
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        ListTag summariesList = new ListTag();
        
        for (String summary : conversationSummaries) {
            summariesList.add(StringTag.valueOf(summary));
        }
        
        nbt.put(NBT_SUMMARIES, summariesList);
        return nbt;
    }
    
    public void deserializeNBT(CompoundTag nbt) {
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
     */
    public static ConversationMemory get(ICitizenData citizen) {
        // Store in citizen's additional data
        // This requires accessing citizen's NBT storage
        // Implement based on Minecolonies' data storage patterns
        
        ConversationMemory memory = new ConversationMemory();
        // Load from citizen's NBT if exists
        
        return memory;
    }
    
    /**
     * Save memory to citizen's data
     */
    public static void save(ICitizenData citizen, ConversationMemory memory) {
        // Save to citizen's NBT storage
        // Mark citizen as dirty so it gets saved
        citizen.markDirty();
    }
}
```

### Phase 6: Event Handlers

**6.1 Conversation Initiation Handler**

```java
package com.thereallemon.llmconversations.events;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.thereallemon.llmconversations.interaction.LLMConversationHandler;
import com.thereallemon.llmconversations.state.ConversationStateManager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public class ConversationInitiationHandler {
    
    @SubscribeEvent
    public static void onPlayerInteractWithCitizen(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof EntityCitizen)) {
            return;
        }
        
        EntityCitizen citizen = (EntityCitizen) event.getTarget();
        ICitizenData citizenData = citizen.getCitizenData();
        Player player = event.getEntity();
        
        if (citizenData == null || player.level.isClientSide) {
            return;
        }
        
        // Check if player is sneaking (shift + right-click) to start conversation
        if (player.isCrouching()) {
            event.setCanceled(true);
            startConversation(player, citizenData);
        }
    }
    
    private static void startConversation(Player player, ICitizenData citizenData) {
        // Check if already in conversation
        if (ConversationStateManager.isInConversation(citizenData.getID())) {
            player.sendSystemMessage(Component.literal(
                "§e" + citizenData.getName() + " is already in a conversation!"
            ));
            return;
        }
        
        // Pause citizen's current task
        if (citizenData.getJob() != null && 
            !citizenData.getJob().canAIBeInterrupted()) {
            player.sendSystemMessage(Component.literal(
                "§e" + citizenData.getName() + " is too busy right now!"
            ));
            return;
        }
        
        // Mark as in conversation
        ConversationStateManager.startConversation(
            citizenData.getID(), 
            player.getUUID()
        );
        
        // Create and trigger LLM interaction
        LLMConversationHandler handler = new LLMConversationHandler(citizenData);
        citizenData.triggerInteraction(handler);
        
        // Freeze citizen stats
        ConversationStateManager.freezeStats(citizenData);
        
        player.sendSystemMessage(Component.literal(
            "§7Started conversation with " + citizenData.getName() + 
            ". Type in chat to talk, or say 'goodbye' to end."
        ));
    }
}
```

**6.2 Chat Message Interceptor**

```java
package com.thereallemon.llmconversations.events;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColonyManager;
import com.thereallemon.llmconversations.interaction.LLMConversationHandler;
import com.thereallemon.llmconversations.state.ConversationStateManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.UUID;

@EventBusSubscriber
public class ChatInterceptHandler {
    
    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        UUID playerUUID = player.getUUID();
        
        // Check if player is in a conversation
        UUID citizenId = ConversationStateManager.getConversationPartner(playerUUID);
        if (citizenId == null) {
            return; // Not in conversation
        }
        
        // Cancel normal chat
        event.setCanceled(true);
        
        // Get citizen data
        ICitizenData citizenData = findCitizen(player, citizenId);
        if (citizenData == null) {
            ConversationStateManager.endConversation(citizenId);
            return;
        }
        
        String message = event.getMessage().getString();
        
        // Check for exit commands
        if (message.equalsIgnoreCase("goodbye") || 
            message.equalsIgnoreCase("bye") ||
            message.equalsIgnoreCase("exit")) {
            endConversation(player, citizenData);
            return;
        }
        
        // Forward to LLM handler
        LLMConversationHandler handler = ConversationStateManager.getHandler(citizenId);
        if (handler != null) {
            handler.handlePlayerMessage(message, player);
        }
    }
    
    private static ICitizenData findCitizen(ServerPlayer player, UUID citizenId) {
        // Search through colonies to find citizen
        return IColonyManager.getInstance()
            .getAllColonies()
            .stream()
            .flatMap(colony -> colony.getCitizenManager().getCitizens().stream())
            .filter(citizen -> citizen.getID().equals(citizenId))
            .findFirst()
            .orElse(null);
    }
    
    private static void endConversation(ServerPlayer player, ICitizenData citizenData) {
        ConversationStateManager.endConversation(citizenData.getID());
        ConversationStateManager.unfreezeStats(citizenData);
        
        // Resume work
        if (citizenData.getJob() != null) {
            citizenData.getJob().onWakeUp();
        }
        
        player.sendSystemMessage(Component.literal(
            "§7Ended conversation with " + citizenData.getName()
        ));
    }
}
```

### Phase 7: Conversation State Manager

**7.1 State Management**

```java
package com.thereallemon.llmconversations.state;

import com.minecolonies.api.colony.ICitizenData;
import com.thereallemon.llmconversations.interaction.LLMConversationHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConversationStateManager {
    
    // citizenId -> playerUUID
    private static final Map<UUID, UUID> activeConversations = new HashMap<>();
    
    // citizenId -> handler
    private static final Map<UUID, LLMConversationHandler> handlers = new HashMap<>();
    
    // citizenId -> frozen saturation
    private static final Map<UUID, Double> frozenSaturation = new HashMap<>();
    
    public static void startConversation(UUID citizenId, UUID playerUUID) {
        activeConversations.put(citizenId, playerUUID);
    }
    
    public static void endConversation(UUID citizenId) {
        activeConversations.remove(citizenId);
        handlers.remove(citizenId);
        frozenSaturation.remove(citizenId);
    }
    
    public static boolean isInConversation(UUID citizenId) {
        return activeConversations.containsKey(citizenId);
    }
    
    public static UUID getConversationPartner(UUID playerUUID) {
        return activeConversations.entrySet().stream()
            .filter(e -> e.getValue().equals(playerUUID))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }
    
    public static void registerHandler(UUID citizenId, LLMConversationHandler handler) {
        handlers.put(citizenId, handler);
    }
    
    public static LLMConversationHandler getHandler(UUID citizenId) {
        return handlers.get(citizenId);
    }
    
    public static void freezeStats(ICitizenData citizen) {
        frozenSaturation.put(citizen.getID(), citizen.getSaturation());
    }
    
    public static void unfreezeStats(ICitizenData citizen) {
        frozenSaturation.remove(citizen.getID());
    }
    
    /**
     * Called on tick to maintain frozen stats
     */
    public static void maintainFrozenStats(ICitizenData citizen) {
        if (isInConversation(citizen.getID())) {
            Double frozenValue = frozenSaturation.get(citizen.getID());
            if (frozenValue != null) {
                citizen.setSaturation(frozenValue);
            }
        }
    }
}
```

**7.2 Stat Maintenance Ticker**

```java
package com.thereallemon.llmconversations.events;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColonyManager;
import com.thereallemon.llmconversations.state.ConversationStateManager;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public class StatMaintenanceHandler {
    
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20; // Once per second
    
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Note: In NeoForge 1.21+, ServerTickEvent is split into Pre and Post
        // We use Post which is equivalent to the old Phase.END
        
        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) {
            return;
        }
        tickCounter = 0;
        
        // Maintain frozen stats for all citizens in conversations
        IColonyManager.getInstance().getAllColonies().forEach(colony -> {
            colony.getCitizenManager().getCitizens().forEach(citizen -> {
                ConversationStateManager.maintainFrozenStats(citizen);
            });
        });
    }
}
```

## Critical Implementation Notes

### 1. Thread Safety

**⚠️ IMPORTANT:** Minecraft is NOT thread-safe!

- LLM API calls are async (run on separate thread)
- **All game modifications MUST happen on the server thread**
- Use `server.execute(() -> { ... })` to run code on server thread
- Never modify citizen data, colony data, or world state from async threads

Example:
```java
llmClient.sendChatRequest(...).thenAccept(response -> {
    // This runs on async thread - DON'T modify game state here!
    
    // Schedule on server thread:
    server.execute(() -> {
        // NOW it's safe to modify citizen data
        citizenData.setSomething(response);
    });
});
```

### 2. Error Handling

**Must handle:**
- API key not configured
- API rate limits
- Network failures
- Invalid responses
- Model not available
- Timeout scenarios

Always provide user feedback:
```java
.exceptionally(throwable -> {
    player.sendSystemMessage(
        Component.literal("§cError: " + throwable.getMessage())
    );
    return null;
});
```

### 3. Performance Considerations

- LLM calls take 1-5 seconds typically
- Show "thinking..." indicator to player
- Don't block the main thread
- Limit conversation history length
- Consider caching frequent responses
- Set reasonable timeout (30 seconds)

### 4. API Cost Management

OpenRouter charges per token:
- Set `max_tokens` limit (150-200 is good)
- Keep conversation history short
- Truncate old messages
- Warn users about API costs in config
- Consider rate limiting per player

### 5. NBT Serialization

All persistent data must be NBT-serializable:
- Conversation history
- Memory summaries
- Conversation state
- Frozen stat values

Test save/load cycle thoroughly!

### 6. Client-Server Sync

- Configuration is client-side (each player has own API key)
- Conversation state is server-side
- Chat messages go through server
- Use network packets for custom GUIs (if needed)

## Optional Features Implementation

### Idle Conversation Initiation

Add to visitor/idle AI:

```java
@SubscribeEvent
public static void onCitizenIdle(TickEvent.ServerTickEvent event) {
    if (!LLMConfig.CLIENT.enableIdleConversations.get()) {
        return;
    }
    
    // Check randomly (every 5 seconds)
    if (event.phase == TickEvent.Phase.END && 
        new Random().nextDouble() < LLMConfig.CLIENT.idleConversationChance.get()) {
        
        // Find idle citizens near players
        for (IColony colony : IColonyManager.getInstance().getAllColonies()) {
            for (ICitizenData citizen : colony.getCitizenManager().getCitizens()) {
                if (isIdleAndNearPlayer(citizen)) {
                    initiateIdleConversation(citizen);
                }
            }
        }
    }
}

private static boolean isIdleAndNearPlayer(ICitizenData citizen) {
    // Check if citizen has no job or is not working
    // Check if player is within range
    // Check if not already in conversation
    return false; // Implement logic
}
```

## Testing Checklist

- [ ] Configuration saves and loads correctly
- [ ] API key validation works
- [ ] Conversations start via shift+right-click
- [ ] Chat messages are intercepted during conversation
- [ ] LLM responses appear in chat
- [ ] Citizens pause work during conversation
- [ ] Stats don't decrease during conversation
- [ ] Conversation ends properly with "goodbye"
- [ ] Memory persists across sessions
- [ ] Works with multiple simultaneous conversations
- [ ] Error messages display for API failures
- [ ] Config GUI is accessible and functional
- [ ] Compatible with different Minecolonies versions
- [ ] No crashes on server restart during conversation
- [ ] Proper cleanup when citizen dies/despawns

## Common Issues & Solutions

### Issue: "Cannot find minecolonies classes"

**Solution:** Ensure Minecolonies is in dependencies and deobfuscated:
```gradle
implementation fg.deobf("com.ldtteam:minecolonies:VERSION")
```

### Issue: "Conversation state lost on restart"

**Solution:** Implement proper NBT serialization in handler and save to citizen data.

### Issue: "API calls block the server"

**Solution:** Always use `CompletableFuture` and async execution. Never call `.get()` or `.join()` on main thread.

### Issue: "Chat messages still appear in global chat"

**Solution:** Make sure to cancel the ServerChatEvent: `event.setCanceled(true)`

### Issue: "Stats still decrease during conversation"

**Solution:** Implement ticker that restores frozen values every tick, not just once.

## Future Enhancements

1. **Voice Generation**: Use TTS API for voice responses
2. **Multi-language Support**: Detect player language, translate prompts
3. **Visual Dialogue UI**: Custom GUI instead of chat
4. **Conversation Branching**: Structured dialogue trees with LLM
5. **Group Conversations**: Multiple citizens in one conversation
6. **Quest Integration**: LLM generates dynamic quests
7. **Emotion System**: Visual indicators for citizen emotions
8. **Learning System**: Citizens remember preferences
9. **Colony Events**: Citizens comment on colony happenings
10. **Voice Input**: Speech-to-text for player input

## Resources

- **Minecolonies Source**: https://github.com/ldtteam/minecolonies (branch: version/1.21)
- **NeoForge Documentation**: https://docs.neoforged.net/ (⚠️ Use this instead of old Forge docs)
- **NeoForge Migration Guide**: https://docs.neoforged.net/docs/gettingstarted/migrating/
- **OpenRouter API**: https://openrouter.ai/docs
- **NeoForge Discord**: For help and questions
- **BlockUI Library**: For custom GUIs (used by Minecolonies)

## License Considerations

- Your mod: Choose your own license (MIT recommended)
- Minecolonies: Check their license (likely MIT)
- OpenRouter: Review their Terms of Service
- Include attribution for all dependencies
- Warn users about API costs and data transmission

## Final Notes

This is a complex mod that touches many systems:
- Minecraft's entity system
- Forge's event system
- Minecolonies' interaction system
- Async HTTP calls
- NBT data persistence
- Client-server networking

**Start small:**
1. Get basic conversation working first
2. Add stat freezing
3. Add memory system
4. Polish UI/UX
5. Add optional features

**Test thoroughly at each step!**

Good luck building your AI conversation mod! 🎮🤖
