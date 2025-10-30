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
        public final ModConfigSpec.BooleanValue debugMode;
        public final ModConfigSpec.ConfigValue<String> exitWords;
        
        public ClientConfig(ModConfigSpec.Builder builder) {
            builder.comment("LLM Conversations Configuration")
                   .push("llm_settings");
            
            apiKey = builder
                .comment("Your OpenRouter API key (get from https://openrouter.ai)")
                .translation("llmconversations.config.api_key")
                .define("api_key", "");
            
            model = builder
                .comment("Model to use for conversations",
                         "Examples: anthropic/claude-3-haiku, openai/gpt-4o-mini, meta-llama/llama-3.1-8b-instruct")
                .translation("llmconversations.config.model")
                .define("model", "anthropic/claude-3-haiku");
            
            systemPrompt = builder
                .comment("System prompt template. Placeholders:",
                         "{name} - Citizen name",
                         "{job} - Citizen job",
                         "{colony_name} - Colony name",
                         "{player_name} - Player name",
                         "{happiness} - Happiness level (0-10)",
                         "{saturation} - Hunger level (0-20)",
                         "{skills} - List of skills",
                         "Note: Recent colony events are automatically added to give context")
                .translation("llmconversations.config.system_prompt")
                .define("system_prompt",
                    "You are {name}, a {job} in the colony of {colony_name}. " +
                    "You are speaking with {player_name}. " +
                    "Your happiness is {happiness}/10. " +
                    "Be friendly, stay in character, and keep responses brief (1-3 sentences). " +
                    "Speak naturally about your work, the colony, your feelings, and recent events. " +
                    "You're aware of births, deaths, and construction happening in the colony.");
            
            maxTokens = builder
                .comment("Maximum tokens per response")
                .translation("llmconversations.config.max_tokens")
                .defineInRange("max_tokens", 150, 50, 500);
            
            temperature = builder
                .comment("Temperature for response generation (0.0-2.0)",
                         "Higher values make output more random, lower values more focused")
                .translation("llmconversations.config.temperature")
                .defineInRange("temperature", 0.7, 0.0, 2.0);
            
            builder.pop();
            
            builder.comment("Idle Conversation Settings (not working yet)")
                   .push("idle_conversations");
            
            enableIdleConversations = builder
                .comment("Enable citizens to randomly start conversations when idle (not implemented yet)")
                .translation("llmconversations.config.enable_idle_conversations")
                .define("enable_idle_conversations", false);
            
            idleConversationChance = builder
                .comment("Chance (0.0-1.0) for idle citizen to initiate conversation per check (not implemented yet)")
                .translation("llmconversations.config.idle_conversation_chance")
                .defineInRange("idle_conversation_chance", 0.01, 0.0, 1.0);
            
            builder.pop();
            
            builder.comment("Memory Settings")
                   .push("memory");
            
            maxConversationHistoryLength = builder
                .comment("Maximum number of past conversation summaries to remember")
                .translation("llmconversations.config.max_history_length")
                .defineInRange("max_history_length", 5, 0, 20);
            
            builder.pop();
            
            builder.comment("Conversation Control Settings")
                   .push("conversation_control");
            
            exitWords = builder
                .comment("Words that end a conversation (comma-separated, case-insensitive)",
                         "Examples: goodbye,bye,cya,exit,stop,later,farewell")
                .translation("llmconversations.config.exit_words")
                .define("exit_words", "goodbye,bye,cya,exit,stop,later");
            
            builder.pop();
            
            builder.comment("Debug Settings")
                   .push("debug");
            
            debugMode = builder
                .comment("Enable debug logging for LLM conversations",
                         "Logs prompts, API calls, responses, summaries, and internal state changes")
                .translation("llmconversations.config.debug_mode")
                .define("debug_mode", false);
            
            builder.pop();
        }
        
        /**
         * Get the list of exit words as a String array
         */
        public static String[] getExitWords() {
            String words = CLIENT.exitWords.get();
            if (words == null || words.trim().isEmpty()) {
                return new String[]{"goodbye"}; // Fallback
            }
            return words.toLowerCase().split("\\s*,\\s*");
        }
    }
}
