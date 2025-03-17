package com.example.gofish.handlers;

import com.example.gofish.config.GoFishConfig;
import com.example.gofish.utils.FishingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

/**
 * Handler for auto-catching fish when notified by the PacketHandler
 * This is a simplified version that relies on packet detection instead of motion detection
 */
public class FishingHandler {
    
    // Debug mode for development
    private static final boolean DEBUG_MODE = true;
    
    // Auto-catch state
    private boolean scheduledCatch = false;
    private long scheduledCatchTime = 0;
    private final Random random = new Random();
    
    // Right-click simulation state
    private boolean isRightClicking = false;
    private int rightClickDuration = 0;
    private static final int RIGHT_CLICK_TICKS = 1; // Hold right-click for this many ticks
    
    /**
     * Helper method to send debug messages to chat
     */
    private void sendDebugMessage(Minecraft mc, String message) {
        if (DEBUG_MODE && mc != null && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText("§7[GoFish Debug] §f" + message));
        }
    }
    
    /**
     * Called by the PacketHandler when a fish bite is detected
     * This method schedules an auto-catch with a random delay
     */
    public void onFishBite() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;
            
            // Only proceed if auto-catch is enabled
            if (!GoFishConfig.enableAutoCatch) return;
            
            // Schedule auto-catch with a random delay
            int delay = GoFishConfig.minCatchDelay + 
                      random.nextInt(GoFishConfig.maxCatchDelay - GoFishConfig.minCatchDelay + 1);
            
            scheduledCatch = true;
            scheduledCatchTime = System.currentTimeMillis() + delay;
            
            // Notify the player that auto-catch is scheduled
            mc.thePlayer.addChatMessage(
                new ChatComponentText("§b[GoFish] §fAuto-catch scheduled in " + delay + "ms")
            );
            
            sendDebugMessage(mc, "Scheduled auto-catch in " + delay + "ms");
        } catch (Exception e) {
            System.err.println("[GoFish] Error scheduling auto-catch: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        try {
            // Only process on the client phase END
            if (event.phase != TickEvent.Phase.END) return;
            
            // Safety check for Minecraft instance
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) {
                return;
            }
            
            // Handle right-click simulation if active
            if (isRightClicking) {
                // Press the right mouse button
                if (rightClickDuration <= RIGHT_CLICK_TICKS) {
                    // Method 1: Use KeyBinding state
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
                    
                    rightClickDuration++;
                    
                    if (rightClickDuration == 1) {
                        sendDebugMessage(mc, "Pressing right mouse button");
                    }
                } else {
                    // Release the use item key after the duration
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                    isRightClicking = false;
                    rightClickDuration = 0;
                    
                    sendDebugMessage(mc, "Released right mouse button");
                }
            }
            
            // Check for scheduled auto-catch
            if (scheduledCatch && System.currentTimeMillis() >= scheduledCatchTime) {
                // Start the right-click simulation if we're fishing and not already right-clicking
                if (FishingUtils.isPlayerFishing() && !isRightClicking) {
                    isRightClicking = true;
                    rightClickDuration = 0;
                    
                    sendDebugMessage(mc, "Auto-catch triggered - DEBUG INFO:");
                    sendDebugMessage(mc, "- Auto-catch enabled: " + GoFishConfig.enableAutoCatch);
                    sendDebugMessage(mc, "- Is fishing: " + FishingUtils.isPlayerFishing());
                    sendDebugMessage(mc, "- Has fishing hook: " + (FishingUtils.getFishingHook() != null));
                    
                    if (mc.thePlayer.getHeldItem() != null) {
                        sendDebugMessage(mc, "- Held item: " + mc.thePlayer.getHeldItem().getUnlocalizedName());
                    } else {
                        sendDebugMessage(mc, "- Held item: null");
                    }
                }
                scheduledCatch = false;
            }
        } catch (Exception e) {
            // Log any errors but don't crash the game
            System.err.println("[GoFish] Error in fishing handler: " + e.getMessage());
            e.printStackTrace();
            
            // Try to send error to chat
            try {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc != null && mc.thePlayer != null) {
                    mc.thePlayer.addChatMessage(
                        new ChatComponentText("§c[GoFish] Error: " + e.getMessage())
                    );
                }
            } catch (Exception ex) {
                // Ignore
            }
            
            // Reset state to prevent cascading errors
            scheduledCatch = false;
            isRightClicking = false;
            rightClickDuration = 0;
            // Make sure to release the key if there was an error
            try {
                KeyBinding.setKeyBindState(Minecraft.getMinecraft().gameSettings.keyBindUseItem.getKeyCode(), false);
            } catch (Exception ex) {
                // Ignore
            }
        }
    }
} 