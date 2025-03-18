package com.example.gofish.handlers;

import com.example.gofish.config.GoFishConfig;
import com.example.gofish.utils.FishingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

/**
 * Handler for periodically jumping to appear more human-like
 */
public class JumpHandler {
    
    // Random number generator
    private final Random random = new Random();
    
    // State tracking
    private boolean isJumping = false;
    private long nextJumpTime = 0;
    private long jumpEndTime = 0;
    
    /**
     * Convert & color codes to § color codes
     * @param message The message with & color codes
     * @return The message with § color codes
     */
    private static String formatColorCodes(String message) {
        char sectionSign = '\u00A7';
        return message.replace('&', sectionSign);
    }
    
    /**
     * Helper method to send debug messages to chat
     */
    private void sendDebugMessage(Minecraft mc, String message) {
        if (GoFishConfig.enableDebugNotifications && mc != null && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(formatColorCodes("&7[GoFish Debug] &f" + message)));
        }
    }
    
    /**
     * Schedule the next jump
     */
    private void scheduleNextJump() {
        // Calculate random delay between min and max
        int delay = GoFishConfig.minJumpInterval + 
                  random.nextInt(GoFishConfig.maxJumpInterval - GoFishConfig.minJumpInterval + 1);
        
        nextJumpTime = System.currentTimeMillis() + delay;
        
        if (GoFishConfig.enableDebugNotifications) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.thePlayer != null) {
                sendDebugMessage(mc, "Next jump scheduled in " + (delay / 1000) + " seconds");
            }
        }
    }
    
    /**
     * Start jumping
     */
    private void startJump(Minecraft mc) {
        if (mc == null || mc.thePlayer == null) return;
        
        // Set state
        isJumping = true;
        jumpEndTime = System.currentTimeMillis() + GoFishConfig.jumpDuration;
        
        // Press the jump key
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);
        
        if (GoFishConfig.enableDebugNotifications) {
            sendDebugMessage(mc, "Started jumping");
        }
    }
    
    /**
     * Stop jumping
     */
    private void stopJump(Minecraft mc) {
        if (mc == null) return;
        
        // Release the jump key
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
        
        // Reset state
        isJumping = false;
        
        // Schedule next jump
        scheduleNextJump();
        
        if (GoFishConfig.enableDebugNotifications) {
            sendDebugMessage(mc, "Stopped jumping");
        }
    }
    
    /**
     * Public method to check if jump is currently active
     * @return true if currently jumping, false otherwise
     */
    public boolean isJumpActive() {
        return isJumping;
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        try {
            // Only process on the client phase END
            if (event.phase != TickEvent.Phase.END) return;
            
            // Check if periodic jump is enabled AND auto-catch is enabled
            if (!GoFishConfig.enablePeriodicJump || !GoFishConfig.enableAutoCatch) {
                // If we were jumping, stop it
                if (isJumping) {
                    Minecraft mc = Minecraft.getMinecraft();
                    stopJump(mc);
                }
                return;
            }
            
            // Safety check for Minecraft instance
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) {
                return;
            }
            
            // Initialize next jump time if needed
            if (nextJumpTime == 0) {
                scheduleNextJump();
                return;
            }
            
            // Handle current jump if active
            if (isJumping) {
                long currentTime = System.currentTimeMillis();
                if (currentTime >= jumpEndTime) {
                    stopJump(mc);
                }
                return;
            }
            
            // Only start new jumps when fishing or holding a fishing rod
            if (!FishingUtils.isPlayerFishing() && !FishingUtils.isHoldingFishingRod()) {
                return;
            }
            
            // Check if it's time for a new jump
            long currentTime = System.currentTimeMillis();
            if (currentTime >= nextJumpTime) {
                startJump(mc);
            }
        } catch (Exception e) {
            // Log any errors but don't crash the game
            System.err.println("[GoFish] Error in jump handler: " + e.getMessage());
            e.printStackTrace();
            
            // Reset state to prevent cascading errors
            isJumping = false;
            nextJumpTime = System.currentTimeMillis() + 1000; // Wait a second before trying again
            
            // Make sure jump key is released
            try {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc != null) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
                }
            } catch (Exception ex) {
                // Ignore
            }
        }
    }
} 