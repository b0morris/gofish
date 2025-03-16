package com.example.gofish.handlers;

import com.example.gofish.config.GoFishConfig;
import com.example.gofish.utils.FishingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatHandler {

    // Debug mode for development
    private static final boolean DEBUG_MODE = true;

    // Patterns for Hypixel SkyBlock fishing messages - more flexible patterns
    private static final Pattern FISH_CATCH_PATTERN = Pattern.compile("(?:§r)?§a(?:You caught a |You caught an )(?:§r)?§[0-9a-f](.+?)(?:§r)?§a!");
    private static final Pattern SEA_CREATURE_PATTERN = Pattern.compile("(?:§r)?§a(?:A |An )(.+?)(?:§r)?§a surfaces!");
    private static final Pattern TREASURE_PATTERN = Pattern.compile("(?:§r)?§a(?:You found a |You found an )(?:§r)?§[0-9a-f](.+?)(?:§r)?§a!");
    
    // Sound resources
    private static final ResourceLocation FISH_CAUGHT_SOUND = new ResourceLocation("random.orb");
    private static final ResourceLocation SEA_CREATURE_SOUND = new ResourceLocation("mob.guardian.elder.hit");
    
    @SubscribeEvent
    public void onChatMessage(ClientChatReceivedEvent event) {
        try {
            // Only process chat messages (type 0)
            if (event.type != 0) return;
            
            // Safety check for Minecraft instance
            if (Minecraft.getMinecraft() == null || Minecraft.getMinecraft().thePlayer == null) {
                return;
            }
            
            // Only process if notifications are enabled
            if (!GoFishConfig.enableNotifications) return;
            
            // Use a more relaxed check - just verify we're on Hypixel
            if (!FishingUtils.isOnHypixel()) return;
            
            String message = event.message.getFormattedText();
            
            if (DEBUG_MODE) {
                // Log raw chat messages for debugging
                System.out.println("[GoFish] Raw chat message: " + message);
            }
            
            // Check if the message matches any of our fishing patterns
            Matcher fishCatchMatcher = FISH_CATCH_PATTERN.matcher(message);
            Matcher seaCreatureMatcher = SEA_CREATURE_PATTERN.matcher(message);
            Matcher treasureMatcher = TREASURE_PATTERN.matcher(message);
            
            if (fishCatchMatcher.find()) {
                String fishName = fishCatchMatcher.group(1);
                handleFishCaught(fishName);
            } else if (seaCreatureMatcher.find()) {
                String creatureName = seaCreatureMatcher.group(1);
                handleSeaCreature(creatureName);
            } else if (treasureMatcher.find()) {
                String treasureName = treasureMatcher.group(1);
                handleTreasure(treasureName);
            }
        } catch (Exception e) {
            // Log any errors but don't crash the game
            System.err.println("[GoFish] Error processing chat message: " + e.getMessage());
        }
    }
    
    /**
     * Handle when a fish is caught
     */
    private void handleFishCaught(String fishName) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;
            
            if (DEBUG_MODE) {
                System.out.println("[GoFish] Fish caught: " + fishName);
            }
            
            // Show message if enabled
            if (GoFishConfig.showFishCaughtMessages) {
                mc.thePlayer.addChatMessage(new ChatComponentText("§b[GoFish] §fCaught: §e" + fishName));
            }
            
            // Play sound if enabled
            if (GoFishConfig.playSoundOnFishCaught) {
                mc.thePlayer.playSound(FISH_CAUGHT_SOUND.toString(), 1.0F, 1.0F);
            }
        } catch (Exception e) {
            System.err.println("[GoFish] Error handling fish caught: " + e.getMessage());
        }
    }
    
    /**
     * Handle when a sea creature appears
     */
    private void handleSeaCreature(String creatureName) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;
            
            if (DEBUG_MODE) {
                System.out.println("[GoFish] Sea creature: " + creatureName);
            }
            
            // Show message if enabled
            if (GoFishConfig.showSeaCreatureMessages) {
                mc.thePlayer.addChatMessage(new ChatComponentText("§b[GoFish] §fSea Creature: §c" + creatureName));
            }
            
            // Play sound if enabled
            if (GoFishConfig.playSoundOnSeaCreature) {
                mc.thePlayer.playSound(SEA_CREATURE_SOUND.toString(), 1.0F, 1.0F);
            }
        } catch (Exception e) {
            System.err.println("[GoFish] Error handling sea creature: " + e.getMessage());
        }
    }
    
    /**
     * Handle when treasure is found
     */
    private void handleTreasure(String treasureName) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;
            
            if (DEBUG_MODE) {
                System.out.println("[GoFish] Treasure: " + treasureName);
            }
            
            // Show message if enabled
            if (GoFishConfig.showTreasureMessages) {
                mc.thePlayer.addChatMessage(new ChatComponentText("§b[GoFish] §fTreasure: §d" + treasureName));
            }
            
            // Always play a sound for treasure
            mc.thePlayer.playSound("random.levelup", 1.0F, 1.0F);
        } catch (Exception e) {
            System.err.println("[GoFish] Error handling treasure: " + e.getMessage());
        }
    }
} 