package com.thereallemon.llmconversations.network;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColonyManager;
import com.thereallemon.llmconversations.interaction.LLMConversationHandler;
import com.thereallemon.llmconversations.state.ConversationStateManager;
import com.thereallemon.llmconversations.util.DebugLogger;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EndConversationPacket() implements CustomPacketPayload {

    public static final Type<EndConversationPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("llmconversations", "end_conversation"));

    public static final StreamCodec<ByteBuf, EndConversationPacket> STREAM_CODEC =
        StreamCodec.unit(new EndConversationPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EndConversationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                // Find which citizen the player is talking to
                Integer citizenId = ConversationStateManager.getConversationPartner(serverPlayer.getUUID());

                if (citizenId == null) {
                    serverPlayer.sendSystemMessage(Component.translatable("llmconversations.message.not_in_conversation"));
                    return;
                }

                // Find the citizen data
                ICitizenData citizenData = IColonyManager.getInstance()
                    .getAllColonies()
                    .stream()
                    .flatMap(colony -> colony.getCitizenManager().getCitizens().stream())
                    .filter(citizen -> citizen.getId() == citizenId)
                    .findFirst()
                    .orElse(null);

                if (citizenData == null) {
                    ConversationStateManager.endConversation(citizenId);
                    serverPlayer.sendSystemMessage(Component.translatable("llmconversations.message.citizen_left"));
                    return;
                }

                // End the conversation properly
                LLMConversationHandler handler = ConversationStateManager.getHandler(citizenId);
                if (handler != null) {
                    DebugLogger.log("Ending conversation between " + serverPlayer.getName().getString() +
                                  " and " + citizenData.getName());
                    handler.endConversation(serverPlayer);
                } else {
                    // Fallback cleanup
                    ConversationStateManager.unfreezeStats(citizenData);
                    ConversationStateManager.endConversation(citizenId);

                    if (citizenData.getJob() != null) {
                        citizenData.getJob().onWakeUp();
                        citizenData.getJob().resetAI();
                    }

                    serverPlayer.sendSystemMessage(Component.translatable(
                        "llmconversations.message.conversation_ended", citizenData.getName()
                    ));
                }

                // Notify client that conversation has ended
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new SyncConversationStatePacket(citizenId, false)
                );
            }
        });
    }
}

