# GoFish - Hypixel SkyBlock Fishing Helper

A Minecraft Forge mod for Minecraft 1.8.9 that enhances the fishing experience in Hypixel SkyBlock.

## Features

- **Fishing Detection**: Detects when a fish is on your hook by monitoring network packets
- **Chat Notifications**: Provides custom notifications for:
  - Fish caught
  - Sea creatures appearing
  - Treasure found
- **Sound Alerts**: Plays distinct sounds for different fishing events
- **Fully Configurable**: All features can be enabled/disabled in the config

## Installation

1. Install Minecraft Forge for version 1.8.9
2. Download the latest release of GoFish from the releases page
3. Place the downloaded JAR file in your Minecraft mods folder
4. Launch Minecraft with the Forge profile

## Configuration

The configuration file is located at `config/gofish.cfg` in your Minecraft directory. You can edit it directly or use the in-game Forge config menu.

### General Settings

- `enableNotifications` - Enable/disable all notifications (default: true)
- `playSoundOnFishCaught` - Play a sound when a fish is caught (default: true)
- `playSoundOnSeaCreature` - Play a sound when a sea creature appears (default: true)

### Notification Settings

- `showFishCaughtMessages` - Show messages when fish are caught (default: true)
- `showSeaCreatureMessages` - Show messages when sea creatures appear (default: true)
- `showTreasureMessages` - Show messages when treasure is found (default: true)

## How It Works

GoFish uses packet interception to detect fishing events in Hypixel SkyBlock:

1. It monitors for the splash sound packet that occurs when a fish bites
2. It checks chat messages for fishing-related notifications
3. It verifies the player is actually fishing and on Hypixel SkyBlock

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

Contributions are welcome! Feel free to submit a pull request or open an issue if you have any suggestions or find any bugs. 