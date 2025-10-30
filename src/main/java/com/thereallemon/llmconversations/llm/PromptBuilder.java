package com.thereallemon.llmconversations.llm;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.colonyEvents.descriptions.IColonyEventDescription;
import com.minecolonies.api.colony.colonyEvents.descriptions.ICitizenEventDescription;
import com.minecolonies.api.colony.colonyEvents.descriptions.IBuildingEventDescription;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenHappinessHandler;
import com.thereallemon.llmconversations.config.LLMConfig;
import com.thereallemon.llmconversations.memory.ConversationMemory;
import com.thereallemon.llmconversations.util.DebugLogger;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Builds context-aware system prompts for LLM conversations
 */
public class PromptBuilder {
    
    /**
     * Build a system prompt with citizen and player context
     * @param citizen The citizen being conversed with
     * @param player The player conversing
     * @return Formatted system prompt with all placeholders replaced
     */
    public static String buildSystemPrompt(ICitizenData citizen, Player player) {
        String template = LLMConfig.CLIENT.systemPrompt.get();
        IColony colony = citizen.getColony();
        
        // Build base prompt with placeholders replaced
        String basePrompt = template
            .replace("{name}", citizen.getName())
            .replace("{job}", getJobName(citizen))
            .replace("{colony_name}", colony.getName())
            .replace("{player_name}", player.getName().getString())
            .replace("{happiness}", String.valueOf(getHappinessLevel(citizen)))
            .replace("{saturation}", String.format("%.1f", citizen.getSaturation()))
            .replace("{skills}", getSkillsSummary(citizen));
        
        // Add conversation memories for context continuity
        String memoryContext = buildMemoryContext(citizen, player);

        // Add recent colony events for situational awareness
        String eventsContext = buildColonyEventsContext(citizen);
        
        String finalPrompt = basePrompt + memoryContext + eventsContext;

        DebugLogger.debugSection("Generated System Prompt");
        DebugLogger.debug("Citizen: {} ({})", citizen.getName(), getJobName(citizen));
        DebugLogger.debug("Colony: {}", colony.getName());
        DebugLogger.debug("Happiness: {}", getHappinessLevel(citizen));
        DebugLogger.debug("Saturation: {}", citizen.getSaturation());
        DebugLogger.debug("Full Prompt:\n{}", finalPrompt);
        DebugLogger.debugSeparator();
        
        return finalPrompt;
    }
    
    /**
     * Build memory context from past conversations with the player
     * This allows the NPC to remember previous interactions and maintain continuity
     *
     * @param citizen The citizen to get memories for
     * @param player The player having the conversation
     * @return Formatted string with past conversation memories, or empty string if no memories
     */
    private static String buildMemoryContext(ICitizenData citizen, Player player) {
        ConversationMemory memory = ConversationMemory.get(citizen);

        if (memory == null || memory.getSummaries().isEmpty()) {
            return "";
        }

        List<String> summaries = memory.getSummaries();

        // Get the most recent memories (last 5-10 summaries)
        int maxMemories = LLMConfig.CLIENT.maxConversationHistoryLength.get();
        int startIndex = Math.max(0, summaries.size() - maxMemories);

        StringBuilder context = new StringBuilder("\n\nYour memories of past conversations with ");
        context.append(player.getName().getString()).append(":\n");

        for (int i = startIndex; i < summaries.size(); i++) {
            context.append("- ").append(summaries.get(i)).append("\n");
        }

        context.append("\nUse these memories to maintain continuity in your conversation. ");
        context.append("If the player asks you something you should know from a previous conversation, refer to your memories.");

        DebugLogger.debug("Added {} memories to system prompt", summaries.size());

        return context.toString();
    }

    /**
     * Get the job name or "unemployed resident" if jobless
     */
    private static String getJobName(ICitizenData citizen) {
        if (citizen.getJob() == null) {
            return "unemployed resident";
        }
        // Get the translation key and extract the job name
        String translationKey = citizen.getJob().getJobRegistryEntry().getTranslationKey();
        // Extract the job name from translation key (e.g., "job.minecolonies.builder" -> "builder")
        String[] parts = translationKey.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : "worker";
    }
    
    /**
     * Get happiness level as a 0-10 integer
     */
    private static int getHappinessLevel(ICitizenData citizen) {
        ICitizenHappinessHandler happinessHandler = citizen.getCitizenHappinessHandler();
        double happiness = happinessHandler.getHappiness(citizen.getColony(), citizen);
        // Happiness is typically 0-10 range
        return (int) Math.round(happiness);
    }
    
    /**
     * Get a summary of the citizen's top skills
     */
    private static String getSkillsSummary(ICitizenData citizen) {
        // Get skill handler
        var skillHandler = citizen.getCitizenSkillHandler();
        
        StringBuilder skills = new StringBuilder();
        skills.append("My skills: ");
        
        // Note: Actual implementation depends on Minecolonies API
        // This is a placeholder that should be updated based on the actual API
        try {
            // Example: Get primary and secondary skills if available
            // skillHandler.getPrimarySkill(), skillHandler.getSecondarySkill(), etc.
            skills.append("various abilities");
        } catch (Exception e) {
            skills.append("still learning");
        }
        
        return skills.toString();
    }
    
    /**
     * Create an initial greeting message based on citizen context
     */
    public static String buildInitialGreeting(ICitizenData citizen, Player player) {
        String greeting = "Hello " + player.getName().getString() + "! ";
        
        if (citizen.getJob() != null) {
            greeting += "I'm busy with my work as a " + getJobName(citizen) + ". ";
        } else {
            greeting += "I'm looking for work. ";
        }
        
        greeting += "What can I help you with?";
        
        return greeting;
    }
    
    /**
     * Build recent colony events context for the LLM
     * This gives the NPC awareness of recent happenings in the colony,
     * making conversations feel more alive and situationally aware.
     * 
     * @param citizen The citizen to get events for
     * @return Formatted string with recent colony events, or empty string if no events
     */
    public static String buildColonyEventsContext(ICitizenData citizen) {
        IColony colony = citizen.getColony();
        
        try {
            List<IColonyEventDescription> events = colony.getEventDescriptionManager().getEventDescriptions();
            
            if (events == null || events.isEmpty()) {
                return "";
            }
            
            // Get the last 5-10 most recent events (configurable)
            int maxEvents = 8;
            int eventCount = Math.min(maxEvents, events.size());
            int startIndex = Math.max(0, events.size() - eventCount);
            
            StringBuilder context = new StringBuilder("\n\nRecent colony events you're aware of:\n");
            
            for (int i = startIndex; i < events.size(); i++) {
                IColonyEventDescription event = events.get(i);
                
                // Format event into readable string based on type
                if (event instanceof ICitizenEventDescription) {
                    ICitizenEventDescription citizenEvent = (ICitizenEventDescription) event;
                    String eventName = citizenEvent.getName();
                    String citizenName = citizenEvent.getCitizenName();
                    
                    // Make event descriptions more natural
                    if (eventName.toLowerCase().contains("born")) {
                        context.append("- ").append(citizenName).append(" was born in the colony\n");
                    } else if (eventName.toLowerCase().contains("died")) {
                        context.append("- ").append(citizenName).append(" passed away\n");
                    } else if (eventName.toLowerCase().contains("spawn")) {
                        context.append("- ").append(citizenName).append(" joined the colony\n");
                    } else if (eventName.toLowerCase().contains("grown")) {
                        context.append("- ").append(citizenName).append(" grew up\n");
                    } else {
                        context.append("- ").append(eventName).append(": ").append(citizenName).append("\n");
                    }
                    
                } else if (event instanceof IBuildingEventDescription) {
                    IBuildingEventDescription buildingEvent = (IBuildingEventDescription) event;
                    String eventName = buildingEvent.getName();
                    String buildingName = buildingEvent.getBuildingName();
                    int level = buildingEvent.getLevel();
                    
                    // Make building event descriptions more natural
                    if (eventName.toLowerCase().contains("built")) {
                        context.append("- A new ").append(buildingName).append(" was built\n");
                    } else if (eventName.toLowerCase().contains("upgrade")) {
                        context.append("- The ").append(buildingName).append(" was upgraded to level ").append(level).append("\n");
                    } else if (eventName.toLowerCase().contains("repair")) {
                        context.append("- The ").append(buildingName).append(" was repaired\n");
                    } else if (eventName.toLowerCase().contains("deconstructed") || eventName.toLowerCase().contains("removed")) {
                        context.append("- The ").append(buildingName).append(" was removed\n");
                    } else {
                        context.append("- ").append(eventName).append(": ").append(buildingName).append(" (level ").append(level).append(")\n");
                    }
                    
                } else {
                    // Generic event
                    context.append("- ").append(event.getName()).append("\n");
                }
            }
            
            context.append("\nYou can naturally mention these events in conversation if relevant.");
            
            return context.toString();
            
        } catch (Exception e) {
            // If event fetching fails, just return empty string
            // Don't want to crash conversation over missing events
            return "";
        }
    }
}
