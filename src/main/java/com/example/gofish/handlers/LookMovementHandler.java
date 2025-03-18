package com.example.gofish.handlers;

import com.example.gofish.config.GoFishConfig;
import com.example.gofish.utils.FishingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

/**
 * Handler for random look movements to simulate human behavior
 */
public class LookMovementHandler {
    
    // Random number generator
    private final Random random = new Random();
    
    // Movement state
    private boolean isMoving = false;
    private long nextMovementTime = 0;
    private long movementEndTime = 0;
    private int movementCounter = 0;
    private static final int MOVEMENTS_BEFORE_RESET = 10; // Reset to original position every X movements
    
    // Target and current movement values
    private float targetYaw = 0;
    private float targetPitch = 0;
    private float startYaw = 0;
    private float startPitch = 0;
    private float currentProgress = 0;
    
    // Original position to maintain circular boundary
    private float originalYaw = 0;
    private float originalPitch = 0;
    private boolean originalPositionSet = false;
    
    // Control points for Bezier curve
    private float controlYaw1 = 0;
    private float controlYaw2 = 0;
    private float controlPitch1 = 0;
    private float controlPitch2 = 0;
    
    // Movement phases for acceleration/deceleration
    private static final float ACCELERATION_PHASE = 0.3f; // First 30% of movement is acceleration
    private static final float DECELERATION_PHASE = 0.7f; // Last 30% of movement is deceleration
    private static final float CONSTANT_SPEED_PHASE = 0.4f; // Middle 40% is constant speed
    
    // Previous position for calculating velocity
    private float prevYaw = 0;
    private float prevPitch = 0;
    private float currentVelocity = 0;
    private float targetVelocity = 0;
    private float maxVelocity = 0;
    
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
     * Schedule the next random look movement
     */
    private void scheduleNextMovement() {
        // Calculate random delay between min and max
        int delay = GoFishConfig.minLookMovementDelay + 
                  random.nextInt(GoFishConfig.maxLookMovementDelay - GoFishConfig.minLookMovementDelay + 1);
        
        nextMovementTime = System.currentTimeMillis() + delay;
        
        if (GoFishConfig.enableDebugNotifications) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.thePlayer != null) {
                sendDebugMessage(mc, "Next look movement scheduled in " + delay + "ms");
            }
        }
    }
    
    /**
     * Start a new random look movement
     */
    private void startNewMovement(Minecraft mc) {
        if (mc == null || mc.thePlayer == null) return;
        
        // Get current player rotation
        startYaw = mc.thePlayer.rotationYaw;
        startPitch = mc.thePlayer.rotationPitch;
        
        // Set original position if not set yet
        if (!originalPositionSet) {
            originalYaw = startYaw;
            originalPitch = startPitch;
            originalPositionSet = true;
            
            if (GoFishConfig.enableDebugNotifications) {
                sendDebugMessage(mc, "Original position set: Yaw=" + originalYaw + ", Pitch=" + originalPitch);
            }
        }
        
        // Calculate how far we've drifted from original position
        float currentDistanceFromOrigin = (float) Math.sqrt(
            Math.pow(startYaw - originalYaw, 2) + 
            Math.pow(startPitch - originalPitch, 2)
        );
        
        // If we're already near the edge of our allowed FOV range, bias movement back toward the center
        boolean needsCenteringMovement = currentDistanceFromOrigin > (GoFishConfig.randomLookFovRange * 0.75f);
        float angle;
        float distance;
        
        if (needsCenteringMovement) {
            // Angle pointing back to original position 
            angle = (float) Math.toDegrees(Math.atan2(originalPitch - startPitch, originalYaw - startYaw));
            
            // Random distance biased toward moving back to center
            // The further we are from center, the stronger the bias
            float centeringBias = Math.min(1.0f, currentDistanceFromOrigin / GoFishConfig.randomLookFovRange);
            float randomFactor = 1.0f - centeringBias;
            
            // When far from center, limit movement away from center, encourage movement toward center
            if (random.nextFloat() < centeringBias * 0.7f) {
                // Move toward center with some randomness
                distance = (random.nextFloat() * 0.5f + 0.5f) * Math.min(currentDistanceFromOrigin, GoFishConfig.randomLookFovRange * 0.3f);
                angle = (angle + (random.nextFloat() * 30 - 15)) % 360; // Small random deviation in angle
                
                if (GoFishConfig.enableDebugNotifications) {
                    sendDebugMessage(mc, "Centering movement triggered. Distance from origin: " + 
                                   String.format("%.2f", currentDistanceFromOrigin) + 
                                   ", bias: " + String.format("%.2f", centeringBias));
                }
            } else {
                // Random movement but limited distance to prevent going further from center
                angle = random.nextFloat() * 360f;
                distance = random.nextFloat() * GoFishConfig.randomLookFovRange * (1 - centeringBias * 0.8f);
            }
        } else {
            // Regular random movement when near the center
            angle = random.nextFloat() * 360f; 
            distance = random.nextFloat() * GoFishConfig.randomLookFovRange * 0.7f; // Reduce max distance to stay safely in FOV
        }
        
        // Convert polar coordinates to Cartesian offsets
        float yawOffset = (float) (distance * Math.cos(Math.toRadians(angle)));
        float pitchOffset = (float) (distance * Math.sin(Math.toRadians(angle)));
        
        // Calculate target position
        float newTargetYaw = startYaw + yawOffset;
        float newTargetPitch = startPitch + pitchOffset;
        
        // Enforce strict FOV boundary relative to original position
        // Check distance from original position to proposed target position
        float targetDistanceFromOrigin = (float) Math.sqrt(
            Math.pow(newTargetYaw - originalYaw, 2) + 
            Math.pow(newTargetPitch - originalPitch, 2)
        );
        
        // If movement would exceed FOV range, scale it back
        if (targetDistanceFromOrigin > GoFishConfig.randomLookFovRange) {
            float scale = GoFishConfig.randomLookFovRange / targetDistanceFromOrigin;
            newTargetYaw = originalYaw + (newTargetYaw - originalYaw) * scale;
            newTargetPitch = originalPitch + (newTargetPitch - originalPitch) * scale;
            
            if (GoFishConfig.enableDebugNotifications) {
                sendDebugMessage(mc, "Movement scaled to stay within FOV range. Scale: " + 
                               String.format("%.2f", scale));
            }
        }
        
        targetYaw = newTargetYaw;
        targetPitch = newTargetPitch;
        
        // Clamp pitch to prevent looking too far up or down
        if (targetPitch < -90) targetPitch = -90;
        if (targetPitch > 90) targetPitch = 90;
        
        // Generate random control points for Bezier curve (curved movement)
        // Control points are offset from the straight line to create a curve
        float curveIntensity = 0.3f + random.nextFloat() * 0.7f; // Random curve intensity between 0.3 and 1.0
        
        // First control point - closer to start
        controlYaw1 = startYaw + (targetYaw - startYaw) * 0.3f + 
                     (random.nextFloat() * 2 - 1) * GoFishConfig.randomLookFovRange * curveIntensity * 0.3f;
        controlPitch1 = startPitch + (targetPitch - startPitch) * 0.3f + 
                       (random.nextFloat() * 2 - 1) * GoFishConfig.randomLookFovRange * curveIntensity * 0.3f;
        
        // Second control point - closer to target
        controlYaw2 = startYaw + (targetYaw - startYaw) * 0.7f + 
                     (random.nextFloat() * 2 - 1) * GoFishConfig.randomLookFovRange * curveIntensity * 0.3f;
        controlPitch2 = startPitch + (targetPitch - startPitch) * 0.7f + 
                       (random.nextFloat() * 2 - 1) * GoFishConfig.randomLookFovRange * curveIntensity * 0.3f;
        
        // Calculate maximum velocity based on distance and duration
        float totalDistance = Math.abs(targetYaw - startYaw) + Math.abs(targetPitch - startPitch);
        maxVelocity = totalDistance / (GoFishConfig.lookMovementDuration / 1000f) * 2.5f;
        
        // Initialize velocity tracking
        prevYaw = startYaw;
        prevPitch = startPitch;
        currentVelocity = 0;
        
        // Set movement state
        isMoving = true;
        currentProgress = 0;
        movementEndTime = System.currentTimeMillis() + GoFishConfig.lookMovementDuration;
        
        if (GoFishConfig.enableDebugNotifications) {
            sendDebugMessage(mc, "Starting curved look movement: " +
                           "Yaw " + String.format("%.2f", startYaw) + " -> " + String.format("%.2f", targetYaw) + ", " +
                           "Pitch " + String.format("%.2f", startPitch) + " -> " + String.format("%.2f", targetPitch) +
                           " (distance from origin: " + String.format("%.2f", targetDistanceFromOrigin) + ")");
        }
    }
    
    /**
     * Update the current look movement
     */
    private void updateMovement(Minecraft mc) {
        if (mc == null || mc.thePlayer == null) return;
        
        // Calculate progress (0.0 to 1.0)
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - (movementEndTime - GoFishConfig.lookMovementDuration);
        currentProgress = Math.min(1.0f, (float) elapsedTime / GoFishConfig.lookMovementDuration);
        
        // Apply acceleration/deceleration curve
        float easedProgress = applyAccelerationDeceleration(currentProgress);
        
        // Use cubic Bezier curve for more natural movement
        float newYaw = cubicBezier(startYaw, controlYaw1, controlYaw2, targetYaw, easedProgress);
        float newPitch = cubicBezier(startPitch, controlPitch1, controlPitch2, targetPitch, easedProgress);
        
        // Calculate current velocity (for debug purposes)
        float yawDelta = Math.abs(newYaw - prevYaw);
        float pitchDelta = Math.abs(newPitch - prevPitch);
        currentVelocity = yawDelta + pitchDelta;
        
        // Store current position for next velocity calculation
        prevYaw = newYaw;
        prevPitch = newPitch;
        
        // Apply rotation
        mc.thePlayer.rotationYaw = newYaw;
        mc.thePlayer.rotationPitch = newPitch;
        
        // Debug velocity information
        if (GoFishConfig.enableDebugNotifications && random.nextInt(10) == 0) { // Only show occasionally to avoid spam
            sendDebugMessage(mc, "Movement velocity: " + String.format("%.2f", currentVelocity) + 
                           " (progress: " + String.format("%.2f", currentProgress) + ")");
        }
        
        // Check if movement is complete
        if (currentTime >= movementEndTime) {
            isMoving = false;
            scheduleNextMovement();
            
            if (GoFishConfig.enableDebugNotifications) {
                sendDebugMessage(mc, "Look movement completed");
            }
        }
    }
    
    /**
     * Apply acceleration and deceleration to the movement
     * @param progress Raw progress from 0.0 to 1.0
     * @return Modified progress with acceleration/deceleration applied
     */
    private float applyAccelerationDeceleration(float progress) {
        // If acceleration is disabled, return linear progress
        if (!GoFishConfig.enableMouseAcceleration) {
            return progress;
        }
        
        // Get acceleration and deceleration phases from config
        float accelPhase = GoFishConfig.accelerationPhase;
        float decelPhase = GoFishConfig.decelerationPhase;
        
        if (progress < accelPhase) {
            // Acceleration phase - ease in
            float accelerationProgress = progress / accelPhase;
            return easeInQuad(accelerationProgress) * accelPhase;
        } else if (progress > (1.0f - decelPhase)) {
            // Deceleration phase - ease out
            float decelerationProgress = (progress - (1.0f - decelPhase)) / decelPhase;
            return (1.0f - decelPhase) + easeOutQuad(decelerationProgress) * decelPhase;
        } else {
            // Constant speed phase - linear
            return progress;
        }
    }
    
    /**
     * Cubic Bezier curve interpolation
     * @param p0 Start point
     * @param p1 First control point
     * @param p2 Second control point
     * @param p3 End point
     * @param t Progress from 0.0 to 1.0
     * @return Interpolated value
     */
    private float cubicBezier(float p0, float p1, float p2, float p3, float t) {
        float oneMinusT = 1 - t;
        return oneMinusT * oneMinusT * oneMinusT * p0 +
               3 * oneMinusT * oneMinusT * t * p1 +
               3 * oneMinusT * t * t * p2 +
               t * t * t * p3;
    }
    
    /**
     * Ease in quadratic function for acceleration
     * @param t Progress from 0.0 to 1.0
     * @return Eased value from 0.0 to 1.0
     */
    private float easeInQuad(float t) {
        return t * t;
    }
    
    /**
     * Ease out quadratic function for deceleration
     * @param t Progress from 0.0 to 1.0
     * @return Eased value from 0.0 to 1.0
     */
    private float easeOutQuad(float t) {
        return 1 - (1 - t) * (1 - t);
    }
    
    /**
     * Ease in/out quadratic function for smooth movement
     * @param t Progress from 0.0 to 1.0
     * @return Eased value from 0.0 to 1.0
     */
    private float easeInOutQuad(float t) {
        return t < 0.5f ? 2 * t * t : 1 - (float)Math.pow(-2 * t + 2, 2) / 2;
    }
    
    /**
     * Reset the original position when fishing stops
     */
    public void resetOriginalPosition() {
        originalPositionSet = false;
        if (GoFishConfig.enableDebugNotifications) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.thePlayer != null) {
                sendDebugMessage(mc, "Original position reset");
            }
        }
    }
    
    /**
     * Add a small random jitter to the movement
     * This simulates hand tremors and imperfect mouse control
     * @param value The base value
     * @param amount The maximum jitter amount
     * @return The value with jitter applied
     */
    private float addJitter(float value, float amount) {
        return value + (random.nextFloat() * 2 - 1) * amount;
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        try {
            // Only process on the client phase END
            if (event.phase != TickEvent.Phase.END) return;
            
            // Check if random look movements are enabled AND auto-catch is enabled
            if (!GoFishConfig.enableRandomLookMovements || !GoFishConfig.enableAutoCatch) return;
            
            // Safety check for Minecraft instance
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) {
                return;
            }
            
            // Check if player is fishing
            boolean isFishing = FishingUtils.isPlayerFishing();
            
            // Reset original position if not fishing
            if (!isFishing && originalPositionSet) {
                resetOriginalPosition();
                return;
            }
            
            // Only apply when fishing
            if (!isFishing) {
                return;
            }
            
            // Initialize next movement time if needed
            if (nextMovementTime == 0) {
                scheduleNextMovement();
                return;
            }
            
            // Handle current movement if active
            if (isMoving) {
                updateMovement(mc);
                return;
            }
            
            // Check if it's time for a new movement
            long currentTime = System.currentTimeMillis();
            if (currentTime >= nextMovementTime) {
                // If we've made several movements, occasionally reset back to original position
                if (movementCounter >= MOVEMENTS_BEFORE_RESET && random.nextFloat() < 0.6f) {
                    // Force a movement back to the original position
                    forceReturnToCenter(mc);
                    movementCounter = 0;
                } else {
                    // Regular random movement
                    startNewMovement(mc);
                    movementCounter++;
                }
            }
        } catch (Exception e) {
            // Log any errors but don't crash the game
            System.err.println("[GoFish] Error in look movement handler: " + e.getMessage());
            e.printStackTrace();
            
            // Reset state to prevent cascading errors
            isMoving = false;
            nextMovementTime = System.currentTimeMillis() + 1000; // Wait a second before trying again
        }
    }
    
    /**
     * Force a return to the original center position
     */
    private void forceReturnToCenter(Minecraft mc) {
        if (mc == null || mc.thePlayer == null || !originalPositionSet) return;
        
        // Get current position
        startYaw = mc.thePlayer.rotationYaw;
        startPitch = mc.thePlayer.rotationPitch;
        
        // Set target to original position
        targetYaw = originalYaw;
        targetPitch = originalPitch;
        
        // Calculate distance to center
        float distanceToCenter = (float) Math.sqrt(
            Math.pow(startYaw - originalYaw, 2) + 
            Math.pow(startPitch - originalPitch, 2)
        );
        
        // If we're already close to center, no need for a centering movement
        if (distanceToCenter < 0.5f) {
            scheduleNextMovement();
            return;
        }
        
        // Straight line movement (less curve for return to center)
        float moveDirection = (float) Math.toDegrees(Math.atan2(originalPitch - startPitch, originalYaw - startYaw));
        
        // Shorter control handles for more direct movement
        controlYaw1 = startYaw + (targetYaw - startYaw) * 0.4f;
        controlPitch1 = startPitch + (targetPitch - startPitch) * 0.4f;
        
        controlYaw2 = startYaw + (targetYaw - startYaw) * 0.6f;
        controlPitch2 = startPitch + (targetPitch - startPitch) * 0.6f;
        
        // Set movement state - slightly faster return to center
        isMoving = true;
        currentProgress = 0;
        movementEndTime = System.currentTimeMillis() + (int)(GoFishConfig.lookMovementDuration * 0.8f);
        
        if (GoFishConfig.enableDebugNotifications) {
            sendDebugMessage(mc, "Returning to center position: " +
                           "Yaw " + String.format("%.2f", startYaw) + " -> " + String.format("%.2f", targetYaw) + ", " +
                           "Pitch " + String.format("%.2f", startPitch) + " -> " + String.format("%.2f", targetPitch) +
                           " (distance: " + String.format("%.2f", distanceToCenter) + ")");
        }
    }
} 