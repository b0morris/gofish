package com.example.gofish.config;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.Properties;

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
    
    // Miss chance settings - sometimes miss the fish on purpose for realism
    public static boolean enableMissChance = true;
    public static int missChancePercentage = 10; // 10% chance to miss
    public static int minMissTimingOffset = -500; // ms to pull too early (negative)
    public static int maxMissTimingOffset = 800;  // ms to pull too late (positive)
    
    // Auto-recast settings
    public static boolean enableAutoRecast = true;
    public static int minRecastDelay = 0;
    public static int maxRecastDelay = 1000;
    
    // Shift key settings
    public static boolean enablePeriodicShift = true;
    public static int minShiftInterval = 20000; // 20 seconds
    public static int maxShiftInterval = 60000; // 60 seconds
    public static int minShiftDuration = 2000;  // 2 seconds
    public static int maxShiftDuration = 4000;  // 4 seconds
    
    // Periodic jump settings (for appearing human-like)
    public static boolean enablePeriodicJump = true;
    public static int minJumpInterval = 120000; // 2 minutes
    public static int maxJumpInterval = 240000; // 4 minutes
    public static int jumpDuration = 500;       // 0.5 seconds
    
    // Advanced settings
    public static int castIgnoreTime = 2000; // Time in ms to ignore detections after casting
    public static int minCastIgnoreTime = 500; // Minimum cast ignore time in ms
    public static int maxCastIgnoreTime = 5000; // Maximum cast ignore time in ms
    public static boolean useRandomCastIgnoreTime = true; // Use random cast ignore time between min and max
    public static int biteCooldown = 1000; // Cooldown between bite detections in ms
    
    // Random look movement settings
    public static boolean enableRandomLookMovements = false;
    public static float randomLookFovRange = 5.0f; // Degrees of FOV for random movements
    public static int minLookMovementDelay = 1000; // Minimum delay between movements (ms)
    public static int maxLookMovementDelay = 4000; // Maximum delay between movements (ms)
    public static int lookMovementDuration = 500; // Duration of each movement (ms)
    public static boolean enableMouseAcceleration = true; // Enable acceleration/deceleration for mouse movements
    public static float accelerationPhase = 0.3f; // Percentage of movement for acceleration (0.0-1.0)
    public static float decelerationPhase = 0.3f; // Percentage of movement for deceleration (0.0-1.0)
    
    // Store the config file reference
    private static File configFileRef;
    
    // Debug settings
    public static boolean enableDebugNotifications = false;
    
    // Safety settings
    public static boolean enableSafetyFeatures = true;
    public static float positionSafetyThreshold = 0.1f;
    public static float rotationSafetyThreshold = 5.0f;
    
    // Safety features
    public static boolean enablePositionSafety = true;
    public static boolean enableRotationSafety = true;
    public static double maxPositionDifference = 0.5;
    public static double maxRotationDifference = 5.0;
    
    // Liquid detection safety feature
    public static boolean enableLiquidDetection = true;   // Toggle for liquid detection safety
    public static int maxLiquidFailures = 2;             // Number of failed liquid casts before disabling auto-catch
    
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
            
            configFileRef = configFile;
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
                Configuration.CATEGORY_GENERAL, 
                "enableAutoCatch",
                false,
                "Enable auto-catching of fish"
            );
            enableAutoCatch = enableAutoCatchProp.getBoolean();
            
            Property minCatchDelayProp = config.get(
                Configuration.CATEGORY_GENERAL, 
                "minCatchDelay",
                80,
                "Minimum delay in milliseconds before catching a fish (0-10000)",
                0,
                10000
            );
            minCatchDelay = minCatchDelayProp.getInt();
            
            Property maxCatchDelayProp = config.get(
                Configuration.CATEGORY_GENERAL, 
                "maxCatchDelay",
                500, 
                "Maximum delay in milliseconds before catching a fish (must be greater than minCatchDelay)",
                minCatchDelay + 1,
                Integer.MAX_VALUE
            );
            maxCatchDelay = maxCatchDelayProp.getInt();
            
            // Miss chance settings
            Property enableMissChanceProp = config.get(
                "misschance",
                "enableMissChance",
                true,
                "Enable occasional missed fish for more realistic behavior"
            );
            enableMissChance = enableMissChanceProp.getBoolean();
            
            Property missChancePercentageProp = config.get(
                "misschance",
                "missChancePercentage",
                10,
                "Percentage chance to intentionally miss a fish (0-100)",
                0,
                100
            );
            missChancePercentage = missChancePercentageProp.getInt();
            
            Property minMissTimingOffsetProp = config.get(
                "misschance",
                "minMissTimingOffset",
                -500,
                "Minimum timing offset in milliseconds (negative = too early, positive = too late)",
                -10000,
                0
            );
            minMissTimingOffset = minMissTimingOffsetProp.getInt();
            
            Property maxMissTimingOffsetProp = config.get(
                "misschance",
                "maxMissTimingOffset",
                800,
                "Maximum timing offset in milliseconds (should be positive for late catches)",
                1,
                10000
            );
            maxMissTimingOffset = maxMissTimingOffsetProp.getInt();
            
            // Auto-recast settings
            Property enableAutoRecastProp = config.get(
                Configuration.CATEGORY_GENERAL, 
                "enableAutoRecast", 
                true, 
                "Enable auto-recasting of fishing rod"
            );
            enableAutoRecast = enableAutoRecastProp.getBoolean();
            
            Property minRecastDelayProp = config.get(
                Configuration.CATEGORY_GENERAL, 
                "minRecastDelay", 
                0, 
                "Minimum delay in milliseconds before recasting (0-10000)",
                0,
                10000
            );
            minRecastDelay = minRecastDelayProp.getInt();
            
            Property maxRecastDelayProp = config.get(
                Configuration.CATEGORY_GENERAL, 
                "maxRecastDelay", 
                1000, 
                "Maximum delay in milliseconds before recasting (must be greater than minRecastDelay)",
                minRecastDelay + 1,
                Integer.MAX_VALUE
            );
            maxRecastDelay = maxRecastDelayProp.getInt();
            
            // Cast ignore time settings
            Property castIgnoreTimeProp = config.get(
                Configuration.CATEGORY_GENERAL, 
                "castIgnoreTime", 
                2000, 
                "Time in milliseconds to ignore detections after casting (500-10000)",
                500,
                10000
            );
            castIgnoreTime = castIgnoreTimeProp.getInt();
            
            Property minCastIgnoreTimeProp = config.get(
                Configuration.CATEGORY_GENERAL, 
                "minCastIgnoreTime", 
                500, 
                "Minimum time in milliseconds to ignore detections after casting (100-10000)",
                100,
                10000
            );
            minCastIgnoreTime = minCastIgnoreTimeProp.getInt();
            
            Property maxCastIgnoreTimeProp = config.get(
                Configuration.CATEGORY_GENERAL, 
                "maxCastIgnoreTime", 
                5000, 
                "Maximum time in milliseconds to ignore detections after casting (must be greater than minCastIgnoreTime)",
                minCastIgnoreTime + 1,
                Integer.MAX_VALUE
            );
            maxCastIgnoreTime = maxCastIgnoreTimeProp.getInt();
            
            Property useRandomCastIgnoreTimeProp = config.get(
                Configuration.CATEGORY_GENERAL,
                "useRandomCastIgnoreTime",
                true,
                "Use random cast ignore time between min and max"
            );
            useRandomCastIgnoreTime = useRandomCastIgnoreTimeProp.getBoolean();
            
            // Notification settings
            Property notificationsProp = config.get(
                "notifications", 
                "enableNotifications", 
                true, 
                "Enable notifications in chat"
            );
            enableNotifications = notificationsProp.getBoolean();
            
            // Shift key settings
            Property enablePeriodicShiftProp = config.get(
                "shiftkey",
                "enablePeriodicShift",
                true,
                "Periodically hold down shift key to appear more human-like"
            );
            enablePeriodicShift = enablePeriodicShiftProp.getBoolean();
            
            Property minShiftIntervalProp = config.get(
                "shiftkey",
                "minShiftInterval",
                20000,
                "Minimum time in milliseconds between shift key presses (1000+)",
                1000,
                Integer.MAX_VALUE
            );
            minShiftInterval = minShiftIntervalProp.getInt();
            
            Property maxShiftIntervalProp = config.get(
                "shiftkey",
                "maxShiftInterval",
                60000,
                "Maximum time in milliseconds between shift key presses (must be greater than minShiftInterval)",
                minShiftInterval + 1000,
                Integer.MAX_VALUE
            );
            maxShiftInterval = maxShiftIntervalProp.getInt();
            
            Property minShiftDurationProp = config.get(
                "shiftkey",
                "minShiftDuration",
                2000,
                "Minimum duration in milliseconds to hold shift key (100+)",
                100,
                Integer.MAX_VALUE
            );
            minShiftDuration = minShiftDurationProp.getInt();
            
            Property maxShiftDurationProp = config.get(
                "shiftkey",
                "maxShiftDuration",
                4000,
                "Maximum duration in milliseconds to hold shift key (must be greater than minShiftDuration)",
                minShiftDuration + 100,
                Integer.MAX_VALUE
            );
            maxShiftDuration = maxShiftDurationProp.getInt();
            
            // Periodic jump settings
            Property enablePeriodicJumpProp = config.get(
                "jump",
                "enablePeriodicJump",
                true,
                "Enable periodic jumping to simulate human behavior"
            );
            enablePeriodicJump = enablePeriodicJumpProp.getBoolean();
            
            Property minJumpIntervalProp = config.get(
                "jump",
                "minJumpInterval",
                120000,
                "Minimum time in milliseconds between jumps (1000+)",
                1000,
                Integer.MAX_VALUE
            );
            minJumpInterval = minJumpIntervalProp.getInt();
            
            Property maxJumpIntervalProp = config.get(
                "jump",
                "maxJumpInterval",
                240000,
                "Maximum time in milliseconds between jumps (must be greater than minJumpInterval)",
                minJumpInterval + 1000,
                Integer.MAX_VALUE
            );
            maxJumpInterval = maxJumpIntervalProp.getInt();
            
            Property jumpDurationProp = config.get(
                "jump",
                "jumpDuration",
                500,
                "Duration in milliseconds of each jump (100+)",
                100,
                Integer.MAX_VALUE
            );
            jumpDuration = jumpDurationProp.getInt();
            
            // Random look movement settings
            Property enableRandomLookMovementsProp = config.get(
                "lookmovements",
                "enableRandomLookMovements",
                false,
                "Enable random camera movements to appear more human-like (only works with auto-fishing)"
            );
            enableRandomLookMovements = enableRandomLookMovementsProp.getBoolean();
            
            Property randomLookFovRangeProp = config.get(
                "lookmovements",
                "randomLookFovRange",
                5.0f,
                "Maximum FOV range in degrees for random movements (0.5-45.0)",
                0.5f,
                45.0f
            );
            randomLookFovRange = (float) randomLookFovRangeProp.getDouble();
            
            Property minLookMovementDelayProp = config.get(
                "lookmovements",
                "minLookMovementDelay",
                1000,
                "Minimum delay in milliseconds between camera movements (100+)",
                100,
                Integer.MAX_VALUE
            );
            minLookMovementDelay = minLookMovementDelayProp.getInt();
            
            Property maxLookMovementDelayProp = config.get(
                "lookmovements",
                "maxLookMovementDelay",
                4000,
                "Maximum delay in milliseconds between camera movements (must be greater than minLookMovementDelay)",
                minLookMovementDelay + 100,
                Integer.MAX_VALUE
            );
            maxLookMovementDelay = maxLookMovementDelayProp.getInt();
            
            Property lookMovementDurationProp = config.get(
                "lookmovements",
                "lookMovementDuration",
                500,
                "Duration in milliseconds of each camera movement (100+)",
                100,
                Integer.MAX_VALUE
            );
            lookMovementDuration = lookMovementDurationProp.getInt();
            
            // Mouse acceleration settings
            Property enableMouseAccelerationProp = config.get(
                "lookmovements",
                "enableMouseAcceleration",
                true,
                "Enable acceleration/deceleration for mouse movements"
            );
            enableMouseAcceleration = enableMouseAccelerationProp.getBoolean();
            
            // Safety settings
            Property enableSafetyFeaturesProp = config.get(
                "safety",
                "enableSafetyFeatures",
                true,
                "Enable safety features to prevent accidental casting"
            );
            enableSafetyFeatures = enableSafetyFeaturesProp.getBoolean();
            
            Property positionSafetyThresholdProp = config.get(
                "safety",
                "positionSafetyThreshold",
                0.1f,
                "Position safety threshold for detecting accidental casting (0.01+)",
                0.01f,
                Float.MAX_VALUE
            );
            positionSafetyThreshold = (float) positionSafetyThresholdProp.getDouble();
            
            Property rotationSafetyThresholdProp = config.get(
                "safety",
                "rotationSafetyThreshold",
                5.0f,
                "Rotation safety threshold for detecting accidental casting (1.0+)",
                1.0f,
                Float.MAX_VALUE
            );
            rotationSafetyThreshold = (float) rotationSafetyThresholdProp.getDouble();
            
            // Load safety settings
            enablePositionSafety = config.get(
                "safety",
                "enablePositionSafety",
                true,
                "Enable position safety feature"
            ).getBoolean();
            enableRotationSafety = config.get(
                "safety",
                "enableRotationSafety",
                true,
                "Enable rotation safety feature"
            ).getBoolean();
            maxPositionDifference = config.get(
                "safety",
                "maxPositionDifference",
                0.5,
                "Maximum position difference for detecting accidental casting (0.01+)",
                0.01,
                Float.MAX_VALUE
            ).getDouble();
            maxRotationDifference = config.get(
                "safety",
                "maxRotationDifference",
                5.0,
                "Maximum rotation difference for detecting accidental casting (1.0+)",
                1.0,
                Float.MAX_VALUE
            ).getDouble();
            enableLiquidDetection = config.get(
                "safety",
                "enableLiquidDetection",
                true,
                "Enable liquid detection safety feature"
            ).getBoolean();
            maxLiquidFailures = config.get(
                "safety",
                "maxLiquidFailures",
                2,
                "Maximum number of liquid cast failures before disabling auto-catch",
                0,
                Integer.MAX_VALUE
            ).getInt();
            
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
                System.err.println("[GoFish] Config is null, cannot save settings");
                return;
            }
            
            // General settings
            config.get(Configuration.CATEGORY_GENERAL, "enableNotifications", true).set(enableNotifications);
            config.get(Configuration.CATEGORY_GENERAL, "playSoundOnFishCaught", true).set(playSoundOnFishCaught);
            config.get(Configuration.CATEGORY_GENERAL, "playSoundOnSeaCreature", true).set(playSoundOnSeaCreature);
            config.get(Configuration.CATEGORY_GENERAL, "enablePacketLogging", false).set(enablePacketLogging);
            config.get(Configuration.CATEGORY_GENERAL, "enableDebugNotifications", false).set(enableDebugNotifications);
            
            // Notification settings
            config.get("notifications", "showFishCaughtMessages", true).set(showFishCaughtMessages);
            config.get("notifications", "showSeaCreatureMessages", true).set(showSeaCreatureMessages);
            config.get("notifications", "showTreasureMessages", true).set(showTreasureMessages);
            
            // Auto-catch settings
            config.get("autocatch", "enableAutoCatch", false).set(enableAutoCatch);
            config.get("autocatch", "minCatchDelay", 80).set(minCatchDelay);
            config.get("autocatch", "maxCatchDelay", 500).set(maxCatchDelay);
            
            // Miss chance settings
            config.get("misschance", "enableMissChance", true).set(enableMissChance);
            config.get("misschance", "missChancePercentage", 10).set(missChancePercentage);
            config.get("misschance", "minMissTimingOffset", -500).set(minMissTimingOffset);
            config.get("misschance", "maxMissTimingOffset", 800).set(maxMissTimingOffset);
            
            // Auto-recast settings
            config.get("autorecast", "enableAutoRecast", true).set(enableAutoRecast);
            config.get("autorecast", "minRecastDelay", 0).set(minRecastDelay);
            config.get("autorecast", "maxRecastDelay", 1000).set(maxRecastDelay);
            
            // Shift key settings
            config.get("shiftkey", "enablePeriodicShift", true).set(enablePeriodicShift);
            config.get("shiftkey", "minShiftInterval", 20000).set(minShiftInterval);
            config.get("shiftkey", "maxShiftInterval", 60000).set(maxShiftInterval);
            config.get("shiftkey", "minShiftDuration", 2000).set(minShiftDuration);
            config.get("shiftkey", "maxShiftDuration", 4000).set(maxShiftDuration);
            
            // Advanced settings
            config.get("advanced", "castIgnoreTime", 2000).set(castIgnoreTime);
            config.get("advanced", "minCastIgnoreTime", 500).set(minCastIgnoreTime);
            config.get("advanced", "maxCastIgnoreTime", 5000).set(maxCastIgnoreTime);
            config.get("advanced", "biteCooldown", 1000).set(biteCooldown);
            
            // Random look movement settings
            config.get("lookmovements", "enableRandomLookMovements", false).set(enableRandomLookMovements);
            config.get("lookmovements", "randomLookFovRange", 5.0f).set(randomLookFovRange);
            config.get("lookmovements", "minLookMovementDelay", 1000).set(minLookMovementDelay);
            config.get("lookmovements", "maxLookMovementDelay", 4000).set(maxLookMovementDelay);
            config.get("lookmovements", "lookMovementDuration", 500).set(lookMovementDuration);
            config.get("lookmovements", "enableMouseAcceleration", true).set(enableMouseAcceleration);
            config.get("lookmovements", "accelerationPhase", 0.3f).set(accelerationPhase);
            config.get("lookmovements", "decelerationPhase", 0.3f).set(decelerationPhase);
            
            // Periodic jump settings
            config.get("jump", "enablePeriodicJump", true).set(enablePeriodicJump);
            config.get("jump", "minJumpInterval", 120000).set(minJumpInterval);
            config.get("jump", "maxJumpInterval", 240000).set(maxJumpInterval);
            config.get("jump", "jumpDuration", 500).set(jumpDuration);
            
            // Safety settings
            config.get("safety", "enableSafetyFeatures", true).set(enableSafetyFeatures);
            config.get("safety", "positionSafetyThreshold", 0.1f).set(positionSafetyThreshold);
            config.get("safety", "rotationSafetyThreshold", 5.0f).set(rotationSafetyThreshold);
            config.get("safety", "enablePositionSafety", true).set(enablePositionSafety);
            config.get("safety", "enableRotationSafety", true).set(enableRotationSafety);
            config.get("safety", "maxPositionDifference", 0.5).set(maxPositionDifference);
            config.get("safety", "maxRotationDifference", 5.0).set(maxRotationDifference);
            config.get("safety", "enableLiquidDetection", true).set(enableLiquidDetection);
            config.get("safety", "maxLiquidFailures", 2).set(maxLiquidFailures);
            
            // Save the config
            config.save();
            
            System.out.println("[GoFish] Configuration saved");
        } catch (Exception e) {
            System.err.println("[GoFish] Error saving config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Save the current configuration to a named file
     * @param configName The name of the configuration
     * @return True if successful, false otherwise
     */
    public static boolean saveNamedConfig(String configName) {
        try {
            // Create configs directory if it doesn't exist
            File configDir = new File(configFileRef.getParentFile(), "configs");
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            
            // Create the named config file
            File namedConfigFile = new File(configDir, configName + ".cfg");
            
            // Create properties object and save current settings
            Properties props = new Properties();
            
            // Add all config properties
            props.setProperty("enableAutoCatch", String.valueOf(enableAutoCatch));
            props.setProperty("enableAutoRecast", String.valueOf(enableAutoRecast));
            props.setProperty("enablePacketLogging", String.valueOf(enablePacketLogging));
            props.setProperty("enableDebugNotifications", String.valueOf(enableDebugNotifications));
            props.setProperty("enablePeriodicShift", String.valueOf(enablePeriodicShift));
            props.setProperty("minShiftInterval", String.valueOf(minShiftInterval));
            props.setProperty("maxShiftInterval", String.valueOf(maxShiftInterval));
            props.setProperty("minShiftDuration", String.valueOf(minShiftDuration));
            props.setProperty("maxShiftDuration", String.valueOf(maxShiftDuration));
            props.setProperty("castIgnoreTime", String.valueOf(castIgnoreTime));
            props.setProperty("minCastIgnoreTime", String.valueOf(minCastIgnoreTime));
            props.setProperty("maxCastIgnoreTime", String.valueOf(maxCastIgnoreTime));
            props.setProperty("minCatchDelay", String.valueOf(minCatchDelay));
            props.setProperty("maxCatchDelay", String.valueOf(maxCatchDelay));
            props.setProperty("minRecastDelay", String.valueOf(minRecastDelay));
            props.setProperty("maxRecastDelay", String.valueOf(maxRecastDelay));
            props.setProperty("enableRandomLookMovements", String.valueOf(enableRandomLookMovements));
            props.setProperty("randomLookFovRange", String.valueOf(randomLookFovRange));
            props.setProperty("minLookMovementDelay", String.valueOf(minLookMovementDelay));
            props.setProperty("maxLookMovementDelay", String.valueOf(maxLookMovementDelay));
            props.setProperty("lookMovementDuration", String.valueOf(lookMovementDuration));
            props.setProperty("enableMouseAcceleration", String.valueOf(enableMouseAcceleration));
            props.setProperty("accelerationPhase", String.valueOf(accelerationPhase));
            props.setProperty("decelerationPhase", String.valueOf(decelerationPhase));
            props.setProperty("enablePeriodicJump", String.valueOf(enablePeriodicJump));
            props.setProperty("minJumpInterval", String.valueOf(minJumpInterval));
            props.setProperty("maxJumpInterval", String.valueOf(maxJumpInterval));
            props.setProperty("jumpDuration", String.valueOf(jumpDuration));
            props.setProperty("enableSafetyFeatures", String.valueOf(enableSafetyFeatures));
            props.setProperty("positionSafetyThreshold", String.valueOf(positionSafetyThreshold));
            props.setProperty("rotationSafetyThreshold", String.valueOf(rotationSafetyThreshold));
            props.setProperty("enableMissChance", String.valueOf(enableMissChance));
            props.setProperty("missChancePercentage", String.valueOf(missChancePercentage));
            props.setProperty("minMissTimingOffset", String.valueOf(minMissTimingOffset));
            props.setProperty("maxMissTimingOffset", String.valueOf(maxMissTimingOffset));
            props.setProperty("enablePositionSafety", String.valueOf(enablePositionSafety));
            props.setProperty("enableRotationSafety", String.valueOf(enableRotationSafety));
            props.setProperty("maxPositionDifference", String.valueOf(maxPositionDifference));
            props.setProperty("maxRotationDifference", String.valueOf(maxRotationDifference));
            props.setProperty("enableLiquidDetection", String.valueOf(enableLiquidDetection));
            props.setProperty("maxLiquidFailures", String.valueOf(maxLiquidFailures));
            
            // Save to file with comment
            FileOutputStream out = new FileOutputStream(namedConfigFile);
            props.store(out, "GoFish Mod Configuration - " + configName);
            out.close();
            
            System.out.println("[GoFish] Saved named configuration: " + configName);
            return true;
        } catch (Exception e) {
            System.err.println("[GoFish] Error saving named configuration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Load a named configuration from file
     * @param configName The name of the configuration to load
     * @return True if successful, false otherwise
     */
    public static boolean loadNamedConfig(String configName) {
        try {
            // Check if configs directory exists
            File configDir = new File(configFileRef.getParentFile(), "configs");
            if (!configDir.exists()) {
                System.err.println("[GoFish] Configs directory does not exist");
                return false;
            }
            
            // Check if named config file exists
            File namedConfigFile = new File(configDir, configName + ".cfg");
            if (!namedConfigFile.exists()) {
                System.err.println("[GoFish] Named configuration does not exist: " + configName);
                return false;
            }
            
            // Load properties from file
            Properties props = new Properties();
            FileInputStream in = new FileInputStream(namedConfigFile);
            props.load(in);
            in.close();
            
            // Apply loaded settings
            enableAutoCatch = Boolean.parseBoolean(props.getProperty("enableAutoCatch", String.valueOf(enableAutoCatch)));
            enableAutoRecast = Boolean.parseBoolean(props.getProperty("enableAutoRecast", String.valueOf(enableAutoRecast)));
            enablePacketLogging = Boolean.parseBoolean(props.getProperty("enablePacketLogging", String.valueOf(enablePacketLogging)));
            enableDebugNotifications = Boolean.parseBoolean(props.getProperty("enableDebugNotifications", String.valueOf(enableDebugNotifications)));
            enablePeriodicShift = Boolean.parseBoolean(props.getProperty("enablePeriodicShift", String.valueOf(enablePeriodicShift)));
            minShiftInterval = Integer.parseInt(props.getProperty("minShiftInterval", String.valueOf(minShiftInterval)));
            maxShiftInterval = Integer.parseInt(props.getProperty("maxShiftInterval", String.valueOf(maxShiftInterval)));
            minShiftDuration = Integer.parseInt(props.getProperty("minShiftDuration", String.valueOf(minShiftDuration)));
            maxShiftDuration = Integer.parseInt(props.getProperty("maxShiftDuration", String.valueOf(maxShiftDuration)));
            castIgnoreTime = Integer.parseInt(props.getProperty("castIgnoreTime", String.valueOf(castIgnoreTime)));
            minCatchDelay = Integer.parseInt(props.getProperty("minCatchDelay", String.valueOf(minCatchDelay)));
            maxCatchDelay = Integer.parseInt(props.getProperty("maxCatchDelay", String.valueOf(maxCatchDelay)));
            minRecastDelay = Integer.parseInt(props.getProperty("minRecastDelay", String.valueOf(minRecastDelay)));
            maxRecastDelay = Integer.parseInt(props.getProperty("maxRecastDelay", String.valueOf(maxRecastDelay)));
            enableRandomLookMovements = Boolean.parseBoolean(props.getProperty("enableRandomLookMovements", String.valueOf(enableRandomLookMovements)));
            randomLookFovRange = Float.parseFloat(props.getProperty("randomLookFovRange", String.valueOf(randomLookFovRange)));
            minLookMovementDelay = Integer.parseInt(props.getProperty("minLookMovementDelay", String.valueOf(minLookMovementDelay)));
            maxLookMovementDelay = Integer.parseInt(props.getProperty("maxLookMovementDelay", String.valueOf(maxLookMovementDelay)));
            lookMovementDuration = Integer.parseInt(props.getProperty("lookMovementDuration", String.valueOf(lookMovementDuration)));
            enableMouseAcceleration = Boolean.parseBoolean(props.getProperty("enableMouseAcceleration", String.valueOf(enableMouseAcceleration)));
            accelerationPhase = Float.parseFloat(props.getProperty("accelerationPhase", String.valueOf(accelerationPhase)));
            decelerationPhase = Float.parseFloat(props.getProperty("decelerationPhase", String.valueOf(decelerationPhase)));
            enablePeriodicJump = Boolean.parseBoolean(props.getProperty("enablePeriodicJump", String.valueOf(enablePeriodicJump)));
            minJumpInterval = Integer.parseInt(props.getProperty("minJumpInterval", String.valueOf(minJumpInterval)));
            maxJumpInterval = Integer.parseInt(props.getProperty("maxJumpInterval", String.valueOf(maxJumpInterval)));
            jumpDuration = Integer.parseInt(props.getProperty("jumpDuration", String.valueOf(jumpDuration)));
            enableSafetyFeatures = Boolean.parseBoolean(props.getProperty("enableSafetyFeatures", String.valueOf(enableSafetyFeatures)));
            positionSafetyThreshold = Float.parseFloat(props.getProperty("positionSafetyThreshold", String.valueOf(positionSafetyThreshold)));
            rotationSafetyThreshold = Float.parseFloat(props.getProperty("rotationSafetyThreshold", String.valueOf(rotationSafetyThreshold)));
            enableMissChance = Boolean.parseBoolean(props.getProperty("enableMissChance", String.valueOf(enableMissChance)));
            missChancePercentage = Integer.parseInt(props.getProperty("missChancePercentage", String.valueOf(missChancePercentage)));
            minMissTimingOffset = Integer.parseInt(props.getProperty("minMissTimingOffset", String.valueOf(minMissTimingOffset)));
            maxMissTimingOffset = Integer.parseInt(props.getProperty("maxMissTimingOffset", String.valueOf(maxMissTimingOffset)));
            enablePositionSafety = Boolean.parseBoolean(props.getProperty("enablePositionSafety", String.valueOf(enablePositionSafety)));
            enableRotationSafety = Boolean.parseBoolean(props.getProperty("enableRotationSafety", String.valueOf(enableRotationSafety)));
            maxPositionDifference = Double.parseDouble(props.getProperty("maxPositionDifference", String.valueOf(maxPositionDifference)));
            maxRotationDifference = Double.parseDouble(props.getProperty("maxRotationDifference", String.valueOf(maxRotationDifference)));
            enableLiquidDetection = Boolean.parseBoolean(props.getProperty("enableLiquidDetection", String.valueOf(enableLiquidDetection)));
            maxLiquidFailures = Integer.parseInt(props.getProperty("maxLiquidFailures", String.valueOf(maxLiquidFailures)));
            
            // Save to main config file as well
            saveConfig();
            
            System.out.println("[GoFish] Loaded named configuration: " + configName);
            return true;
        } catch (Exception e) {
            System.err.println("[GoFish] Error loading named configuration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * List all available named configurations
     * @return Array of configuration names
     */
    public static String[] listNamedConfigs() {
        try {
            // Check if configs directory exists
            File configDir = new File(configFileRef.getParentFile(), "configs");
            if (!configDir.exists()) {
                return new String[0];
            }
            
            // List all .cfg files
            File[] configFiles = configDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".cfg");
                }
            });
            
            if (configFiles == null || configFiles.length == 0) {
                return new String[0];
            }
            
            // Extract names without extension
            String[] configNames = new String[configFiles.length];
            for (int i = 0; i < configFiles.length; i++) {
                String fileName = configFiles[i].getName();
                configNames[i] = fileName.substring(0, fileName.length() - 4); // Remove .cfg
            }
            
            return configNames;
        } catch (Exception e) {
            System.err.println("[GoFish] Error listing named configurations: " + e.getMessage());
            e.printStackTrace();
            return new String[0];
        }
    }

    /**
     * Delete a named configuration
     * @param configName The name of the configuration to delete
     * @return True if successful, false otherwise
     */
    public static boolean deleteNamedConfig(String configName) {
        try {
            // Check if configs directory exists
            File configDir = new File(configFileRef.getParentFile(), "configs");
            if (!configDir.exists()) {
                return false;
            }
            
            // Check if named config file exists
            File namedConfigFile = new File(configDir, configName + ".cfg");
            if (!namedConfigFile.exists()) {
                return false;
            }
            
            // Delete the file
            boolean success = namedConfigFile.delete();
            
            if (success) {
                System.out.println("[GoFish] Deleted named configuration: " + configName);
            } else {
                System.err.println("[GoFish] Failed to delete named configuration: " + configName);
            }
            
            return success;
        } catch (Exception e) {
            System.err.println("[GoFish] Error deleting named configuration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
} 