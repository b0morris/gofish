package com.example.gofish.handlers;

import com.example.gofish.config.GoFishConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**
 * Handler for key bindings in the GoFish mod
 */
public class KeyBindingHandler {
    
    private static final KeyBinding TOGGLE_AUTO_FISH = new KeyBinding("Toggle Auto-Fish", Keyboard.KEY_F, "GoFish");
    private static final KeyBinding TOGGLE_PACKET_LOGGING = new KeyBinding("Toggle Packet Logging", Keyboard.KEY_P, "GoFish");
    
    private final PacketHandler packetHandler;
    
    public KeyBindingHandler(PacketHandler packetHandler) {
        this.packetHandler = packetHandler;
        ClientRegistry.registerKeyBinding(TOGGLE_AUTO_FISH);
        ClientRegistry.registerKeyBinding(TOGGLE_PACKET_LOGGING);
        FMLCommonHandler.instance().bus().register(this);
    }
    
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (TOGGLE_AUTO_FISH.isPressed()) {
            GoFishConfig.enableAutoCatch = !GoFishConfig.enableAutoCatch;
            GoFishConfig.saveConfig();
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.AQUA + "[GoFish] " + EnumChatFormatting.WHITE + "Auto-fishing " +
                            (GoFishConfig.enableAutoCatch ? "enabled" : "disabled") + "."
            ));
        }
        
        if (TOGGLE_PACKET_LOGGING.isPressed()) {
            packetHandler.startPacketLogging();
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.AQUA + "[GoFish] " + EnumChatFormatting.WHITE + "Started packet logging for 5 seconds."
            ));
        }
    }
} 