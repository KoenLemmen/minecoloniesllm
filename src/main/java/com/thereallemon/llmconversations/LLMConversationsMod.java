package com.thereallemon.llmconversations;

import com.thereallemon.llmconversations.config.LLMConfig;
import com.thereallemon.llmconversations.network.EndConversationPacket;
import com.thereallemon.llmconversations.network.StartConversationPacket;
import com.thereallemon.llmconversations.network.SyncConversationStatePacket;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("llmconversations")
public class LLMConversationsMod {
    public static final String MOD_ID = "llmconversations";
    public static final Logger LOGGER = LogManager.getLogger();

    public LLMConversationsMod(ModContainer container, IEventBus modEventBus) {
        LOGGER.info("Initializing LLM Conversations for Minecolonies");

        // Register configuration
        container.registerConfig(
            ModConfig.Type.CLIENT,
            LLMConfig.CLIENT_SPEC,
            "llm-conversations-client.toml"
        );

        // Register network packets
        modEventBus.addListener(this::registerNetworkPackets);

        LOGGER.info("LLM Conversations mod initialized successfully");
    }

    private void registerNetworkPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // Client to Server packets
        registrar.playToServer(
            StartConversationPacket.TYPE,
            StartConversationPacket.STREAM_CODEC,
            StartConversationPacket::handle
        );

        registrar.playToServer(
            EndConversationPacket.TYPE,
            EndConversationPacket.STREAM_CODEC,
            EndConversationPacket::handle
        );

        // Server to Client packets
        registrar.playToClient(
            SyncConversationStatePacket.TYPE,
            SyncConversationStatePacket.STREAM_CODEC,
            SyncConversationStatePacket::handle
        );

        LOGGER.info("Network packets registered");
    }
}

