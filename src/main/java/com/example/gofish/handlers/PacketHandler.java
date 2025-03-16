package com.example.gofish.handlers;

import com.example.gofish.config.GoFishConfig;
import com.example.gofish.utils.FishingUtils;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

public class PacketHandler {
    
    // Sound constants for fishing detection
    private static final String SPLASH_SOUND = "random.splash";
    private static final String WATER_SPLASH_SOUND = "game.player.swim.splash";
    private static final String FISH_CAUGHT_MESSAGE = "§r§aYou caught a §r";
    
    // Debug mode for development
    private static final boolean DEBUG_MODE = true;
    
    // Track hook cast time to avoid false detections
    private long lastHookCastTime = 0;
    private long lastBiteTime = 0;
    private static final long CAST_IGNORE_TIME = 2000; // Ignore detections for 2 seconds after casting
    private static final long BITE_COOLDOWN = 1000; // 1 second cooldown between bite detections
    
    @SubscribeEvent
    public void onClientConnectedToServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        try {
            // Add our packet interceptor when client connects to server
            event.manager.channel().pipeline().addBefore("packet_handler", "gofish_packet_handler", new FishingPacketHandler());
            
            // Only try to send a message if the player is not null
            if (DEBUG_MODE && Minecraft.getMinecraft() != null && Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(
                    new ChatComponentText("§b[GoFish] §fPacket handler initialized")
                );
            }
        } catch (Exception e) {
            // Log any errors that occur during initialization
            System.err.println("[GoFish] Error initializing packet handler: " + e.getMessage());
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
            System.err.println("[GoFish] Error removing packet handler: " + e.getMessage());
        }
    }
    
    /**
     * Update the hook cast time to avoid false detections
     */
    public void updateHookCastTime() {
        lastHookCastTime = System.currentTimeMillis();
        if (DEBUG_MODE) {
            System.out.println("[GoFish] Hook cast time updated");
        }
    }
    
    /**
     * Custom packet handler to intercept and process network packets
     */
    private class FishingPacketHandler extends ChannelDuplexHandler {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            try {
                // Process incoming packets
                if (msg instanceof Packet && Minecraft.getMinecraft() != null && Minecraft.getMinecraft().thePlayer != null) {
                    // Use a more relaxed check - just verify we're on Hypixel
                    if (FishingUtils.isOnHypixel() && isFishingPacket(msg)) {
                        handleFishingPacket((Packet) msg);
                    }
                }
            } catch (Exception e) {
                // Log any errors but don't crash the game
                System.err.println("[GoFish] Error processing packet: " + e.getMessage());
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
            if (System.currentTimeMillis() - lastHookCastTime < CAST_IGNORE_TIME) {
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
            if (currentTime - lastBiteTime < BITE_COOLDOWN) {
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
                if (distance < 5.0) {
                    lastBiteTime = currentTime;
                    
                    if (DEBUG_MODE) {
                        Minecraft.getMinecraft().thePlayer.addChatMessage(
                            new ChatComponentText("§b[GoFish] §fDetected sound: " + soundPacket.getSoundName() + 
                                                 " at distance: " + String.format("%.2f", distance))
                        );
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
            } else if (packet instanceof S2APacketParticles) {
                S2APacketParticles particlePacket = (S2APacketParticles) packet;
                
                if (particlePacket.getParticleType() == EnumParticleTypes.WATER_SPLASH) {
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
                    
                    // If the particles are close to the fishing hook
                    if (distance < 5.0) {
                        lastBiteTime = currentTime;
                        
                        if (DEBUG_MODE) {
                            Minecraft.getMinecraft().thePlayer.addChatMessage(
                                new ChatComponentText("§b[GoFish] §fDetected water splash particles at distance: " + 
                                                     String.format("%.2f", distance))
                            );
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
            }
        }
    }
} 