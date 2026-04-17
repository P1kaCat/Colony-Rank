# Colony Rank

A NeoForge mod for Minecraft 1.21.1 that ranks MineColonies colonies with a transparent scoring system and optional Discord leaderboard publishing.

## Initial Release
This repository now includes the **Initial Release** of Colony Rank (`initial-release`).

## Main Features
- Live colony ranking with `/colonyrank`
- Detailed colony score view with `/colonyscore`
- Admin tools with `/colonyadmin` (`reload`, `refresh`, `status`, `sendleaderboard`, `senddaily`, etc.)
- Score components:
  - Population
  - Buildings
  - Normalized average building level (scaled to /5)
  - Claimed chunks
  - Overall happiness
- Colony age display in ranking/details
- Discord webhook integration with update mode or daily mode
- FR/EN language support with in-game config (Fzzy Config)

## Requirements
- Minecraft `1.21.1`
- NeoForge `21.1.219`
- Java `21`
- MineColonies-compatible modpack/server

## Installation
1. Build the mod or download the release jar.
2. Put `ColonyRank-1.21.1.jar` into your `mods` folder.
3. Start the game/server once to generate config files.

## Configuration
### Language (in-game config)
- Config id: `colonyrank:settings`
- Default language: `en`
- Values: `en` or `fr`

### Discord integration
File: `config/colonyrank-discord.properties`

Example:
```properties
webhookUrl=https://discord.com/api/webhooks/PASTE_WEBHOOK_HERE
mode=daily
dailyTime=20:00
timezone=Europe/Paris
```

- `mode=update`: publish on data updates
- `mode=daily`: publish once per day at `dailyTime`

## Commands
- `/colonyrank`
- `/colonyscore list`
- `/colonyscore id <colonyId>`
- `/colonyscore <colonyName>`
- `/colonyadmin reload`
- `/colonyadmin refresh`
- `/colonyadmin status`
- `/colonyadmin sendleaderboard`
- `/colonyadmin senddaily`

## Development
```powershell
./gradlew.bat build --no-daemon
```

The project currently auto-copies the built jar to the configured local profile mods folder (see `build.gradle`).

## Changelog
See [CHANGELOG.md](CHANGELOG.md).