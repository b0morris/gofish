package com.example.gofish.handlers;

import com.example.gofish.config.GoFishConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Handler class to monitor player position and rotation for safety features
 * If player moves or looks too far from original position, auto-fishing will be disabled
 */
public class PositionTracker {
    
    // Track player position
    private double lastX = 0;
    private double lastZ = 0;
    private boolean positionInitialized = false;
    
    // Track player rotation
    private float lastYaw = 0;
    private float lastPitch = 0;
    private boolean rotationInitialized = false;
    
    // Safety detection state
    private boolean movementDetected = false;
    private boolean rotationDetected = false;
    
    /**
     * Convert & color codes to § color codes
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
     * Send notification message to chat
     */
    private void sendNotification(Minecraft mc, String message) {
        if (mc != null && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(formatColorCodes("&c[GoFish] &f" + message)));
        }
    }
    
    /**
     * Reset position tracking when auto-catch is toggled on
     */
    public void resetTracking() {
        positionInitialized = false;
        rotationInitialized = false;
        movementDetected = false;
        rotationDetected = false;
    }
    
    /**
     * Check if movement or rotation is detected that should disable auto-fishing
     */
    public boolean isSafetyTriggered() {
        return movementDetected || rotationDetected;
    }
    
    /**
     * Get reason for safety trigger
     */
    public String getSafetyTriggerReason() {
        if (movementDetected) {
            return "Player movement detected";
        } else if (rotationDetected) {
            return "Player looking too far from original view";
        }
        return "Unknown";
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        try {
            // Only process on the client phase END
            if (event.phase != TickEvent.Phase.END) return;
            
            // Only track if auto-catch is enabled
            if (!GoFishConfig.enableAutoCatch) {
                resetTracking();
                return;
            }
            
            // Safety check for Minecraft instance
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) {
                return;
            }
            
            // Track position
            if (!positionInitialized) {
                lastX = mc.thePlayer.posX;
                lastZ = mc.thePlayer.posZ;
                positionInitialized = true;
                
                if (GoFishConfig.enableDebugNotifications) {
                    sendDebugMessage(mc, "Initialized position tracking at X: " + lastX + ", Z: " + lastZ);
                }
            } else {
                // Check if player moved
                double currentX = mc.thePlayer.posX;
                double currentZ = mc.thePlayer.posZ;
                
                double deltaX = Math.abs(currentX - lastX);
                double deltaZ = Math.abs(currentZ - lastZ);
                
                // If player moved more than configured threshold in X or Z, trigger safety
                if ((deltaX > GoFishConfig.positionSafetyThreshold || deltaZ > GoFishConfig.positionSafetyThreshold) 
                    && GoFishConfig.enableSafetyFeatures) {
                    if (!movementDetected) {
                        sendNotification(mc, "Movement detected! Auto-fishing disabled for safety.");
                        sendDebugMessage(mc, "Movement of " + deltaX + ", " + deltaZ + " blocks detected!");
                        movementDetected = true;
                    }
                }
            }
            
            // Track rotation
            if (!rotationInitialized) {
                lastYaw = mc.thePlayer.rotationYaw;
                lastPitch = mc.thePlayer.rotationPitch;
                rotationInitialized = true;
                
                if (GoFishConfig.enableDebugNotifications) {
                    sendDebugMessage(mc, "Initialized rotation tracking at Yaw: " + lastYaw + ", Pitch: " + lastPitch);
                }
            } else {
                // Check if player rotated too far
                float currentYaw = mc.thePlayer.rotationYaw;
                float currentPitch = mc.thePlayer.rotationPitch;
                
                // Normalize yaw differences (handle -180/180 boundary)
                float yawDiff = Math.abs(currentYaw - lastYaw);
                if (yawDiff > 180) {
                    yawDiff = 360 - yawDiff;
                }
                
                float pitchDiff = Math.abs(currentPitch - lastPitch);
                
                // Allow normal movement within FOV range + safety margin
                float allowedRotation = GoFishConfig.randomLookFovRange + GoFishConfig.rotationSafetyThreshold;
                
                // If player rotated too far, trigger safety
                if ((yawDiff > allowedRotation || pitchDiff > allowedRotation) && GoFishConfig.enableSafetyFeatures) {
                    if (!rotationDetected) {
                        sendNotification(mc, "Looking too far from original view! Auto-fishing disabled for safety.");
                        sendDebugMessage(mc, "Rotation of " + yawDiff + " yaw, " + pitchDiff + " pitch detected!");
                        rotationDetected = true;
                    }
                }
            }
        } catch (Exception e) {
            // Log any errors but don't crash the game
            System.err.println("[GoFish] Error in position tracker: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 