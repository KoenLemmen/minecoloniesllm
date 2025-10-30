package com.thereallemon.llmconversations.network;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.thereallemon.llmconversations.state.ConversationStateManager;
import com.thereallemon.llmconversations.util.DebugLogger;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StartConversationPacket(int entityId) implements CustomPacketPayload {

    public static final Type<StartConversationPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("llmconversations", "start_conversation"));

    public static final StreamCodec<ByteBuf, StartConversationPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.INT,
            StartConversationPacket::entityId,
            StartConversationPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StartConversationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                Entity entity = serverPlayer.level().getEntity(packet.entityId());

                if (entity instanceof AbstractEntityCitizen citizen) {
                    DebugLogger.log("Server: Received start conversation request for citizen " + citizen.getId());

                    // Check if player is already in a conversation
                    if (ConversationStateManager.isInConversation(serverPlayer)) {
                        serverPlayer.sendSystemMessage(
                            Component.translatable("llmconversations.message.already_in_conversation")
                        );
                        return;
                    }

                    // Check distance (within 10 blocks)
                    double distance = serverPlayer.distanceToSqr(citizen);
                    if (distance > 100) { // 10 blocks squared
                        serverPlayer.sendSystemMessage(
                            Component.translatable("llmconversations.message.too_far_away")
                        );
                        return;
                    }

                    // Start the conversation
                    ConversationStateManager.startConversation(serverPlayer, citizen);
                    serverPlayer.sendSystemMessage(
                        Component.translatable("llmconversations.message.conversation_started",
                            citizen.getName().getString())
                    );

                    // Notify client about conversation state
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                        serverPlayer,
                        new SyncConversationStatePacket(citizen.getCitizenData().getId(), true)
                    );

                    DebugLogger.log("Started conversation between " + serverPlayer.getName().getString() +
                                  " and " + citizen.getName().getString());
                }
            }
        });
    }
}

