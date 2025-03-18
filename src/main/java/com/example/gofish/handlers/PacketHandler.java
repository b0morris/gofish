package com.example.gofish.handlers;

import com.example.gofish.config.GoFishConfig;
import com.example.gofish.utils.FishingUtils;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.network.play.server.S2CPacketSpawnGlobalEntity;
import net.minecraft.network.play.server.S0EPacketSpawnObject;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class PacketHandler {
    
    // Constants for packet analysis
    private static final int FISHING_BOBBER_ENTITY_ID = 90;
    private static final String SPLASH_PARTICLE_NAME = "splash";
    private static final String WAKE_PARTICLE_NAME = "wake";
    private static final String WATER_BUBBLE_PARTICLE_NAME = "bubble";
    private static final String FISHING_HOOK_SOUND = "random.splash";
    private static final String FISH_CAUGHT_MESSAGE = "&r&aYou caught a &r";
    private static final String SPLASH_SOUND = "random.splash";
    private static final String WATER_SPLASH_SOUND = "game.player.swim.splash";
    
    // Packet logging state
    private boolean isLoggingPackets = false;
    private long packetLoggingStartTime = 0;
    private static final long packetLoggingDuration = 10000; // 10 seconds
    private Set<String> loggedPacketTypes = new HashSet<>();
    
    // Fishing state
    private long lastHookCastTime = 0;
    private long lastBiteTime = 0;
    private int fishingHookEntityId = -1;
    
    // Reference to the fishing handler for callbacks
    private final FishingHandler fishingHandler;
    
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
     * Constructor
     * @param fishingHandler The fishing handler to notify when a fish bites
     */
    public PacketHandler(FishingHandler fishingHandler) {
        this.fishingHandler = fishingHandler;
    }
    
    /**
     * Default constructor for backward compatibility
     */
    public PacketHandler() {
        this.fishingHandler = null;
    }
    
    /**
     * Helper method to safely get entity ID from any packet using reflection
     */
    private int getEntityIdFromPacket(Object packet) {
        try {
            // Try common method names first
            try {
                return (int) packet.getClass().getMethod("getEntityID").invoke(packet);
            } catch (Exception e1) {
                try {
                    return (int) packet.getClass().getMethod("getEntityId").invoke(packet);
                } catch (Exception e2) {
                    // If methods fail, try to find a field with "entityId" in the name
                    for (Field field : packet.getClass().getDeclaredFields()) {
                        if (field.getName().toLowerCase().contains("entityid") || 
                            field.getName().toLowerCase().contains("entity_id") ||
                            field.getName().toLowerCase().contains("eid")) {
                            field.setAccessible(true);
                            return (int) field.get(packet);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silently fail and return -1
        }
        return -1;
    }
    
    @SubscribeEvent
    public void onClientConnectedToServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        try {
            // Add our packet interceptor when client connects to server
            event.manager.channel().pipeline().addBefore("packet_handler", "gofish_packet_handler", new FishingPacketHandler());
            
            // Only try to send a message if the player is not null
            if (GoFishConfig.enableDebugNotifications && Minecraft.getMinecraft() != null && Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(
                    new ChatComponentText(formatColorCodes("&b[GoFish] &fPacket handler initialized"))
                );
            }
        } catch (Exception e) {
            // Log any errors that occur during initialization
            System.err.println(formatColorCodes("[GoFish] Error initializing packet handler: " + e.getMessage()));
            e.printStackTrace();
        }
    }
    
    @SubscribeEvent
    public void onClientDisconnectionFromServer(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        try {
            // Clean up when client disconnects
            if (event.manager.channel().pipeline().get("gofish_packet_handler") != null) {
                event.manager.channel().pipeline().remove("gofish_packet_handler");
            }
        } catch (Exception e) {
            // Log any errors that occur during cleanup
            System.err.println(formatColorCodes("[GoFish] Error removing packet handler: " + e.getMessage()));
        }
    }
    
    /**
     * Update the hook cast time to avoid false detections
     */
    public void updateHookCastTime() {
        lastHookCastTime = System.currentTimeMillis();
        if (GoFishConfig.enableDebugNotifications) {
            System.out.println(formatColorCodes("[GoFish] Hook cast time updated"));
        }
        
        // Update fishing hook entity ID
        updateFishingHookEntityId();
    }
    
    /**
     * Update the fishing hook entity ID for packet analysis
     */
    private void updateFishingHookEntityId() {
        try {
            // Get the fishing hook entity
            net.minecraft.entity.projectile.EntityFishHook hook = FishingUtils.getFishingHook();
            if (hook != null) {
                fishingHookEntityId = hook.getEntityId();
                if (GoFishConfig.enableDebugNotifications) {
                    System.out.println(formatColorCodes("[GoFish] Fishing hook entity ID updated: " + fishingHookEntityId));
                }
            }
        } catch (Exception e) {
            System.err.println(formatColorCodes("[GoFish] Error updating fishing hook entity ID: " + e.getMessage()));
        }
    }
    
    /**
     * Start logging packets for a short duration
     */
    public void startPacketLogging() {
        packetLoggingStartTime = System.currentTimeMillis();
        if (GoFishConfig.enableDebugNotifications && Minecraft.getMinecraft() != null && Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(
                new ChatComponentText(formatColorCodes("&b[GoFish] &fStarted packet logging for " + (packetLoggingDuration / 1000) + " seconds"))
            );
        }
    }
    
    /**
     * Log packet types during the logging duration
     */
    private void logPacketType(Packet packet) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - packetLoggingStartTime < packetLoggingDuration) {
            String packetType = packet.getClass().getSimpleName();
            
            // Only log each packet type once to avoid spam
            if (!loggedPacketTypes.contains(packetType)) {
                loggedPacketTypes.add(packetType);
                System.out.println(formatColorCodes("[GoFish] Detected packet type: " + packetType));
                
                // Also show in chat if debug mode is on
                if (GoFishConfig.enableDebugNotifications && Minecraft.getMinecraft() != null && Minecraft.getMinecraft().thePlayer != null) {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText(formatColorCodes("&b[GoFish] &fDetected packet: &e" + packetType))
                    );
                }
            }
            
            // Log detailed information for specific packet types
            logDetailedPacketInfo(packet);
        }
    }
    
    /**
     * Log detailed information for specific packet types that might be related to fishing
     */
    private void logDetailedPacketInfo(Packet packet) {
        try {
            if (packet instanceof S29PacketSoundEffect) {
                S29PacketSoundEffect soundPacket = (S29PacketSoundEffect) packet;
                String soundName = soundPacket.getSoundName();
                float volume = soundPacket.getVolume();
                float pitch = soundPacket.getPitch();
                
                System.out.println(formatColorCodes("[GoFish] Sound packet: " + soundName + 
                                   " (volume=" + volume + ", pitch=" + pitch + ")"));
                
                // Also show in chat if debug mode is on
                if (GoFishConfig.enableDebugNotifications && Minecraft.getMinecraft() != null && Minecraft.getMinecraft().thePlayer != null) {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText(formatColorCodes("&b[GoFish] &fSound: &e" + soundName + 
                                             " &f(volume=" + volume + ", pitch=" + pitch + ")"))
                    );
                }
            } else if (packet instanceof S2APacketParticles) {
                S2APacketParticles particlePacket = (S2APacketParticles) packet;
                EnumParticleTypes particleType = particlePacket.getParticleType();
                
                System.out.println(formatColorCodes("[GoFish] Particle packet: " + particleType.getParticleName()));
                
                // Also show in chat if debug mode is on
                if (GoFishConfig.enableDebugNotifications && Minecraft.getMinecraft() != null && Minecraft.getMinecraft().thePlayer != null) {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText(formatColorCodes("&b[GoFish] &fParticle: &e" + particleType.getParticleName()))
                    );
                }
            } else if (packet instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity velocityPacket = (S12PacketEntityVelocity) packet;
                int entityId = velocityPacket.getEntityID();
                double motionX = velocityPacket.getMotionX() / 8000.0;
                double motionY = velocityPacket.getMotionY() / 8000.0;
                double motionZ = velocityPacket.getMotionZ() / 8000.0;
                
                System.out.println(formatColorCodes("[GoFish] Velocity packet: entityId=" + entityId + 
                                   " (motionX=" + String.format("%.2f", motionX) + 
                                   ", motionY=" + String.format("%.2f", motionY) + 
                                   ", motionZ=" + String.format("%.2f", motionZ) + ")"));
                
                // Also show in chat if debug mode is on
                if (GoFishConfig.enableDebugNotifications && Minecraft.getMinecraft() != null && Minecraft.getMinecraft().thePlayer != null) {
                    Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText(formatColorCodes("&b[GoFish] &fVelocity: entityId=" + entityId + 
                                             " &f(motionY=" + String.format("%.2f", motionY) + ")"))
                    );
                }
            }
        } catch (Exception e) {
            // Silently ignore errors in packet logging
        }
    }
    
    /**
     * Inner class to handle packet interception
     */
    private class FishingPacketHandler extends ChannelDuplexHandler {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            try {
                // Process incoming packets
                if (msg instanceof Packet && Minecraft.getMinecraft() != null && Minecraft.getMinecraft().thePlayer != null) {
                    // Log packet types if logging is active
                    if (System.currentTimeMillis() - packetLoggingStartTime < packetLoggingDuration) {
                        logPacketType((Packet) msg);
                    }
                    
                    // Use a more relaxed check - just verify we're on Hypixel
                    if (FishingUtils.isOnHypixel() && isFishingPacket(msg)) {
                        handleFishingPacket((Packet) msg);
                    }
                }
            } catch (Exception e) {
                // Log any errors but don't crash the game
                System.err.println(formatColorCodes("[GoFish] Error processing packet: " + e.getMessage()));
            }
            
            // Pass the packet along the pipeline
            super.channelRead(ctx, msg);
        }
        
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            // Process outgoing packets if needed
            super.write(ctx, msg, promise);
        }
        
        /**
         * Determine if a packet is related to fishing
         */
        private boolean isFishingPacket(Object packet) {
            // Safety check
            if (Minecraft.getMinecraft() == null || Minecraft.getMinecraft().thePlayer == null) {
                return false;
            }
            
            // Only process fishing packets if the player is actually fishing
            if (!FishingUtils.isPlayerFishing()) {
                // If player just started fishing, update the cast time
                if (packet instanceof S29PacketSoundEffect) {
                    S29PacketSoundEffect soundPacket = (S29PacketSoundEffect) packet;
                    String soundName = soundPacket.getSoundName();
                    
                    // If we hear a splash and we're not currently fishing, it might be a new cast
                    if ((soundName.equals(SPLASH_SOUND) || soundName.equals(WATER_SPLASH_SOUND))) {
                        updateHookCastTime();
                    }
                }
                return false;
            }
            
            // Only process if notifications are enabled
            if (!GoFishConfig.enableNotifications) {
                return false;
            }
            
            // Ignore packets that are too close to the cast time
            if (System.currentTimeMillis() - lastHookCastTime < GoFishConfig.castIgnoreTime) {
                return false;
            }
            
            // Check for splash sound effect packet
            if (packet instanceof S29PacketSoundEffect) {
                S29PacketSoundEffect soundPacket = (S29PacketSoundEffect) packet;
                String soundName = soundPacket.getSoundName();
                
                // Check for various splash sounds that might indicate a fish bite
                return soundName.equals(SPLASH_SOUND) || soundName.equals(WATER_SPLASH_SOUND);
            }
            
            // Check for water splash particles
            if (packet instanceof S2APacketParticles) {
                S2APacketParticles particlePacket = (S2APacketParticles) packet;
                
                // Water splash particles often appear when a fish bites
                if (particlePacket.getParticleType() == EnumParticleTypes.WATER_SPLASH) {
                    return true;
                }
            }
            
            // Check for entity velocity changes for the fishing hook
            if (packet instanceof S12PacketEntityVelocity && fishingHookEntityId != -1) {
                S12PacketEntityVelocity velocityPacket = (S12PacketEntityVelocity) packet;
                if (velocityPacket.getEntityID() == fishingHookEntityId) {
                    // If the hook has significant vertical velocity, it might be a bite
                    double motionY = velocityPacket.getMotionY() / 8000.0;
                    if (Math.abs(motionY) > 0.2) {
                        return true;
                    }
                }
            }
            
            return false;
        }
        
        /**
         * Handle fishing-related packets
         */
        private void handleFishingPacket(Packet packet) {
            // Safety check
            if (Minecraft.getMinecraft() == null || Minecraft.getMinecraft().thePlayer == null) {
                return;
            }
            
            // Check for cooldown to prevent spam
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastBiteTime < GoFishConfig.biteCooldown) {
                return;
            }
            
            if (packet instanceof S29PacketSoundEffect) {
                S29PacketSoundEffect soundPacket = (S29PacketSoundEffect) packet;
                
                // Get the coordinates of the sound
                double x = soundPacket.getX();
                double y = soundPacket.getY();
                double z = soundPacket.getZ();
                
                // Get fishing hook position
                net.minecraft.entity.projectile.EntityFishHook fishHook = FishingUtils.getFishingHook();
                if (fishHook == null) return;
                
                double hookX = fishHook.posX;
                double hookY = fishHook.posY;
                double hookZ = fishHook.posZ;
                
                // Calculate distance between sound and fishing hook
                double distance = Math.sqrt(
                    Math.pow(x - hookX, 2) + 
                    Math.pow(y - hookY, 2) + 
                    Math.pow(z - hookZ, 2)
                );
                
                // If the splash is close to the fishing hook (likely a fish bite)
                // Use a larger detection radius for Hypixel
                if (distance < 2.0) {
                    lastBiteTime = currentTime;
                    
                    if (GoFishConfig.enableDebugNotifications) {
                        Minecraft.getMinecraft().thePlayer.addChatMessage(
                            new ChatComponentText(formatColorCodes("&b[GoFish] &fDetected splash near hook: distance=" + 
                                                 String.format("%.2f", distance)))
                        );
                    }
                    
                    // Only show notification if enabled
                    if (GoFishConfig.showFishCaughtMessages) {
                        // Notify the player that a fish was caught
                        Minecraft.getMinecraft().thePlayer.addChatMessage(
                            new ChatComponentText(formatColorCodes("&b[GoFish] &fFish on the hook! Reel it in!"))
                        );
                    }
                    
                    // Play sound if enabled
                    if (GoFishConfig.playSoundOnFishCaught) {
                        Minecraft.getMinecraft().thePlayer.playSound("random.orb", 1.0F, 1.0F);
                    }
                    
                    // Call the fishing handler's onFishBite method
                    if (fishingHandler != null) {
                        fishingHandler.onFishBite();
                    }
                }
            } else if (packet instanceof S2APacketParticles) {
                S2APacketParticles particlePacket = (S2APacketParticles) packet;
                
                // Get the coordinates of the particles
                double x = particlePacket.getXCoordinate();
                double y = particlePacket.getYCoordinate();
                double z = particlePacket.getZCoordinate();
                
                // Get fishing hook position
                net.minecraft.entity.projectile.EntityFishHook fishHook = FishingUtils.getFishingHook();
                if (fishHook == null) return;
                
                double hookX = fishHook.posX;
                double hookY = fishHook.posY;
                double hookZ = fishHook.posZ;
                
                // Calculate distance between particles and fishing hook
                double distance = Math.sqrt(
                    Math.pow(x - hookX, 2) + 
                    Math.pow(y - hookY, 2) + 
                    Math.pow(z - hookZ, 2)
                );
                
                // If the particles are close to the fishing hook (likely a fish bite)
                if (distance < 1.5) {
                    lastBiteTime = currentTime;
                    
                    if (GoFishConfig.enableDebugNotifications) {
                        Minecraft.getMinecraft().thePlayer.addChatMessage(
                            new ChatComponentText(formatColorCodes("&b[GoFish] &fDetected particles near hook: distance=" + 
                                                 String.format("%.2f", distance)))
                        );
                    }
                    
                    // Only show notification if enabled
                    if (GoFishConfig.showFishCaughtMessages) {
                        // Notify the player that a fish was caught
                        Minecraft.getMinecraft().thePlayer.addChatMessage(
                            new ChatComponentText(formatColorCodes("&b[GoFish] &fFish on the hook! Reel it in!"))
                        );
                    }
                    
                    // Play sound if enabled
                    if (GoFishConfig.playSoundOnFishCaught) {
                        Minecraft.getMinecraft().thePlayer.playSound("random.orb", 1.0F, 1.0F);
                    }
                    
                    // Call the fishing handler's onFishBite method
                    if (fishingHandler != null) {
                        fishingHandler.onFishBite();
                    }
                }
            } else if (packet instanceof S12PacketEntityVelocity && fishingHookEntityId != -1) {
                S12PacketEntityVelocity velocityPacket = (S12PacketEntityVelocity) packet;
                
                if (velocityPacket.getEntityID() == fishingHookEntityId) {
                    double motionY = velocityPacket.getMotionY() / 8000.0;
                    
                    // If the hook has significant vertical velocity, it might be a bite
                    if (Math.abs(motionY) > 0.2) {
                        lastBiteTime = currentTime;
                        
                        if (GoFishConfig.enableDebugNotifications) {
                            Minecraft.getMinecraft().thePlayer.addChatMessage(
                                new ChatComponentText(formatColorCodes("&b[GoFish] &fDetected hook velocity change: motionY=" + 
                                                     String.format("%.2f", motionY)))
                            );
                        }
                        
                        // Only show notification if enabled
                        if (GoFishConfig.showFishCaughtMessages) {
                            // Notify the player that a fish was caught
                            Minecraft.getMinecraft().thePlayer.addChatMessage(
                                new ChatComponentText(formatColorCodes("&b[GoFish] &fFish on the hook! Reel it in!"))
                            );
                        }
                        
                        // Play sound if enabled
                        if (GoFishConfig.playSoundOnFishCaught) {
                            Minecraft.getMinecraft().thePlayer.playSound("random.orb", 1.0F, 1.0F);
                        }
                        
                        // Call the fishing handler's onFishBite method
                        if (fishingHandler != null) {
                            fishingHandler.onFishBite();
                        }
                    }
                }
            }
        }
    }
} 