package com.example.gofish.utils;

import com.example.gofish.handlers.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;

/**
 * Utility class for fishing-related functionality
 */
public class FishingUtils {
    
    // Track previous fishing state to detect when casting happens
    private static boolean wasFishing = false;
    private static PacketHandler packetHandler = null;

    /**
     * Set the packet handler for communication
     */
    public static void setPacketHandler(PacketHandler handler) {
        packetHandler = handler;
    }
    
    /**
     * Check if the player is currently fishing
     * @return true if the player is fishing, false otherwise
     */
    public static boolean isPlayerFishing() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null) return false;
            
            EntityPlayer player = mc.thePlayer;
            if (player == null) return false;
            
            // Check if player is holding a fishing rod
            ItemStack heldItem = player.getHeldItem();
            if (heldItem == null || !(heldItem.getItem() instanceof ItemFishingRod)) {
                return false;
            }
            
            // Check if player has a fishing hook in the water
            boolean isFishing = player.fishEntity != null;
            
            // Detect when player starts fishing (casting)
            if (isFishing && !wasFishing) {
                // Player just cast their rod
                if (packetHandler != null) {
                    packetHandler.updateHookCastTime();
                }
            }
            
            // Update previous state
            wasFishing = isFishing;
            
            return isFishing;
        } catch (Exception e) {
            System.err.println("[GoFish] Error checking if player is fishing: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if the player was fishing in the previous tick
     * @return true if the player was fishing, false otherwise
     */
    public static boolean wasFishing() {
        return wasFishing;
    }
    
    /**
     * Check if the player is holding a fishing rod
     * @return true if the player is holding a fishing rod, false otherwise
     */
    public static boolean isHoldingFishingRod() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null) return false;
            
            EntityPlayer player = mc.thePlayer;
            if (player == null) return false;
            
            // Check if player is holding a fishing rod
            ItemStack heldItem = player.getHeldItem();
            return heldItem != null && heldItem.getItem() instanceof ItemFishingRod;
        } catch (Exception e) {
            System.err.println("[GoFish] Error checking if player is holding fishing rod: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get the position of the player's fishing hook
     * @return the fishing hook entity or null if not fishing
     */
    public static EntityFishHook getFishingHook() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null) return null;
            
            EntityPlayer player = mc.thePlayer;
            if (player == null) return null;
            
            return player.fishEntity;
        } catch (Exception e) {
            System.err.println("[GoFish] Error getting fishing hook: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if the player is on Hypixel SkyBlock
     * @return true if on Hypixel SkyBlock, false otherwise
     */
    public static boolean isOnHypixelSkyblock() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null || mc.theWorld == null) return false;
            
            // Check if connected to Hypixel
            if (mc.getCurrentServerData() != null) {
                String serverIP = mc.getCurrentServerData().serverIP.toLowerCase();
                if (serverIP == null || !(serverIP.contains("hypixel.net") || serverIP.contains("hypixel.io"))) {
                    return false;
                }
                
                // Check for SkyBlock-specific scoreboard
                Scoreboard scoreboard = mc.theWorld.getScoreboard();
                if (scoreboard != null) {
                    // Check for SkyBlock scoreboard objective (typically "SBScoreboard")
                    for (ScoreObjective objective : scoreboard.getScoreObjectives()) {
                        if (objective == null) continue;
                        
                        String displayName = objective.getDisplayName();
                        String objectiveName = objective.getName();
                        
                        if (displayName != null && 
                            (displayName.contains("SKYBLOCK") || 
                             displayName.contains("SkyBlock"))) {
                            return true;
                        }
                        
                        if (objectiveName != null && objectiveName.contains("sb")) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            System.err.println("[GoFish] Error checking if on Hypixel SkyBlock: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if we're on Hypixel (even if not specifically in SkyBlock)
     * This is a fallback detection method
     */
    public static boolean isOnHypixel() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null || mc.getCurrentServerData() == null) return false;
            
            String serverIP = mc.getCurrentServerData().serverIP;
            if (serverIP == null) return false;
            
            serverIP = serverIP.toLowerCase();
            return serverIP.contains("hypixel.net") || serverIP.contains("hypixel.io");
        } catch (Exception e) {
            System.err.println("[GoFish] Error checking if on Hypixel: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if the player's fishing hook is in a valid liquid (water or lava)
     * @return true if the hook exists and is in liquid, false otherwise
     */
    public static boolean isHookInLiquid() {
        EntityFishHook hook = getFishingHook();
        if (hook == null) return false;
        
        // Check if the hook is in water
        boolean inWater = hook.isInWater();
        
        if (inWater) {
            return true;
        } else {
            // Additional checks - sometimes isInWater() can be unreliable
            // Check if the hook's motion has settled which usually indicates it's in water
            boolean hasSettled = Math.abs(hook.motionY) < 0.01 && 
                               Math.abs(hook.motionX) < 0.01 && 
                               Math.abs(hook.motionZ) < 0.01;
            
            // Check if the hook is below a certain Y level which likely means it's in water
            boolean atWaterLevel = hook.posY % 1 < 0.9;
            
            return hasSettled && atWaterLevel;
        }
    }

    /**
     * Check if the player's fishing hook is in lava
     * @return true if the hook is in lava, false otherwise
     */
    public static boolean isHookInLava() {
        try {
            EntityFishHook hook = getFishingHook();
            if (hook == null) return false;
            
            return hook.isInLava();
        } catch (Exception e) {
            System.err.println("[GoFish] Error checking if hook is in lava: " + e.getMessage());
            return false;
        }
    }
} 