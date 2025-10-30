package com.thereallemon.llmconversations.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = "llmconversations", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class KeyBindings {

    public static final String KEY_CATEGORY = "key.categories.llmconversations";

    // Key mapping for starting conversations
    public static KeyMapping START_CONVERSATION;

    /**
     * Register key bindings
     */
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        START_CONVERSATION = new KeyMapping(
            "key.llmconversations.start_conversation",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,  // Default: V key
            KEY_CATEGORY
        );

        event.register(START_CONVERSATION);
    }
}
