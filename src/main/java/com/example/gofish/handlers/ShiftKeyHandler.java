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
 * Handler for periodically holding down the shift key
 * This simulates a player sneaking occasionally to appear more human-like
 */
public class ShiftKeyHandler {
    
    // Random number generator
    private final Random random = new Random();
    
    // State tracking
    private boolean isHoldingShift = false;
    private long nextShiftTime = 0;
    private long shiftEndTime = 0;
    
    /**
     * Public method to check if shift is currently active
     * @return true if shift is currently being held, false otherwise
     */
    public boolean isShiftActive() {
        return isHoldingShift;
    }
    
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
     * Schedule the next shift key press
     */
    private void scheduleNextShift() {
        // Calculate random delay between min and max
        int delay = GoFishConfig.minShiftInterval + 
                  random.nextInt(GoFishConfig.maxShiftInterval - GoFishConfig.minShiftInterval + 1);
        
        nextShiftTime = System.currentTimeMillis() + delay;
        
        if (GoFishConfig.enableDebugNotifications) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.thePlayer != null) {
                sendDebugMessage(mc, "Next shift scheduled in " + (delay / 1000) + " seconds");
            }
        }
    }
    
    /**
     * Start holding the shift key
     */
    private void startShiftHold(Minecraft mc) {
        if (mc == null || mc.thePlayer == null) return;
        
        // Calculate random duration between min and max
        int duration = GoFishConfig.minShiftDuration + 
                     random.nextInt(GoFishConfig.maxShiftDuration - GoFishConfig.minShiftDuration + 1);
        
        // Set state
        isHoldingShift = true;
        shiftEndTime = System.currentTimeMillis() + duration;
        
        // Press the shift key
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
        
        if (GoFishConfig.enableDebugNotifications) {
            sendDebugMessage(mc, "Started holding shift key for " + (duration / 1000) + " seconds");
        }
    }
    
    /**
     * Stop holding the shift key
     */
    private void stopShiftHold(Minecraft mc) {
        if (mc == null) return;
        
        // Release the shift key
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
        
        // Reset state
        isHoldingShift = false;
        
        // Schedule next shift
        scheduleNextShift();
        
        if (GoFishConfig.enableDebugNotifications) {
            sendDebugMessage(mc, "Released shift key");
        }
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        try {
            // Only process on the client phase END
            if (event.phase != TickEvent.Phase.END) return;
            
            // Check if periodic shift is enabled AND auto-catch is enabled
            if (!GoFishConfig.enablePeriodicShift || !GoFishConfig.enableAutoCatch) {
                // If we were holding shift, release it
                if (isHoldingShift) {
                    Minecraft mc = Minecraft.getMinecraft();
                    stopShiftHold(mc);
                }
                return;
            }
            
            // Safety check for Minecraft instance
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) {
                return;
            }
            
            // Initialize next shift time if needed
            if (nextShiftTime == 0) {
                scheduleNextShift();
                return;
            }
            
            // Handle current shift if active
            if (isHoldingShift) {
                long currentTime = System.currentTimeMillis();
                if (currentTime >= shiftEndTime) {
                    stopShiftHold(mc);
                }
                return;
            }
            
            // Only start new shifts when fishing
            if (!FishingUtils.isPlayerFishing() && !FishingUtils.isHoldingFishingRod()) {
                return;
            }
            
            // Check if it's time for a new shift
            long currentTime = System.currentTimeMillis();
            if (currentTime >= nextShiftTime) {
                startShiftHold(mc);
            }
        } catch (Exception e) {
            // Log any errors but don't crash the game
            System.err.println("[GoFish] Error in shift key handler: " + e.getMessage());
            e.printStackTrace();
            
            // Reset state to prevent cascading errors
            isHoldingShift = false;
            nextShiftTime = System.currentTimeMillis() + 1000; // Wait a second before trying again
            
            // Make sure shift key is released
            try {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc != null) {
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
                }
            } catch (Exception ex) {
                // Ignore
            }
        }
    }
} 