package com.example.gofish;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import com.example.gofish.handlers.PacketHandler;
import com.example.gofish.handlers.ChatHandler;
import com.example.gofish.handlers.FishingHandler;
import com.example.gofish.handlers.KeyBindingHandler;
import com.example.gofish.handlers.LookMovementHandler;
import com.example.gofish.handlers.ShiftKeyHandler;
import com.example.gofish.handlers.JumpHandler;
import com.example.gofish.handlers.PositionTracker;
import com.example.gofish.config.GoFishConfig;
import com.example.gofish.utils.FishingUtils;

@Mod(modid = GoFishMod.MODID, version = GoFishMod.VERSION)
public class GoFishMod
{
    public static final String MODID = "gofish";
    public static final String VERSION = "1.0";
    
    // Static instance for access from command handlers
    @Mod.Instance(MODID)
    public static GoFishMod instance;
    
    // Store handlers as instance variables for better coordination
    private PacketHandler packetHandler;
    private ChatHandler chatHandler;
    private FishingHandler fishingHandler;
    private KeyBindingHandler keyBindingHandler;
    private LookMovementHandler lookMovementHandler;
    private ShiftKeyHandler shiftKeyHandler;
    private JumpHandler jumpHandler;
    private PositionTracker positionTracker;
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        try {
            // Initialize configuration
            GoFishConfig.init(event.getSuggestedConfigurationFile());
            
            // Create handlers
            fishingHandler = new FishingHandler();
            packetHandler = new PacketHandler(fishingHandler);
            chatHandler = new ChatHandler();
            keyBindingHandler = new KeyBindingHandler(packetHandler);
            lookMovementHandler = new LookMovementHandler();
            shiftKeyHandler = new ShiftKeyHandler();
            jumpHandler = new JumpHandler();
            positionTracker = new PositionTracker();
            
            // Connect handlers
            FishingUtils.setPacketHandler(packetHandler);
            fishingHandler.setShiftKeyHandler(shiftKeyHandler);
            fishingHandler.setJumpHandler(jumpHandler);
            fishingHandler.setPositionTracker(positionTracker);
            
            // Register our handlers
            MinecraftForge.EVENT_BUS.register(packetHandler);
            MinecraftForge.EVENT_BUS.register(chatHandler);
            MinecraftForge.EVENT_BUS.register(fishingHandler);
            MinecraftForge.EVENT_BUS.register(keyBindingHandler);
            MinecraftForge.EVENT_BUS.register(lookMovementHandler);
            MinecraftForge.EVENT_BUS.register(shiftKeyHandler);
            MinecraftForge.EVENT_BUS.register(jumpHandler);
            MinecraftForge.EVENT_BUS.register(positionTracker);
            
            // Log that the mod is starting
            System.out.println("[GoFish] Initializing GoFish mod version " + VERSION);
        } catch (Exception e) {
            // Log any errors during initialization
            System.err.println("[GoFish] Error during initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        try {
            // Register debug command
            ClientCommandHandler.instance.registerCommand(new CommandGoFish(fishingHandler));
            
            // some example code
            System.out.println("DIRT BLOCK >> "+Blocks.dirt.getUnlocalizedName());
        } catch (Exception e) {
            // Log any errors during initialization
            System.err.println("[GoFish] Error during init: " + e.getMessage());
            e.printStackTrace();
        }
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
     * Debug command for GoFish
     */
    public static class CommandGoFish extends CommandBase {
        private final FishingHandler fishingHandler;
        
        public CommandGoFish(FishingHandler fishingHandler) {
            this.fishingHandler = fishingHandler;
        }
        
        @Override
        public String getCommandName() {
            return "gofish";
        }
        
        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "§b/gofish help §f- Show this help message\n" +
                   "§b/gofish autocatch [on|off] §f- Toggle auto-fishing on/off\n" +
                   "§b/gofish packetlogging [on|off] §f- Toggle packet logging\n" + 
                   "§b/gofish castignore <time> §f- Set cast ignore time (ms)\n" +
                   "§b/gofish lookmove [on|off|fov|delay|duration|accel] §f- Control random look movements\n" +
                   "§b/gofish shift [on|off|interval|duration] §f- Control periodic shift key\n" +
                   "§b/gofish jump [on|off|interval|duration] §f- Control periodic jumping\n" +
                   "§b/gofish safety [on|off|position|rotation] §f- Control safety features\n" +
                   "§b/gofish debug [on|off] §f- Toggle debug notifications\n" +
                   "§b/gofish catchtime <min> <max> §f- Set auto-catch delay range (ms)\n" +
                   "§b/gofish recasttime <min> <max> §f- Set auto-recast delay range (ms)\n" +
                   "§b/gofish miss [on|off|chance|timing] §f- Control miss chance settings\n" +
                   "§b/gofish config [save|load|list|delete] <name> §f- Manage configurations\n" +
                   "§b/gofish reload §f- Reload configuration";
        }
        
        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) {
            return true; // Allow all players to use this command
        }
        
        @Override
        public void processCommand(ICommandSender sender, String[] args) throws CommandException {
            try {
                if (args.length == 0) {
                    // Display help
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fAvailable commands: ")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish reload &f- Reload configuration")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish autocatch [on|off] &f- Toggle auto-catch")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish packetlogging [on|off] &f- Toggle packet logging")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish castignore <time> &f- Set cast ignore time (ms)")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish lookmove [on|off|fov|delay|duration|accel] &f- Control random look movements")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish shift [on|off|interval|duration] &f- Control periodic shift key")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish jump [on|off|interval|duration] &f- Control periodic jumping")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish safety [on|off|position|rotation] &f- Control safety features")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish debug [on|off] &f- Toggle debug notifications")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish catchtime <min> <max> &f- Set auto-catch delay range (ms)")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish recasttime <min> <max> &f- Set auto-recast delay range (ms)")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish miss [on|off|chance|timing] &f- Control miss chance settings")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish config [save|load|list|delete] <name> &f- Manage configurations")));
                    return;
                }
                
                String subCommand = args[0].toLowerCase();
                
                if (subCommand.equals("reload")) {
                    // Reload config
                    GoFishConfig.loadConfig();
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fConfiguration reloaded! ")));
                } else if (subCommand.equals("autocatch")) {
                    // Toggle auto-catch
                    if (args.length > 1) {
                        String option = args[1].toLowerCase();
                        if (option.equals("on") || option.equals("enable") || option.equals("true")) {
                            GoFishConfig.enableAutoCatch = true;
                            GoFishConfig.enableAutoRecast = true;
                            GoFishConfig.saveConfig();
                            
                            // Reset position tracking
                            if (GoFishMod.instance != null && GoFishMod.instance.positionTracker != null) {
                                GoFishMod.instance.positionTracker.resetTracking();
                            }
                            
                            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fAuto-fishing enabled.")));
                            
                            // Auto-cast the rod if needed
                            if (fishingHandler != null) {
                                fishingHandler.castRodIfNeeded();
                            }
                        } else if (option.equals("off") || option.equals("disable") || option.equals("false")) {
                            GoFishConfig.enableAutoCatch = false;
                            GoFishConfig.enableAutoRecast = false;
                            GoFishConfig.saveConfig();
                            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fAuto-fishing disabled.")));
                        } else {
                            sender.addChatMessage(new ChatComponentText(formatColorCodes("&c[GoFish] &fInvalid option. Use 'on' or 'off'.")));
                        }
                    } else {
                        // Display current settings
                        boolean enabled = GoFishConfig.enableAutoCatch && GoFishConfig.enableAutoRecast;
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fAuto-fishing is currently " + 
                            (enabled ? "&aEnabled" : "&cDisabled") + "&f.")));
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fUse &e/gofish autocatch on&f or &e/gofish autocatch off&f to change.")));
                    }
                } else if (subCommand.equals("packetlogging")) {
                    // Toggle packet logging
                    if (args.length > 1) {
                        String option = args[1].toLowerCase();
                        if (option.equals("on") || option.equals("enable") || option.equals("true")) {
                            GoFishConfig.enablePacketLogging = true;
                            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &aPacket logging enabled! ")));
                        } else if (option.equals("off") || option.equals("disable") || option.equals("false")) {
                            GoFishConfig.enablePacketLogging = false;
                            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &cPacket logging disabled! ")));
                        } else {
                            sender.addChatMessage(new ChatComponentText(formatColorCodes("&c[GoFish] &fInvalid option. Use 'on' or 'off'.")));
                        }
                    } else {
                        // Display current settings
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fPacket logging is currently " + 
                            (GoFishConfig.enablePacketLogging ? "&aEnabled" : "&cDisabled") + "&f.")));
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fUse &e/gofish packetlogging on&f or &e/gofish packetlogging off&f to change.")));
                    }
                    
                    // Save the config
                    GoFishConfig.saveConfig();
                } else if (subCommand.equals("castignore")) {
                    // Set cast ignore time
                    if (args.length > 1) {
                        try {
                            int time = Integer.parseInt(args[1]);
                            if (time >= 500 && time <= 25000) {
                                GoFishConfig.castIgnoreTime = time;
                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fCast ignore time set to &e" + time + "ms&f! ")));
                                
                                // Save the config
                                GoFishConfig.saveConfig();
                            } else {
                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &cInvalid time. Must be between 500 and 25000 ms (25 seconds).")));
                            }
                        } catch (NumberFormatException e) {
                            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &cInvalid number format. Use a number between 500 and 25000.")));
                        }
                    } else {
                        // Show current value
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fCurrent cast ignore time: &e" + GoFishConfig.castIgnoreTime + "ms")));
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fUse &e/gofish castignore <time>&f to change it.")));
                    }
                } else if (subCommand.equals("lookmove")) {
                    // Handle look movement commands
                    if (args.length > 1) {
                        String option = args[1].toLowerCase();
                        
                        if (option.equals("on") || option.equals("enable") || option.equals("true")) {
                            GoFishConfig.enableRandomLookMovements = true;
                            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &aRandom look movements enabled! ")));
                            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fNote: Look movements only work when auto-catch is enabled.")));
                            GoFishConfig.saveConfig();
                        } else if (option.equals("off") || option.equals("disable") || option.equals("false")) {
                            GoFishConfig.enableRandomLookMovements = false;
                            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &cRandom look movements disabled! ")));
                            GoFishConfig.saveConfig();
                        } else if (option.equals("fov") && args.length > 2) {
                            try {
                                float fov = Float.parseFloat(args[2]);
                                
                                // Only enforce minimum values, no upper limits
                                if (fov < 0.5f) fov = 0.5f; // Minimum 0.5 degrees
                                
                                GoFishConfig.randomLookFovRange = fov;
                                GoFishConfig.saveConfig();
                                
                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fRandom look FOV range set to " + 
                                    fov + " degrees.")));
                            } catch (NumberFormatException e) {
                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&c[GoFish] &fInvalid number. Usage: /gofish lookmove fov <value>")));
                            }
                        } else if (option.equals("delay") && args.length > 3) {
                            try {
                                int minDelay = Integer.parseInt(args[2]);
                                int maxDelay = Integer.parseInt(args[3]);
                                
                                // Only enforce minimum values, no upper limits
                                if (minDelay < 100) minDelay = 100; // Minimum 100ms
                                if (maxDelay < minDelay + 100) maxDelay = minDelay + 100; // At least 100ms more than min
                                
                                GoFishConfig.minLookMovementDelay = minDelay;
                                GoFishConfig.maxLookMovementDelay = maxDelay;
                                GoFishConfig.saveConfig();
                                
                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fLook movement delay set to " + 
                                    minDelay + "-" + maxDelay + " ms.")));
                            } catch (NumberFormatException e) {
                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&c[GoFish] &fInvalid numbers. Usage: /gofish lookmove delay <min> <max>")));
                            }
                        } else if (option.equals("duration") && args.length > 2) {
                            try {
                                int duration = Integer.parseInt(args[2]);
                                
                                // Only enforce minimum values, no upper limits
                                if (duration < 100) duration = 100; // Minimum 100ms
                                
                                GoFishConfig.lookMovementDuration = duration;
                                GoFishConfig.saveConfig();
                                
                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fLook movement duration set to " + 
                                    duration + " ms.")));
                            } catch (NumberFormatException e) {
                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&c[GoFish] &fInvalid number. Usage: /gofish lookmove duration <value>")));
                            }
                        } else if (option.equals("accel")) {
                            if (args.length > 2) {
                                String accelOption = args[2].toLowerCase();
                                
                                if (accelOption.equals("on") || accelOption.equals("enable") || accelOption.equals("true")) {
                                    GoFishConfig.enableMouseAcceleration = true;
                                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &aMouse acceleration enabled! ")));
                                    GoFishConfig.saveConfig();
                                } else if (accelOption.equals("off") || accelOption.equals("disable") || accelOption.equals("false")) {
                                    GoFishConfig.enableMouseAcceleration = false;
                                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &cMouse acceleration disabled! ")));
                                    GoFishConfig.saveConfig();
                                } else if (args.length > 3) {
                                    try {
                                        float accelPhase = Float.parseFloat(args[2]);
                                        float decelPhase = Float.parseFloat(args[3]);
                                        
                                        if (accelPhase >= 0.1f && accelPhase <= 0.5f && decelPhase >= 0.1f && decelPhase <= 0.5f) {
                                            // Ensure there's at least 10% for constant speed phase
                                            if (accelPhase + decelPhase <= 0.9f) {
                                                GoFishConfig.accelerationPhase = accelPhase;
                                                GoFishConfig.decelerationPhase = decelPhase;
                                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fAcceleration phases set to &e" + 
                                                    String.format("%.1f", accelPhase * 100) + "% accel, " + 
                                                    String.format("%.1f", decelPhase * 100) + "% decel&f! ")));
                                                GoFishConfig.saveConfig();
                                            } else {
                                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &cInvalid phases. Sum of acceleration and deceleration must be <= 0.9 (90%).")));
                                            }
                                        } else {
                                            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &cInvalid phases. Each must be between 0.1 and 0.5 (10%-50%).")));
                                        }
                                    } catch (NumberFormatException e) {
                                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &cInvalid number format. Use decimal numbers between 0.1 and 0.5.")));
                                    }
                                } else {
                                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fCurrent acceleration settings: ")));
                                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- Enabled: " + (GoFishConfig.enableMouseAcceleration ? "&aYes" : "&cNo"))));
                                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- Acceleration phase: &e" + String.format("%.1f", GoFishConfig.accelerationPhase * 100) + "%")));
                                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- Deceleration phase: &e" + String.format("%.1f", GoFishConfig.decelerationPhase * 100) + "%")));
                                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- Constant speed phase: &e" + String.format("%.1f", (1 - GoFishConfig.accelerationPhase - GoFishConfig.decelerationPhase) * 100) + "%")));
                                }
                            } else {
                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fCurrent acceleration settings: ")));
                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- Enabled: " + (GoFishConfig.enableMouseAcceleration ? "&aYes" : "&cNo"))));
                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- Acceleration phase: &e" + String.format("%.1f", GoFishConfig.accelerationPhase * 100) + "%")));
                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- Deceleration phase: &e" + String.format("%.1f", GoFishConfig.decelerationPhase * 100) + "%")));
                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- Constant speed phase: &e" + String.format("%.1f", (1 - GoFishConfig.accelerationPhase - GoFishConfig.decelerationPhase) * 100) + "%")));
                            }
                        } else {
                            // Show help for look movement commands
                            showLookMoveHelp(sender);
                        }
                    } else {
                        // Show current settings
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fRandom look movement settings: ")));
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- Enabled: " + (GoFishConfig.enableRandomLookMovements ? "&aYes" : "&cNo"))));
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- FOV Range: &e" + GoFishConfig.randomLookFovRange + "°")));
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- Delay: &e" + GoFishConfig.minLookMovementDelay + "-" + GoFishConfig.maxLookMovementDelay + "ms")));
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- Duration: &e" + GoFishConfig.lookMovementDuration + "ms")));
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- Acceleration: " + (GoFishConfig.enableMouseAcceleration ? "&aEnabled" : "&cDisabled"))));
                        if (GoFishConfig.enableMouseAcceleration) {
                            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- Accel/Decel: &e" + 
                                String.format("%.1f", GoFishConfig.accelerationPhase * 100) + "% / " + 
                                String.format("%.1f", GoFishConfig.decelerationPhase * 100) + "%")));
                        }
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fNote: Look movements only work when auto-catch is enabled.")));
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fUse &e/gofish lookmove help&f for command help.")));
                    }
                } else if (subCommand.equals("debug")) {
                    // Toggle debug notifications
                    if (args.length > 1) {
                        String option = args[1].toLowerCase();
                        if (option.equals("on") || option.equals("enable") || option.equals("true")) {
                            GoFishConfig.enableDebugNotifications = true;
                            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &aDebug notifications enabled! ")));
                        } else if (option.equals("off") || option.equals("disable") || option.equals("false")) {
                            GoFishConfig.enableDebugNotifications = false;
                            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &cDebug notifications disabled! ")));
                        } else {
                            sender.addChatMessage(new ChatComponentText(formatColorCodes("&c[GoFish] &fInvalid option. Use 'on' or 'off'.")));
                        }
                    } else {
                        // Display current settings
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fDebug notifications are currently " + 
                            (GoFishConfig.enableDebugNotifications ? "&aEnabled" : "&cDisabled") + "&f.")));
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fUse &e/gofish debug on&f or &e/gofish debug off&f to change.")));
                    }
                    
                    // Save the config
                    GoFishConfig.saveConfig();
                } else if (subCommand.equals("catchtime")) {
                    // Set auto-catch delay range
                    if (args.length > 2) {
                        try {
                            int minDelay = Integer.parseInt(args[1]);
                            int maxDelay = Integer.parseInt(args[2]);
                            
                            // Only enforce minimum values, no upper limits
                            if (minDelay < 0) minDelay = 0; // Minimum 0ms
                            if (maxDelay < minDelay + 1) maxDelay = minDelay + 1; // At least 1ms more than min
                            
                            GoFishConfig.minCatchDelay = minDelay;
                            GoFishConfig.maxCatchDelay = maxDelay;
                            GoFishConfig.saveConfig();
                            
                            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fAuto-catch delay set to " + 
                                minDelay + "-" + maxDelay + " ms.")));
                        } catch (NumberFormatException e) {
                            sender.addChatMessage(new ChatComponentText(formatColorCodes("&c[GoFish] &fInvalid numbers. Usage: /gofish catchtime <min> <max>")));
                        }
                    } else {
                        // Show current settings
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fCurrent auto-catch delay range: &e" + 
                            GoFishConfig.minCatchDelay + "-" + GoFishConfig.maxCatchDelay + "ms")));
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fUse &e/gofish catchtime <min> <max>&f to change it.")));
                    }
                } else if (subCommand.equals("recasttime")) {
                    if (args.length >= 3) {
                        try {
                            int minDelay = Integer.parseInt(args[1]);
                            int maxDelay = Integer.parseInt(args[2]);
                            
                            // Only enforce minimum values, no upper limits
                            if (minDelay < 0) minDelay = 0; // Minimum 0ms
                            if (maxDelay < minDelay + 1) maxDelay = minDelay + 1; // At least 1ms more than min
                            
                            // Set the values
                            GoFishConfig.minRecastDelay = minDelay;
                            GoFishConfig.maxRecastDelay = maxDelay;
                            GoFishConfig.saveConfig();
                            
                            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fAuto-recast delay set to " + 
                                minDelay + "-" + maxDelay + " ms.")));
                        } catch (NumberFormatException e) {
                            sender.addChatMessage(new ChatComponentText(formatColorCodes("&c[GoFish] &fInvalid numbers. Usage: /gofish recasttime <min> <max>")));
                        }
                    } else {
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fCurrent auto-recast delay range: " + GoFishConfig.minRecastDelay + "-" + GoFishConfig.maxRecastDelay + "ms")));
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fUsage: /gofish recasttime <min> <max>")));
                    }
                } else if (subCommand.equals("shift")) {
                    // Handle shift key commands
                    if (args.length > 1) {
                        String option = args[1].toLowerCase();
                        
                        if (option.equals("on") || option.equals("enable") || option.equals("true")) {
                            GoFishConfig.enablePeriodicShift = true;
                            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &aPeriodic shift key enabled! ")));
                            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fNote: Shift key only works when auto-catch is enabled.")));
                            GoFishConfig.saveConfig();
                        } else if (option.equals("off") || option.equals("disable") || option.equals("false")) {
                            GoFishConfig.enablePeriodicShift = false;
                            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &cPeriodic shift key disabled! ")));
                            GoFishConfig.saveConfig();
                        } else if (option.equals("interval") && args.length == 4) {
                            try {
                                int min = Integer.parseInt(args[2]) * 1000; // Convert seconds to milliseconds
                                int max = Integer.parseInt(args[3]) * 1000;
                                
                                // Only enforce minimum values, no upper limits
                                if (min < 1000) min = 1000; // Minimum 1 second
                                if (max < min + 1000) max = min + 1000; // At least 1 second more than min
                                
                                GoFishConfig.minShiftInterval = min;
                                GoFishConfig.maxShiftInterval = max;
                                GoFishConfig.saveConfig();
                                
                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fShift interval set to " + 
                                    min/1000 + "-" + max/1000 + " seconds.")));
                            } catch (NumberFormatException e) {
                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&c[GoFish] &fInvalid numbers. Usage: /gofish shift interval <min_seconds> <max_seconds>")));
                            }
                        } else if (option.equals("duration") && args.length == 4) {
                            try {
                                int min = Integer.parseInt(args[2]) * 1000; // Convert seconds to milliseconds
                                int max = Integer.parseInt(args[3]) * 1000;
                                
                                // Only enforce minimum values, no upper limits
                                if (min < 100) min = 100; // Minimum 0.1 seconds
                                if (max < min + 100) max = min + 100; // At least 0.1 seconds more than min
                                
                                GoFishConfig.minShiftDuration = min;
                                GoFishConfig.maxShiftDuration = max;
                                GoFishConfig.saveConfig();
                                
                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fShift duration set to " + 
                                    min/1000.0 + "-" + max/1000.0 + " seconds.")));
                            } catch (NumberFormatException e) {
                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&c[GoFish] &fInvalid numbers. Usage: /gofish shift duration <min_seconds> <max_seconds>")));
                            }
                        } else {
                            // Show help for shift key commands
                            showShiftKeyHelp(sender);
                        }
                    } else {
                        // Show current settings
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fPeriodic shift key settings: ")));
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- Enabled: " + (GoFishConfig.enablePeriodicShift ? "&aYes" : "&cNo"))));
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- Interval: &e" + 
                            (GoFishConfig.minShiftInterval / 1000) + "-" + (GoFishConfig.maxShiftInterval / 1000) + " seconds")));
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- Duration: &e" + 
                            (GoFishConfig.minShiftDuration / 1000) + "-" + (GoFishConfig.maxShiftDuration / 1000) + " seconds")));
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fNote: Shift key only works when auto-catch is enabled.")));
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fUse &e/gofish shift help&f for command help.")));
                    }
                } else if (subCommand.equals("jump")) {
                    if (args.length == 1 || args[1].equalsIgnoreCase("help") || args[1].equalsIgnoreCase("settings")) {
                        showJumpHelp(sender);
                    }
                    else if (args[1].equalsIgnoreCase("on")) {
                        GoFishConfig.enablePeriodicJump = true;
                        GoFishConfig.saveConfig();
                        sender.addChatMessage(new ChatComponentText("§b[GoFish] §fPeriodic jumping enabled."));
                        sender.addChatMessage(new ChatComponentText("§b[GoFish] §fJump interval: " + 
                                GoFishConfig.minJumpInterval/1000 + "-" + GoFishConfig.maxJumpInterval/1000 + " seconds"));
                        sender.addChatMessage(new ChatComponentText("§b[GoFish] §fJump duration: " + 
                                GoFishConfig.jumpDuration + " ms"));
                    }
                    else if (args[1].equalsIgnoreCase("off")) {
                        GoFishConfig.enablePeriodicJump = false;
                        GoFishConfig.saveConfig();
                        sender.addChatMessage(new ChatComponentText("§b[GoFish] §fPeriodic jumping disabled."));
                    }
                    else if (args[1].equalsIgnoreCase("interval") && args.length == 4) {
                        try {
                            int min = Integer.parseInt(args[2]) * 1000; // Convert seconds to milliseconds
                            int max = Integer.parseInt(args[3]) * 1000;
                            
                            // Only enforce minimum values, no upper limits
                            if (min < 1000) min = 1000; // Minimum 1 second
                            if (max < min + 1000) max = min + 1000; // At least 1 second more than min
                            
                            GoFishConfig.minJumpInterval = min;
                            GoFishConfig.maxJumpInterval = max;
                            GoFishConfig.saveConfig();
                            
                            sender.addChatMessage(new ChatComponentText("§b[GoFish] §fJump interval set to " + 
                                    min/1000 + "-" + max/1000 + " seconds."));
                        } catch (NumberFormatException e) {
                            sender.addChatMessage(new ChatComponentText("§c[GoFish] §fInvalid numbers. Usage: /gofish jump interval <min_seconds> <max_seconds>"));
                        }
                    }
                    else if (args[1].equalsIgnoreCase("duration") && args.length == 3) {
                        try {
                            int duration = Integer.parseInt(args[2]);
                            
                            // Only enforce minimum values, no upper limits
                            if (duration < 100) duration = 100; // Minimum 100ms
                            
                            GoFishConfig.jumpDuration = duration;
                            GoFishConfig.saveConfig();
                            
                            sender.addChatMessage(new ChatComponentText("§b[GoFish] §fJump duration set to " + 
                                    duration + " ms."));
                        } catch (NumberFormatException e) {
                            sender.addChatMessage(new ChatComponentText("§c[GoFish] §fInvalid number. Usage: /gofish jump duration <milliseconds>"));
                        }
                    }
                    else {
                        showJumpHelp(sender);
                    }
                } else if (subCommand.equals("safety")) {
                    if (args.length == 1 || args[1].equalsIgnoreCase("help") || args[1].equalsIgnoreCase("settings")) {
                        showSafetyHelp(sender);
                    }
                    else if (args[1].equalsIgnoreCase("on")) {
                        GoFishConfig.enableSafetyFeatures = true;
                        GoFishConfig.saveConfig();
                        sender.addChatMessage(new ChatComponentText("§b[GoFish] §fSafety features enabled."));
                    }
                    else if (args[1].equalsIgnoreCase("off")) {
                        GoFishConfig.enableSafetyFeatures = false;
                        GoFishConfig.saveConfig();
                        sender.addChatMessage(new ChatComponentText("§b[GoFish] §fSafety features disabled."));
                        sender.addChatMessage(new ChatComponentText("§c[GoFish] §fWarning: Disabling safety features may increase detection risk!"));
                    }
                    else if (args[1].equalsIgnoreCase("position") && args.length == 3) {
                        try {
                            float threshold = Float.parseFloat(args[2]);
                            
                            // Only enforce minimum values, no upper limits
                            if (threshold < 0.01f) threshold = 0.01f; // Minimum 0.01 blocks
                            
                            GoFishConfig.positionSafetyThreshold = threshold;
                            GoFishConfig.saveConfig();
                            
                            sender.addChatMessage(new ChatComponentText("§b[GoFish] §fPosition safety threshold set to " + 
                                    threshold + " blocks."));
                        } catch (NumberFormatException e) {
                            sender.addChatMessage(new ChatComponentText("§c[GoFish] §fInvalid number. Usage: /gofish safety position <threshold>"));
                        }
                    }
                    else if (args[1].equalsIgnoreCase("rotation") && args.length == 3) {
                        try {
                            float threshold = Float.parseFloat(args[2]);
                            
                            // Only enforce minimum values, no upper limits
                            if (threshold < 1.0f) threshold = 1.0f; // Minimum 1 degree
                            
                            GoFishConfig.rotationSafetyThreshold = threshold;
                            GoFishConfig.saveConfig();
                            
                            sender.addChatMessage(new ChatComponentText("§b[GoFish] §fRotation safety threshold set to " + 
                                    threshold + " degrees."));
                        } catch (NumberFormatException e) {
                            sender.addChatMessage(new ChatComponentText("§c[GoFish] §fInvalid number. Usage: /gofish safety rotation <threshold>"));
                        }
                    }
                    else {
                        showSafetyHelp(sender);
                    }
                } else if (subCommand.equals("miss")) {
                    if (args.length == 1 || args[1].equalsIgnoreCase("help") || args[1].equalsIgnoreCase("settings")) {
                        showMissChanceHelp(sender);
                    }
                    else if (args[1].equalsIgnoreCase("on")) {
                        GoFishConfig.enableMissChance = true;
                        GoFishConfig.saveConfig();
                        sender.addChatMessage(new ChatComponentText("§b[GoFish] §fMiss chance feature enabled."));
                        sender.addChatMessage(new ChatComponentText("§b[GoFish] §fMiss chance: " + 
                                GoFishConfig.missChancePercentage + "%"));
                        sender.addChatMessage(new ChatComponentText("§b[GoFish] §fMiss timing: " + 
                                GoFishConfig.minMissTimingOffset + "ms to " + GoFishConfig.maxMissTimingOffset + "ms"));
                    }
                    else if (args[1].equalsIgnoreCase("off")) {
                        GoFishConfig.enableMissChance = false;
                        GoFishConfig.saveConfig();
                        sender.addChatMessage(new ChatComponentText("§b[GoFish] §fMiss chance feature disabled."));
                    }
                    else if (args[1].equalsIgnoreCase("chance") && args.length == 3) {
                        try {
                            int chance = Integer.parseInt(args[2]);
                            
                            // Validate chance
                            if (chance < 0) chance = 0;
                            if (chance > 100) chance = 100;
                            
                            GoFishConfig.missChancePercentage = chance;
                            GoFishConfig.saveConfig();
                            
                            sender.addChatMessage(new ChatComponentText("§b[GoFish] §fMiss chance percentage set to " + 
                                    chance + "%."));
                        } catch (NumberFormatException e) {
                            sender.addChatMessage(new ChatComponentText("§c[GoFish] §fInvalid number. Usage: /gofish miss chance <percentage>"));
                        }
                    }
                    else if (args[1].equalsIgnoreCase("timing") && args.length == 4) {
                        try {
                            int earlyMs = Integer.parseInt(args[2]);  // Early timing (negative)
                            int lateMs = Integer.parseInt(args[3]);   // Late timing (positive)
                            
                            // Validate timing values
                            if (earlyMs > 0) earlyMs = -earlyMs;     // Force early timing to be negative
                            if (lateMs < 0) lateMs = -lateMs;        // Force late timing to be positive
                            
                            GoFishConfig.minMissTimingOffset = earlyMs;
                            GoFishConfig.maxMissTimingOffset = lateMs;
                            GoFishConfig.saveConfig();
                            
                            sender.addChatMessage(new ChatComponentText("§b[GoFish] §fMiss timing set to " + 
                                    earlyMs + "ms early to " + lateMs + "ms late."));
                        } catch (NumberFormatException e) {
                            sender.addChatMessage(new ChatComponentText("§c[GoFish] §fInvalid numbers. Usage: /gofish miss timing <early_ms> <late_ms>"));
                            sender.addChatMessage(new ChatComponentText("§c[GoFish] §fExample: /gofish miss timing -500 800"));
                        }
                    }
                    else {
                        showMissChanceHelp(sender);
                    }
                } else if (subCommand.equals("help")) {
                    // Show help for all commands
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fAvailable commands: ")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish reload &f- Reload configuration")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish autocatch [on|off] &f- Toggle auto-catch")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish packetlogging [on|off] &f- Toggle packet logging")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish castignore <time> &f- Set cast ignore time (ms)")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish lookmove [on|off|fov|delay|duration|accel] &f- Control random look movements")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish shift [on|off|interval|duration] &f- Control periodic shift key")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish jump [on|off|interval|duration] &f- Control periodic jumping")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish safety [on|off|position|rotation] &f- Control safety features")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish debug [on|off] &f- Toggle debug notifications")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish catchtime <min> <max> &f- Set auto-catch delay range (ms)")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish recasttime <min> <max> &f- Set auto-recast delay range (ms)")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish miss [on|off|chance|timing] &f- Control miss chance settings")));
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish config [save|load|list|delete] <name> &f- Manage configurations")));
                } else if (subCommand.equals("config")) {
                    // Handle config commands
                    if (args.length > 1) {
                        String option = args[1].toLowerCase();
                        
                        if (option.equals("save")) {
                            if (args.length > 2) {
                                // Save named configuration
                                String configName = args[2];
                                boolean success = GoFishConfig.saveNamedConfig(configName);
                                
                                if (success) {
                                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &aConfiguration saved as '" + configName + "'")));
                                } else {
                                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &cFailed to save configuration as '" + configName + "'")));
                                }
                            } else {
                                // Save default configuration
                                GoFishConfig.saveConfig();
                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &aConfiguration saved successfully!")));
                            }
                        } else if (option.equals("load")) {
                            if (args.length > 2) {
                                // Load named configuration
                                String configName = args[2];
                                boolean success = GoFishConfig.loadNamedConfig(configName);
                                
                                if (success) {
                                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &aLoaded configuration '" + configName + "'")));
                                } else {
                                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &cFailed to load configuration '" + configName + "'")));
                                }
                            } else {
                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &cPlease specify a configuration name to load")));
                            }
                        } else if (option.equals("list")) {
                            // List all available configurations
                            String[] configs = GoFishConfig.listNamedConfigs();
                            
                            if (configs.length > 0) {
                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fAvailable configurations:")));
                                for (String config : configs) {
                                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e" + config)));
                                }
                            } else {
                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fNo saved configurations found")));
                            }
                        } else if (option.equals("delete")) {
                            if (args.length > 2) {
                                // Delete named configuration
                                String configName = args[2];
                                boolean success = GoFishConfig.deleteNamedConfig(configName);
                                
                                if (success) {
                                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &aDeleted configuration '" + configName + "'")));
                                } else {
                                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &cFailed to delete configuration '" + configName + "'")));
                                }
                            } else {
                                sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &cPlease specify a configuration name to delete")));
                            }
                        } else {
                            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &cUnknown config command. Available: save, load, list, delete")));
                        }
                    } else {
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fConfig Commands:")));
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish config save [name] &f- Save current configuration")));
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish config load <name> &f- Load a saved configuration")));
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish config list &f- List all saved configurations")));
                        sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish config delete <name> &f- Delete a saved configuration")));
                    }
                } else {
                    sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &cUnknown command. Use /gofish for help.")));
                }
            } catch (Exception e) {
                // Log any errors but don't crash the game
                System.err.println("[GoFish] Error processing command: " + e.getMessage());
                sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &cAn error occurred while processing the command.")));
            }
        }
        
        /**
         * Show help for look movement commands
         */
        private void showLookMoveHelp(ICommandSender sender) {
            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fLook Movement Commands: ")));
            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish lookmove on|off &f- Enable/disable random look movements")));
            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish lookmove fov <value> &f- Set FOV range (0.5-15.0 degrees)")));
            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish lookmove delay <min> <max> &f- Set delay range (ms)")));
            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish lookmove duration <value> &f- Set movement duration (ms)")));
            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish lookmove accel on|off &f- Enable/disable mouse acceleration")));
            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish lookmove accel <accel> <decel> &f- Set acceleration/deceleration phases (0.1-0.5)")));
            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fNote: Look movements only work when auto-catch is enabled.")));
        }
        
        /**
         * Show help for shift key commands
         */
        private void showShiftKeyHelp(ICommandSender sender) {
            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fShift Key Commands: ")));
            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish shift on|off &f- Enable/disable periodic shift key")));
            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish shift interval <min> <max> &f- Set interval in seconds (10-180)")));
            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &f- &e/gofish shift duration <min> <max> &f- Set duration in seconds (5-60)")));
            sender.addChatMessage(new ChatComponentText(formatColorCodes("&b[GoFish] &fNote: Shift key only works when auto-catch is enabled.")));
        }
        
        private void showJumpHelp(ICommandSender sender) {
            sender.addChatMessage(new ChatComponentText("§b[GoFish] §fPeriodic Jump Commands:"));
            sender.addChatMessage(new ChatComponentText("§b/gofish jump on §f- Enable periodic jumping"));
            sender.addChatMessage(new ChatComponentText("§b/gofish jump off §f- Disable periodic jumping"));
            sender.addChatMessage(new ChatComponentText("§b/gofish jump interval <min> <max> §f- Set jump interval in seconds (60-600)"));
            sender.addChatMessage(new ChatComponentText("§b/gofish jump duration <value> §f- Set jump duration in milliseconds (100-1000)"));
            
            sender.addChatMessage(new ChatComponentText("§b[GoFish] §fCurrent settings:"));
            sender.addChatMessage(new ChatComponentText("§fJumping enabled: §b" + GoFishConfig.enablePeriodicJump));
            sender.addChatMessage(new ChatComponentText("§fJump interval: §b" + 
                    GoFishConfig.minJumpInterval/1000 + "-" + GoFishConfig.maxJumpInterval/1000 + " seconds"));
            sender.addChatMessage(new ChatComponentText("§fJump duration: §b" + 
                    GoFishConfig.jumpDuration + " ms"));
            sender.addChatMessage(new ChatComponentText("§7Note: Jump and fishing actions won't overlap"));
        }
        
        private void showSafetyHelp(ICommandSender sender) {
            sender.addChatMessage(new ChatComponentText("§b[GoFish] §fSafety Feature Commands:"));
            sender.addChatMessage(new ChatComponentText("§b/gofish safety on §f- Enable safety features"));
            sender.addChatMessage(new ChatComponentText("§b/gofish safety off §f- Disable safety features"));
            sender.addChatMessage(new ChatComponentText("§b/gofish safety position <value> §f- Set position safety threshold (0.01-1.0 blocks)"));
            sender.addChatMessage(new ChatComponentText("§b/gofish safety rotation <value> §f- Set rotation safety threshold (1-15 degrees)"));
            
            sender.addChatMessage(new ChatComponentText("§b[GoFish] §fCurrent settings:"));
            sender.addChatMessage(new ChatComponentText("§fSafety features enabled: §b" + GoFishConfig.enableSafetyFeatures));
            sender.addChatMessage(new ChatComponentText("§fPosition threshold: §b" + GoFishConfig.positionSafetyThreshold + " blocks"));
            sender.addChatMessage(new ChatComponentText("§fRotation threshold: §b" + GoFishConfig.rotationSafetyThreshold + " degrees"));
            
            sender.addChatMessage(new ChatComponentText("§7Note: Auto-fishing will be disabled if player moves or looks away from fishing position"));
        }
        
        /**
         * Show help for miss chance commands
         */
        private void showMissChanceHelp(ICommandSender sender) {
            sender.addChatMessage(new ChatComponentText("§b[GoFish] §fMiss Chance Commands:"));
            sender.addChatMessage(new ChatComponentText("§b/gofish miss on §f- Enable miss chance feature"));
            sender.addChatMessage(new ChatComponentText("§b/gofish miss off §f- Disable miss chance feature"));
            sender.addChatMessage(new ChatComponentText("§b/gofish miss chance <percentage> §f- Set chance to miss (0-100%)"));
            sender.addChatMessage(new ChatComponentText("§b/gofish miss timing <early_ms> <late_ms> §f- Set timing offsets"));
            sender.addChatMessage(new ChatComponentText("§7  early_ms should be negative (ms too early)"));
            sender.addChatMessage(new ChatComponentText("§7  late_ms should be positive (ms too late)"));
            
            sender.addChatMessage(new ChatComponentText("§b[GoFish] §fCurrent settings:"));
            sender.addChatMessage(new ChatComponentText("§fMiss chance feature enabled: §b" + GoFishConfig.enableMissChance));
            sender.addChatMessage(new ChatComponentText("§fMiss chance percentage: §b" + GoFishConfig.missChancePercentage + "%"));
            sender.addChatMessage(new ChatComponentText("§fMiss timing range: §b" + 
                    GoFishConfig.minMissTimingOffset + "ms (early) to " + 
                    GoFishConfig.maxMissTimingOffset + "ms (late)"));
            
            sender.addChatMessage(new ChatComponentText("§7Note: Negative timing means catching too early, positive means too late"));
        }
        
        @Override
        public int getRequiredPermissionLevel() {
            return 0; // Everyone can use this command
        }
    }
} 