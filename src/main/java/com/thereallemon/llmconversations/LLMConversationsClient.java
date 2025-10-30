package com.thereallemon.llmconversations;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Client-side mod class for LLM Conversations
 * Handles client-only initialization like the configuration screen
 */
@Mod(value = LLMConversationsMod.MOD_ID, dist = Dist.CLIENT)
public class LLMConversationsClient {
    private static final Logger LOGGER = LogManager.getLogger();

    public LLMConversationsClient(ModContainer container) {
        LOGGER.info("Initializing LLM Conversations Client");

        // Register the configuration screen
        // This will use NeoForge's built-in ConfigurationScreen to display this mod's configs
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        LOGGER.info("LLM Conversations Client initialized with config screen");
    }
}

