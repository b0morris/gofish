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
    
    // Auto-catch state
    private boolean scheduledCatch = false;
    private long scheduledCatchTime = 0;
    private final Random random = new Random();
    
    // Right-click simulation state
    private boolean isRightClicking = false;
    private int rightClickDuration = 0;
    private static final int RIGHT_CLICK_TICKS = 1; // Hold right-click for this many ticks
    
    // Auto-recast state
    private boolean scheduledRecast = false;
    private long scheduledRecastTime = 0;
    
    // Backup recast state to catch missed recasts
    private boolean needsBackupRecast = false;
    private long lastFishCatchTime = 0;
    private static final int BACKUP_RECAST_TIMEOUT = 10000; // 10 seconds backup timeout
    
    // Reference to ShiftKeyHandler
    private ShiftKeyHandler shiftKeyHandler;
    
    // Reference to JumpHandler
    private JumpHandler jumpHandler;
    
    // Reference to PositionTracker
    private PositionTracker positionTracker;
    
    // Set the ShiftKeyHandler reference
    public void setShiftKeyHandler(ShiftKeyHandler shiftKeyHandler) {
        this.shiftKeyHandler = shiftKeyHandler;
    }
    
    // Set the JumpHandler reference
    public void setJumpHandler(JumpHandler jumpHandler) {
        this.jumpHandler = jumpHandler;
    }
    
    // Set the PositionTracker reference
    public void setPositionTracker(PositionTracker positionTracker) {
        this.positionTracker = positionTracker;
    }
    
    // Check if shift is currently active
    private boolean isShiftActive() {
        return shiftKeyHandler != null && shiftKeyHandler.isShiftActive();
    }
    
    // Check if jump is currently active
    private boolean isJumpActive() {
        return jumpHandler != null && jumpHandler.isJumpActive();
    }
    
    // Check if safety features are triggered
    private boolean isSafetyTriggered() {
        return positionTracker != null && positionTracker.isSafetyTriggered();
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
     * Called by the PacketHandler when a fish bite is detected
     * This method schedules an auto-catch with a random delay
     */
    public void onFishBite() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;
            
            // Only proceed if auto-catch is enabled
            if (!GoFishConfig.enableAutoCatch) return;
            
            // Determine if we should intentionally miss this fish
            boolean shouldMiss = GoFishConfig.enableMissChance && 
                                random.nextInt(100) < GoFishConfig.missChancePercentage;
            
            // Calculate base delay
            int delay = GoFishConfig.minCatchDelay + 
                      random.nextInt(GoFishConfig.maxCatchDelay - GoFishConfig.minCatchDelay + 1);
            
            // Apply timing offset if we should miss
            int timingOffset = 0;
            if (shouldMiss) {
                // Generate a random offset between min and max miss timing
                timingOffset = GoFishConfig.minMissTimingOffset + 
                             random.nextInt(GoFishConfig.maxMissTimingOffset - GoFishConfig.minMissTimingOffset + 1);
                
                if (GoFishConfig.enableDebugNotifications) {
                    String missType = timingOffset < 0 ? "early" : "late";
                    mc.thePlayer.addChatMessage(
                        new ChatComponentText(formatColorCodes("&7[GoFish] &fIntentionally missing fish (" + 
                                             missType + " by " + Math.abs(timingOffset) + "ms)"))
                    );
                }
            }
            
            // Apply the final delay (base delay + timing offset if missing)
            int finalDelay = delay + timingOffset;
            if (finalDelay < 0) finalDelay = 0; // Ensure we don't get negative delay
            
            scheduledCatch = true;
            scheduledCatchTime = System.currentTimeMillis() + finalDelay;
            
            // Set the last catch time for backup recast mechanism
            lastFishCatchTime = System.currentTimeMillis();
            needsBackupRecast = true;
            
            // Notify the player that auto-catch is scheduled
            if (GoFishConfig.enableDebugNotifications) {
                String catchMsg = shouldMiss ? 
                    String.format("Auto-catch scheduled in %dms (miss by %d ms)", finalDelay, timingOffset) :
                    String.format("Auto-catch scheduled in %dms", finalDelay);
                
                mc.thePlayer.addChatMessage(
                    new ChatComponentText(formatColorCodes("&b[GoFish] &f" + catchMsg))
                );
            }
            
            sendDebugMessage(mc, "Scheduled auto-catch in " + finalDelay + "ms" + 
                (shouldMiss ? " (intentional miss)" : ""));
        } catch (Exception e) {
            System.err.println("[GoFish] Error scheduling auto-catch: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Schedule a recast of the fishing rod after a random delay
     */
    private void scheduleRecast() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;
            
            sendDebugMessage(mc, "Started recast");
            
            // Only proceed if auto-recast is enabled
            if (!GoFishConfig.enableAutoRecast) return;
            
            // Schedule recast with a random delay between minRecastDelay and maxRecastDelay
            int delay = GoFishConfig.minRecastDelay + 
                      random.nextInt(GoFishConfig.maxRecastDelay - GoFishConfig.minRecastDelay + 1);
            
            scheduledRecast = true;
            scheduledRecastTime = System.currentTimeMillis() + delay;
            
            // Clear the backup recast flag since we're handling a recast now
            needsBackupRecast = false;
            
            if (GoFishConfig.enableDebugNotifications) {
                mc.thePlayer.addChatMessage(
                    new ChatComponentText(formatColorCodes("&b[GoFish] &fAuto-recast scheduled in " + delay + "ms"))
                );
            }
            
            sendDebugMessage(mc, "Scheduled auto-recast in " + delay + "ms");
        } catch (Exception e) {
            System.err.println("[GoFish] Error scheduling auto-recast: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Cast the fishing rod if not already fishing
     */
    public void castRodIfNeeded() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;
            
            // Only cast if we're holding a fishing rod and not already fishing
            if (!FishingUtils.isPlayerFishing() && FishingUtils.isHoldingFishingRod() && !isRightClicking) {
                sendDebugMessage(mc, "Auto-casting fishing rod");
                
                // Start right-click simulation
                isRightClicking = true;
                rightClickDuration = 0;
                
                if (GoFishConfig.enableDebugNotifications) {
                    mc.thePlayer.addChatMessage(
                        new ChatComponentText(formatColorCodes("&b[GoFish] &fAuto-casting fishing rod"))
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("[GoFish] Error auto-casting rod: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Reel in the fishing rod if currently fishing
     */
    public void reelInIfNeeded() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;
            
            // Only reel in if we're currently fishing and not already right-clicking
            if (FishingUtils.isPlayerFishing() && !isRightClicking) {
                sendDebugMessage(mc, "Auto-reeling in fishing rod");
                
                // Start right-click simulation
                isRightClicking = true;
                rightClickDuration = 0;
                
                if (GoFishConfig.enableDebugNotifications) {
                    mc.thePlayer.addChatMessage(
                        new ChatComponentText(formatColorCodes("&b[GoFish] &fAuto-reeling in fishing rod"))
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("[GoFish] Error reeling in rod: " + e.getMessage());
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
            
            // Check if safety is triggered and disable auto-catch if needed
            if (isSafetyTriggered() && GoFishConfig.enableAutoCatch) {
                GoFishConfig.enableAutoCatch = false;
                GoFishConfig.enableAutoRecast = false;
                GoFishConfig.saveConfig();
                
                String reason = positionTracker.getSafetyTriggerReason();
                sendDebugMessage(mc, "Safety triggered: " + reason);
                
                // Notify player that auto-fishing was disabled
                mc.thePlayer.addChatMessage(new ChatComponentText(
                    formatColorCodes("&c[GoFish] &fAuto-fishing disabled for safety: " + reason)
                ));
                
                // Reset scheduledCatch and scheduledRecast
                scheduledCatch = false;
                scheduledRecast = false;
                needsBackupRecast = false;
                return;
            }
            
            // Check if we need a backup recast (in case the normal recast mechanism failed)
            if (needsBackupRecast && GoFishConfig.enableAutoRecast && System.currentTimeMillis() - lastFishCatchTime > BACKUP_RECAST_TIMEOUT) {
                // Only trigger if we're not fishing, not already scheduling a recast, and not right-clicking
                if (!FishingUtils.isPlayerFishing() && !scheduledRecast && !isRightClicking && FishingUtils.isHoldingFishingRod()) {
                    sendDebugMessage(mc, "BACKUP RECAST TRIGGERED - Fishing state appears stuck!");
                    
                    if (GoFishConfig.enableDebugNotifications) {
                        mc.thePlayer.addChatMessage(
                            new ChatComponentText(formatColorCodes("&e[GoFish] &fBackup recast triggered - normal recast seems to have failed."))
                        );
                    }
                    
                    // Force a recast
                    scheduleRecast();
                }
                
                // Reset the backup recast flag regardless
                needsBackupRecast = false;
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
                    
                    // Enhanced recast logic: if we were fishing (reel in operation) or need to start fishing
                    boolean wasFishing = FishingUtils.isPlayerFishing();
                    
                    // Wait a few ticks to ensure fishing state is updated
                    mc.theWorld.updateEntities(); // Force entity update to ensure fishing state is current
                    
                    // If auto-recast is enabled and we're not currently fishing (either after reeling in or initially)
                    if (GoFishConfig.enableAutoRecast && 
                        (wasFishing || !FishingUtils.isPlayerFishing()) && 
                        FishingUtils.isHoldingFishingRod()) {
                        
                        sendDebugMessage(mc, "Recast condition met - was fishing: " + wasFishing + 
                                         ", now fishing: " + FishingUtils.isPlayerFishing());
                        scheduleRecast();
                    }
                }
            }
            
            // Check for scheduled auto-catch
            if (scheduledCatch && System.currentTimeMillis() >= scheduledCatchTime) {
                // If jump is active, don't reel in until jump is complete
                if (isJumpActive()) {
                    sendDebugMessage(mc, "Delaying auto-catch because jump is active");
                    return;
                }
                
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
            
            // Check for scheduled auto-recast
            if (scheduledRecast && System.currentTimeMillis() >= scheduledRecastTime) {
                // If jump is active, don't recast until jump is complete
                if (isJumpActive()) {
                    sendDebugMessage(mc, "Delaying auto-recast because jump is active");
                    return;
                }
                
                // Start the right-click simulation if we're not fishing and not already right-clicking
                if (!FishingUtils.isPlayerFishing() && !isRightClicking && FishingUtils.isHoldingFishingRod()) {
                    isRightClicking = true;
                    rightClickDuration = 0;
                    
                    sendDebugMessage(mc, "Auto-recast triggered - DEBUG INFO:");
                    sendDebugMessage(mc, "- Auto-recast enabled: " + GoFishConfig.enableAutoRecast);
                    sendDebugMessage(mc, "- Is fishing: " + FishingUtils.isPlayerFishing());
                    
                    if (mc.thePlayer.getHeldItem() != null) {
                        sendDebugMessage(mc, "- Held item: " + mc.thePlayer.getHeldItem().getUnlocalizedName());
                    } else {
                        sendDebugMessage(mc, "- Held item: null");
                    }
                }
                scheduledRecast = false;
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
                        new ChatComponentText(formatColorCodes("&c[GoFish] Error: " + e.getMessage()))
                    );
                }
            } catch (Exception ex) {
                // Ignore
            }
            
            // Reset state to prevent cascading errors
            scheduledCatch = false;
            scheduledRecast = false;
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
    
    /**
     * Helper method to check if the player was fishing in the previous tick
     * This is used to detect when the player reels in their fishing rod
     */
    private boolean wasFishing() {
        return FishingUtils.wasFishing();
    }
} 