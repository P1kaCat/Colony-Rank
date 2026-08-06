# Colony Rank

A NeoForge mod for Minecraft 1.21.1 that ranks MineColonies colonies with a transparent, configurable scoring system and optional Discord leaderboard publishing.

## Main Features
- Live colony ranking with `/colonyrank`
- Detailed colony score view with `/colonyscore`
- Admin tools with `/colonyadmin` (`reload`, `refresh`, `status`, `sendleaderboard`, `senddaily`, etc.)
- In-game Fzzy Config screen for language, scoring modes, multipliers, and presets (`colonyrank:settings`)
- Two scoring modes: **OLD** (historical multiplier system) and **NEW** (cross-product system)
- 5 presets for each scoring mode (Developpement, Population, Expansion, Gestion, Metropole, etc.)
- Score components:
  - Population
  - Buildings
  - Average building level
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
2. Put `ColonyRank-1.21.1-2.0.0.jar` into your `mods` folder.
3. Start the game/server once to generate config files.

## Configuration
### Language (in-game config)
- Config id: `colonyrank:settings`
- Default language: `en`
- Values: `en` or `fr`

### Scoring Modes & Presets (in-game config)
Configured via `colonyrank:settings` in Fzzy Config. You can select between two scoring modes (**OLD** and **NEW**) and choose among 5 presets per mode.

#### 1. OLD Mode (Historical Multiplier System)
The OLD mode uses individual multipliers for each of the 5 stats with a strict quota distribution:
- **Quota requirement:** Exactly two stats at `5x`, two stats at `10x`, and one stat at `100x`.

**Formula:**  
`Score = (Population × multiplier) + (Buildings × multiplier) + (Average Building Level × multiplier) + (Claimed Chunks × multiplier) + (Overall Happiness × multiplier)`

**OLD Presets (5):**
- **Original**
- **Population**
- **Expansion**
- **Gestion**
- **Metropole**

#### 2. NEW Mode (Cross-Product System)
The NEW mode introduces cross-products linking related stats together for a dynamic score calculation.

**Formula:**  
`Score = (Pop + Bonheur + Bat + Niveau + Claims) × 5`

**NEW Presets (5):**  
All NEW preset formulas calculate component values first and multiply the overall sum by **5** at the end:

- **Developpement** (Level-focused):
  - `Pop` = Population × 5
  - `Bonheur` = PNJ × Bonheur × 0.4
  - `Bat` = Buildings × 5
  - `Niveau` = NivMoyen × Buildings × 2.5
  - `Claims` = Claimed Chunks × 0.5

- **Population** (Pop-focused):
  - `Pop` = Population × 10
  - `Bonheur` = PNJ × Bonheur × 0.6
  - `Bat` = Buildings × 5
  - `Niveau` = NivMoyen × Buildings × 1.5
  - `Claims` = Claimed Chunks × 0.5

- **Expansion** (Territory-focused):
  - `Pop` = Population × 5
  - `Bonheur` = PNJ × Bonheur × 0.4
  - `Bat` = Buildings × 8
  - `Niveau` = NivMoyen × Buildings × 1.5
  - `Claims` = Claimed Chunks × 2

- **Gestion** (Happiness-focused):
  - `Pop` = Population × 5
  - `Bonheur` = PNJ × Bonheur × 1.0
  - `Bat` = Buildings × 6
  - `Niveau` = NivMoyen × Buildings × 1.5
  - `Claims` = Claimed Chunks × 0.5

- **Metropole** (Buildings-focused):
  - `Pop` = Population × 5
  - `Bonheur` = PNJ × Bonheur × 0.4
  - `Bat` = Buildings × 12
  - `Niveau` = NivMoyen × Buildings × 1.0
  - `Claims` = Claimed Chunks × 1

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
