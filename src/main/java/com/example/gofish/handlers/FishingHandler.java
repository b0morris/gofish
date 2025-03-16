package com.example.gofish.handlers;

import com.example.gofish.config.GoFishConfig;
import com.example.gofish.utils.FishingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Handler for detecting fishing bobber movement
 * This is a more reliable way to detect when a fish bites in Hypixel SkyBlock
 */
public class FishingHandler {
    
    // Debug mode for development
    private static final boolean DEBUG_MODE = true;
    
    // Track the fishing hook's previous position and motion
    private double prevMotionY = 0.0;
    private long lastBiteTime = 0;
    private long hookCastTime = 0;
    private static final long BITE_COOLDOWN = 1000; // 1 second cooldown between bite detections
    private static final long CAST_IGNORE_TIME = 2000; // Ignore detections for 2 seconds after casting
    
    // Track hook state
    private boolean hookInWater = false;
    private int ticksInWater = 0;
    private int stabilityCounter = 0;
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        try {
            // Only process on the client phase END
            if (event.phase != TickEvent.Phase.END) return;
            
            // Safety check for Minecraft instance
            if (Minecraft.getMinecraft() == null || Minecraft.getMinecraft().thePlayer == null) {
                return;
            }
            
            // Only process if notifications are enabled
            if (!GoFishConfig.enableNotifications) return;
            
            // Only process if we're on Hypixel
            if (!FishingUtils.isOnHypixel()) return;
            
            // Check if the player is fishing
            EntityFishHook fishHook = FishingUtils.getFishingHook();
            if (fishHook == null) {
                // Reset state when not fishing
                prevMotionY = 0.0;
                hookInWater = false;
                ticksInWater = 0;
                stabilityCounter = 0;
                return;
            }
            
            // Detect new hook cast
            if (!hookInWater && fishHook.isInWater()) {
                hookInWater = true;
                hookCastTime = System.currentTimeMillis();
                if (DEBUG_MODE) {
                    System.out.println("[GoFish] Hook entered water");
                }
            } else if (hookInWater && !fishHook.isInWater()) {
                hookInWater = false;
                ticksInWater = 0;
                stabilityCounter = 0;
                if (DEBUG_MODE) {
                    System.out.println("[GoFish] Hook left water");
                }
            }
            
            // Only process if hook is in water
            if (!hookInWater) {
                prevMotionY = fishHook.motionY;
                return;
            }
            
            // Count ticks in water
            ticksInWater++;
            
            // Get current motion
            double currentMotionY = fishHook.motionY;
            
            // Wait for the bobber to stabilize after casting
            // This helps avoid false detections from the initial splash
            if (System.currentTimeMillis() - hookCastTime < CAST_IGNORE_TIME) {
                prevMotionY = currentMotionY;
                return;
            }
            
            // Check if the bobber has stabilized (minimal vertical movement)
            if (Math.abs(currentMotionY) < 0.01) {
                stabilityCounter++;
            } else {
                stabilityCounter = 0;
            }
            
            // Only start detecting bites after the bobber has been stable for a while
            if (stabilityCounter < 10) {
                prevMotionY = currentMotionY;
                return;
            }
            
            // Check for the characteristic "bobbing" motion when a fish bites
            // In Hypixel SkyBlock, when a fish bites, the bobber typically has a sudden upward motion
            // after being relatively stable
            if (prevMotionY < -0.02 && currentMotionY > 0.03) {
                // This pattern indicates a fish bite - sudden change from downward to upward motion
                long currentTime = System.currentTimeMillis();
                
                // Only trigger if we haven't detected a bite recently (prevent spam)
                if (currentTime - lastBiteTime > BITE_COOLDOWN) {
                    lastBiteTime = currentTime;
                    
                    if (DEBUG_MODE) {
                        System.out.println("[GoFish] Detected bobber movement: prevY=" + 
                                          String.format("%.4f", prevMotionY) + 
                                          ", currentY=" + String.format("%.4f", currentMotionY) +
                                          ", ticks in water=" + ticksInWater);
                    }
                    
                    // Only show notification if enabled
                    if (GoFishConfig.showFishCaughtMessages) {
                        // Notify the player that a fish was caught
                        Minecraft.getMinecraft().thePlayer.addChatMessage(
                            new ChatComponentText("§b[GoFish] §fFish on the hook! Reel it in!")
                        );
                    }
                    
                    // Play sound if enabled
                    if (GoFishConfig.playSoundOnFishCaught) {
                        Minecraft.getMinecraft().thePlayer.playSound("random.orb", 1.0F, 1.0F);
                    }
                }
            }
            
            // Update previous motion for next tick
            prevMotionY = currentMotionY;
        } catch (Exception e) {
            // Log any errors but don't crash the game
            System.err.println("[GoFish] Error in fishing handler: " + e.getMessage());
            // Reset state to prevent cascading errors
            prevMotionY = 0.0;
            hookInWater = false;
            ticksInWater = 0;
            stabilityCounter = 0;
        }
    }
} 