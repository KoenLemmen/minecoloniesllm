package com.thereallemon.llmconversations.network;

import com.thereallemon.llmconversations.client.ClientConversationState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet sent from server to client to update conversation state
 */
public record SyncConversationStatePacket(int citizenId, boolean inConversation) implements CustomPacketPayload {

    public static final Type<SyncConversationStatePacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("llmconversations", "sync_conversation_state"));

    public static final StreamCodec<ByteBuf, SyncConversationStatePacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.INT,
            SyncConversationStatePacket::citizenId,
            ByteBufCodecs.BOOL,
            SyncConversationStatePacket::inConversation,
            SyncConversationStatePacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncConversationStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.inConversation()) {
                // Start conversation on client
                ClientConversationState.setConversationPartner(packet.citizenId());
            } else {
                // End conversation on client
                ClientConversationState.clearConversation();
            }
        });
    }
}

