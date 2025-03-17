package com.example.gofish.config;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;

/**
 * Configuration handler for the GoFish mod
 */
public class GoFishConfig {
    
    private static Configuration config;
    
    // General settings
    public static boolean enableNotifications = true;
    public static boolean playSoundOnFishCaught = true;
    public static boolean playSoundOnSeaCreature = true;
    public static boolean enablePacketLogging = false;
    
    // Notification settings
    public static boolean showFishCaughtMessages = true;
    public static boolean showSeaCreatureMessages = true;
    public static boolean showTreasureMessages = true;
    
    // Auto-catch settings
    public static boolean enableAutoCatch = false;
    public static int minCatchDelay = 80;
    public static int maxCatchDelay = 500;
    
    /**
     * Initialize the configuration
     * @param configFile The configuration file
     */
    public static void init(File configFile) {
        try {
            if (configFile == null) {
                System.err.println("[GoFish] Config file is null, using default settings");
                return;
            }
            
            config = new Configuration(configFile);
            loadConfig();
        } catch (Exception e) {
            System.err.println("[GoFish] Error initializing config: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Load the configuration from file
     */
    public static void loadConfig() {
        try {
            if (config == null) {
                System.err.println("[GoFish] Config is null, using default settings");
                return;
            }
            
            config.load();
            
            // General settings
            Property enableNotificationsProp = config.get(
                Configuration.CATEGORY_GENERAL, 
                "enableNotifications", 
                true, 
                "Enable in-game notifications for fishing events"
            );
            enableNotifications = enableNotificationsProp.getBoolean();
            
            Property playSoundOnFishCaughtProp = config.get(
                Configuration.CATEGORY_GENERAL, 
                "playSoundOnFishCaught", 
                true, 
                "Play a sound when a fish is caught"
            );
            playSoundOnFishCaught = playSoundOnFishCaughtProp.getBoolean();
            
            Property playSoundOnSeaCreatureProp = config.get(
                Configuration.CATEGORY_GENERAL, 
                "playSoundOnSeaCreature", 
                true, 
                "Play a sound when a sea creature appears"
            );
            playSoundOnSeaCreature = playSoundOnSeaCreatureProp.getBoolean();
            
            Property enablePacketLoggingProp = config.get(
                Configuration.CATEGORY_GENERAL, 
                "enablePacketLogging", 
                false, 
                "Enable packet logging for fishing-related packets (can be toggled with a key binding)"
            );
            enablePacketLogging = enablePacketLoggingProp.getBoolean();
            
            // Notification settings
            Property showFishCaughtMessagesProp = config.get(
                "notifications", 
                "showFishCaughtMessages", 
                true, 
                "Show messages when fish are caught"
            );
            showFishCaughtMessages = showFishCaughtMessagesProp.getBoolean();
            
            Property showSeaCreatureMessagesProp = config.get(
                "notifications", 
                "showSeaCreatureMessages", 
                true, 
                "Show messages when sea creatures appear"
            );
            showSeaCreatureMessages = showSeaCreatureMessagesProp.getBoolean();
            
            Property showTreasureMessagesProp = config.get(
                "notifications", 
                "showTreasureMessages", 
                true, 
                "Show messages when treasure is found"
            );
            showTreasureMessages = showTreasureMessagesProp.getBoolean();
            
            // Auto-catch settings
            Property enableAutoCatchProp = config.get(
                "autocatch",
                "enableAutoCatch",
                false,
                "Automatically catch fish when detected (use at your own risk)"
            );
            enableAutoCatch = enableAutoCatchProp.getBoolean();
            
            Property minCatchDelayProp = config.get(
                "autocatch",
                "minCatchDelay",
                80,
                "Minimum delay in milliseconds before auto-catching (30-1000)",
                30,
                1000
            );
            minCatchDelay = minCatchDelayProp.getInt();
            
            Property maxCatchDelayProp = config.get(
                "autocatch",
                "maxCatchDelay",
                300,
                "Maximum delay in milliseconds before auto-catching (must be greater than minCatchDelay)",
                31,
                2000
            );
            maxCatchDelay = maxCatchDelayProp.getInt();
            
            // Validate catch delay settings
            if (maxCatchDelay <= minCatchDelay) {
                maxCatchDelay = minCatchDelay + 1;
                maxCatchDelayProp.set(maxCatchDelay);
            }
            
            if (config.hasChanged()) {
                config.save();
            }
        } catch (Exception e) {
            System.err.println("[GoFish] Error loading config: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Save the configuration to file
     */
    public static void saveConfig() {
        try {
            if (config == null) {
                System.err.println("[GoFish] Cannot save config, config is null");
                return;
            }
            
            config.save();
        } catch (Exception e) {
            System.err.println("[GoFish] Error saving config: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 