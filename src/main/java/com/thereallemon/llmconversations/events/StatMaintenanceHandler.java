package com.thereallemon.llmconversations.events;

import com.minecolonies.api.colony.IColonyManager;
import com.thereallemon.llmconversations.state.ConversationStateManager;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Maintains frozen stats for citizens in conversations
 */
@EventBusSubscriber(modid = "llmconversations")
public class StatMaintenanceHandler {
    
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20; // Once per second
    
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // In NeoForge 1.21+, ServerTickEvent is split into Pre and Post
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
