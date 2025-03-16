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
 * Handler for detecting fishing bobber movement
 * This is a more reliable way to detect when a fish bites in Hypixel SkyBlock
 * Supports both water and lava fishing
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
    private boolean hookInLiquid = false;
    private boolean isInLava = false;
    private int ticksInLiquid = 0;
    private int stabilityCounter = 0;
    
    // Auto-catch state
    private boolean scheduledCatch = false;
    private long scheduledCatchTime = 0;
    private final Random random = new Random();
    
    // Right-click simulation state
    private boolean isRightClicking = false;
    private int rightClickDuration = 0;
    private static final int RIGHT_CLICK_TICKS = 4; // Hold right-click for this many ticks
    
    // Lava has different physics, so we need different thresholds
    private static final double WATER_STABILITY_THRESHOLD = 0.01;
    private static final double LAVA_STABILITY_THRESHOLD = 0.02; // Lava is more viscous
    private static final double WATER_BITE_DOWN_THRESHOLD = -0.02;
    private static final double WATER_BITE_UP_THRESHOLD = 0.03;
    private static final double LAVA_BITE_DOWN_THRESHOLD = -0.015;
    private static final double LAVA_BITE_UP_THRESHOLD = 0.025;
    
    /**
     * Helper method to send debug messages to chat
     */
    private void sendDebugMessage(Minecraft mc, String message) {
        if (DEBUG_MODE && mc != null && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText("§7[GoFish Debug] §f" + message));
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
                    
                    // Method 2: Direct call to Minecraft's right click method
                    if (rightClickDuration == 1) {
                        // This is a more direct way to trigger a right-click
                        //mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
                        
                        sendDebugMessage(mc, "Directly calling right-click action");
                        
                        // Also notify the player in chat that we're trying to catch
                        mc.thePlayer.addChatMessage(
                            new ChatComponentText("§b[GoFish] §fAttempting to reel in fish...")
                        );
                    }
                    
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
                
                // Continue processing other logic
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
                    
                    // Try a direct right-click approach
                    try {
                        // Simulate right mouse click directly
                        mc.thePlayer.swingItem();
                        KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());
                        sendDebugMessage(mc, "Attempted direct right-click simulation");
                    } catch (Exception e) {
                        sendDebugMessage(mc, "Error in direct right-click: " + e.getMessage());
                    }
                }
                scheduledCatch = false;
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
                hookInLiquid = false;
                isInLava = false;
                ticksInLiquid = 0;
                stabilityCounter = 0;
                scheduledCatch = false;
                return;
            }
            
            boolean currentlyInLiquid = fishHook.isInWater() || fishHook.isInLava();
            boolean currentlyInLava = fishHook.isInLava();
            
            // Detect new hook cast
            if (!hookInLiquid && currentlyInLiquid) {
                hookInLiquid = true;
                isInLava = currentlyInLava;
                hookCastTime = System.currentTimeMillis();
                sendDebugMessage(mc, "Hook entered " + (isInLava ? "lava" : "water"));
            } else if (hookInLiquid && !currentlyInLiquid) {
                hookInLiquid = false;
                isInLava = false;
                ticksInLiquid = 0;
                stabilityCounter = 0;
                sendDebugMessage(mc, "Hook left liquid");
            }
            
            // Only process if hook is in liquid
            if (!hookInLiquid) {
                prevMotionY = fishHook.motionY;
                return;
            }
            
            // Count ticks in liquid
            ticksInLiquid++;
            
            // Get current motion
            double currentMotionY = fishHook.motionY;
            
            // Wait for the bobber to stabilize after casting
            if (System.currentTimeMillis() - hookCastTime < CAST_IGNORE_TIME) {
                prevMotionY = currentMotionY;
                return;
            }
            
            // Use appropriate thresholds based on liquid type
            double stabilityThreshold = isInLava ? LAVA_STABILITY_THRESHOLD : WATER_STABILITY_THRESHOLD;
            
            // Check if the bobber has stabilized
            if (Math.abs(currentMotionY) < stabilityThreshold) {
                stabilityCounter++;
            } else {
                stabilityCounter = 0;
            }
            
            // Only start detecting bites after the bobber has been stable for a while
            if (stabilityCounter < 10) {
                prevMotionY = currentMotionY;
                return;
            }
            
            // Use appropriate bite thresholds based on liquid type
            double biteDownThreshold = isInLava ? LAVA_BITE_DOWN_THRESHOLD : WATER_BITE_DOWN_THRESHOLD;
            double biteUpThreshold = isInLava ? LAVA_BITE_UP_THRESHOLD : WATER_BITE_UP_THRESHOLD;
            
            // Check for the characteristic "bobbing" motion when a fish bites
            if (prevMotionY < biteDownThreshold && currentMotionY > biteUpThreshold) {
                long currentTime = System.currentTimeMillis();
                
                // Only trigger if we haven't detected a bite recently (prevent spam)
                if (currentTime - lastBiteTime > BITE_COOLDOWN) {
                    lastBiteTime = currentTime;
                    
                    sendDebugMessage(mc, "Detected bobber movement in " + (isInLava ? "lava" : "water") +
                                      ": prevY=" + String.format("%.4f", prevMotionY) + 
                                      ", currentY=" + String.format("%.4f", currentMotionY) +
                                      ", ticks in liquid=" + ticksInLiquid);
                    
                    // Only show notification if enabled
                    if (GoFishConfig.showFishCaughtMessages) {
                        // Notify the player that a fish was caught
                        mc.thePlayer.addChatMessage(
                            new ChatComponentText("§b[GoFish] §fFish on the hook! Reel it in!")
                        );
                    }
                    
                    // Play sound if enabled
                    if (GoFishConfig.playSoundOnFishCaught) {
                        mc.thePlayer.playSound("random.orb", 1.0F, 1.0F);
                    }
                    
                    // Schedule auto-catch if enabled
                    if (GoFishConfig.enableAutoCatch) {
                        int delay = GoFishConfig.minCatchDelay + 
                                  random.nextInt(GoFishConfig.maxCatchDelay - GoFishConfig.minCatchDelay + 1);
                        scheduledCatch = true;
                        scheduledCatchTime = System.currentTimeMillis() + delay;
                        
                        // Notify the player that auto-catch is scheduled
                        mc.thePlayer.addChatMessage(
                            new ChatComponentText("§b[GoFish] §fAuto-catch scheduled in " + delay + "ms")
                        );
                        
                        sendDebugMessage(mc, "Scheduled auto-catch in " + delay + "ms");
                    }
                }
            }
            
            // Update previous motion for next tick
            prevMotionY = currentMotionY;
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
            prevMotionY = 0.0;
            hookInLiquid = false;
            isInLava = false;
            ticksInLiquid = 0;
            stabilityCounter = 0;
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