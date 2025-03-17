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
import com.example.gofish.config.GoFishConfig;
import com.example.gofish.utils.FishingUtils;

@Mod(modid = GoFishMod.MODID, version = GoFishMod.VERSION)
public class GoFishMod
{
    public static final String MODID = "gofish";
    public static final String VERSION = "1.0";
    
    // Store handlers as instance variables for better coordination
    private PacketHandler packetHandler;
    private ChatHandler chatHandler;
    private FishingHandler fishingHandler;
    private KeyBindingHandler keyBindingHandler;
    
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
            
            // Connect handlers
            FishingUtils.setPacketHandler(packetHandler);
            
            // Register our handlers
            MinecraftForge.EVENT_BUS.register(packetHandler);
            MinecraftForge.EVENT_BUS.register(chatHandler);
            MinecraftForge.EVENT_BUS.register(fishingHandler);
            MinecraftForge.EVENT_BUS.register(keyBindingHandler);
            
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
            ClientCommandHandler.instance.registerCommand(new CommandGoFish());
            
            // some example code
            System.out.println("DIRT BLOCK >> "+Blocks.dirt.getUnlocalizedName());
        } catch (Exception e) {
            // Log any errors during initialization
            System.err.println("[GoFish] Error during init: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Debug command for GoFish
     */
    public static class CommandGoFish extends CommandBase {
        @Override
        public String getCommandName() {
            return "gofish";
        }
        
        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/gofish [debug|reload|autocatch|packetlogging]";
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
                    sender.addChatMessage(new ChatComponentText("§b[GoFish] §fAvailable commands:"));
                    sender.addChatMessage(new ChatComponentText("§b[GoFish] §f- §e/gofish reload §f- Reload configuration"));
                    sender.addChatMessage(new ChatComponentText("§b[GoFish] §f- §e/gofish autocatch [on|off] §f- Toggle auto-catch"));
                    sender.addChatMessage(new ChatComponentText("§b[GoFish] §f- §e/gofish packetlogging [on|off] §f- Toggle packet logging"));
                    return;
                }
                
                String subCommand = args[0].toLowerCase();
                
                if (subCommand.equals("debug")) {
                    // Show debug info
                    boolean isOnHypixel = FishingUtils.isOnHypixel();
                    boolean isOnSkyblock = FishingUtils.isOnHypixelSkyblock();
                    boolean isFishing = FishingUtils.isPlayerFishing();
                    
                    sender.addChatMessage(new ChatComponentText("§b[GoFish] §fDebug Information:"));
                    sender.addChatMessage(new ChatComponentText("§b[GoFish] §fOn Hypixel: " + (isOnHypixel ? "§aYes" : "§cNo")));
                    sender.addChatMessage(new ChatComponentText("§b[GoFish] §fOn SkyBlock: " + (isOnSkyblock ? "§aYes" : "§cNo")));
                    sender.addChatMessage(new ChatComponentText("§b[GoFish] §fCurrently Fishing: " + (isFishing ? "§aYes" : "§cNo")));
                    sender.addChatMessage(new ChatComponentText("§b[GoFish] §fAuto-Catch: " + (GoFishConfig.enableAutoCatch ? "§aEnabled" : "§cDisabled")));
                    
                    if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().getCurrentServerData() != null) {
                        String serverIP = Minecraft.getMinecraft().getCurrentServerData().serverIP;
                        if (serverIP != null) {
                            sender.addChatMessage(new ChatComponentText("§b[GoFish] §fServer IP: §e" + serverIP));
                        }
                    }
                    
                    // Show scoreboard info if available
                    if (Minecraft.getMinecraft() != null && 
                        Minecraft.getMinecraft().theWorld != null && 
                        Minecraft.getMinecraft().theWorld.getScoreboard() != null) {
                        
                        sender.addChatMessage(new ChatComponentText("§b[GoFish] §fScoreboard Objectives:"));
                        for (net.minecraft.scoreboard.ScoreObjective objective : 
                             Minecraft.getMinecraft().theWorld.getScoreboard().getScoreObjectives()) {
                            
                            if (objective != null) {
                                sender.addChatMessage(new ChatComponentText("§b[GoFish] §f - " + 
                                    objective.getName() + " (" + objective.getDisplayName() + ")"));
                            }
                        }
                    }
                    
                    // Show fishing hook info if available
                    EntityFishHook fishHook = FishingUtils.getFishingHook();
                    if (fishHook != null) {
                        sender.addChatMessage(new ChatComponentText("§b[GoFish] §fFishing Hook Info:"));
                        sender.addChatMessage(new ChatComponentText("§b[GoFish] §f - Position: " + 
                            String.format("%.2f, %.2f, %.2f", fishHook.posX, fishHook.posY, fishHook.posZ)));
                        sender.addChatMessage(new ChatComponentText("§b[GoFish] §f - Motion: " + 
                            String.format("%.4f, %.4f, %.4f", fishHook.motionX, fishHook.motionY, fishHook.motionZ)));
                        sender.addChatMessage(new ChatComponentText("§b[GoFish] §f - In Water: " + 
                            (fishHook.isInWater() ? "§aYes" : "§cNo")));
                        sender.addChatMessage(new ChatComponentText("§b[GoFish] §f - In Lava: " + 
                            (fishHook.isInLava() ? "§aYes" : "§cNo")));
                    }
                } else if (subCommand.equals("reload")) {
                    // Reload config
                    GoFishConfig.loadConfig();
                    sender.addChatMessage(new ChatComponentText("§b[GoFish] §fConfiguration reloaded!"));
                } else if (subCommand.equals("autocatch")) {
                    // Toggle auto-catch
                    if (args.length > 1) {
                        String option = args[1].toLowerCase();
                        if (option.equals("on") || option.equals("enable") || option.equals("true")) {
                            GoFishConfig.enableAutoCatch = true;
                            sender.addChatMessage(new ChatComponentText("§b[GoFish] §aAuto-catch enabled!"));
                        } else if (option.equals("off") || option.equals("disable") || option.equals("false")) {
                            GoFishConfig.enableAutoCatch = false;
                            sender.addChatMessage(new ChatComponentText("§b[GoFish] §cAuto-catch disabled!"));
                        } else {
                            sender.addChatMessage(new ChatComponentText("§b[GoFish] §cInvalid option. Use 'on' or 'off'."));
                        }
                    } else {
                        // Toggle current state
                        GoFishConfig.enableAutoCatch = !GoFishConfig.enableAutoCatch;
                        sender.addChatMessage(new ChatComponentText("§b[GoFish] §fAuto-catch " + 
                            (GoFishConfig.enableAutoCatch ? "§aenabled" : "§cdisabled") + "§f!"));
                    }
                    
                    // Save the config
                    GoFishConfig.saveConfig();
                } else if (subCommand.equals("packetlogging")) {
                    // Toggle packet logging
                    if (args.length > 1) {
                        String option = args[1].toLowerCase();
                        if (option.equals("on") || option.equals("enable") || option.equals("true")) {
                            GoFishConfig.enablePacketLogging = true;
                            sender.addChatMessage(new ChatComponentText("§b[GoFish] §aPacket logging enabled!"));
                        } else if (option.equals("off") || option.equals("disable") || option.equals("false")) {
                            GoFishConfig.enablePacketLogging = false;
                            sender.addChatMessage(new ChatComponentText("§b[GoFish] §cPacket logging disabled!"));
                        } else {
                            sender.addChatMessage(new ChatComponentText("§b[GoFish] §cInvalid option. Use 'on' or 'off'."));
                        }
                    } else {
                        // Toggle current state
                        GoFishConfig.enablePacketLogging = !GoFishConfig.enablePacketLogging;
                        sender.addChatMessage(new ChatComponentText("§b[GoFish] §fPacket logging " + 
                            (GoFishConfig.enablePacketLogging ? "§aenabled" : "§cdisabled") + "§f!"));
                    }
                    
                    // Save the config
                    GoFishConfig.saveConfig();
                } else {
                    sender.addChatMessage(new ChatComponentText("§b[GoFish] §cUnknown command. Use /gofish for help."));
                }
            } catch (Exception e) {
                // Log any errors but don't crash the game
                System.err.println("[GoFish] Error processing command: " + e.getMessage());
                sender.addChatMessage(new ChatComponentText("§b[GoFish] §cAn error occurred while processing the command."));
            }
        }
        
        @Override
        public int getRequiredPermissionLevel() {
            return 0; // No permission level required
        }
    }
} 