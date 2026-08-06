# Colony Rank

A server-side mod that ranks MineColonies colonies with a transparent, configurable scoring system and optional Discord leaderboard publishing.

Supports **two Minecraft versions**:
- **NeoForge 1.21.1** (branch `1.21.1`)
- **Forge 1.20.1** (branch `1.20.1`)

## Main Features
- Live colony ranking with `/colonyrank`
- Detailed colony score view with `/colonyscore`
- Admin tools with `/colonyadmin` (`reload`, `refresh`, `status`, `sendleaderboard`, `senddaily`, etc.)
- In-game Fzzy Config screen for language, scoring modes, multipliers, and presets (`colonyrank:settings`)
- Two scoring modes: **OLD** (historical multiplier system) and **NEW** (cross-product system)
- 5 built-in presets for NEW mode + **Custom preset** with user-defined coefficients
- **Ignored colonies** — exclude specific colonies from the ranking
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

### NeoForge 1.21.1 (branch `1.21.1`)
- Minecraft `1.21.1`
- NeoForge `21.1.219`
- Java `21`

### Forge 1.20.1 (branch `1.20.1`)
- Minecraft `1.20.1`
- Forge `47.4.10`
- Java `17`

## Installation
1. Build the mod or download the release jar for your Minecraft version.
2. Put the jar into your `mods` folder.
3. Start the game/server once to generate config files.

## Configuration
### Language (in-game config)
- Config id: `colonyrank:settings`
- Default language: `en`
- Values: `en` or `fr`

### Scoring Modes & Presets (in-game config)
Configured via `colonyrank:settings` in Fzzy Config. You can select between two scoring modes (**OLD** and **NEW**) and choose among presets.

#### 1. OLD Mode (Historical Multiplier System)
The OLD mode uses individual multipliers for each of the 5 stats with a strict quota distribution:
- **Quota requirement:** Exactly two stats at `5x`, two stats at `10x`, and one stat at `100x`.

**Formula:**  
`Score = (Population × multiplier) + (Buildings × multiplier) + (Average Building Level × multiplier) + (Claimed Chunks × multiplier) + (Overall Happiness × multiplier)`

#### 2. NEW Mode (Cross-Product System)
The NEW mode introduces cross-products linking related stats together for a dynamic score calculation.

**Formula:**  
`Score = (Pop + Bonheur + Bat + Niveau + Claims) × 5`

All NEW preset formulas calculate component values first and multiply the overall sum by **5** at the end:

| Preset | Pop coef | Happiness coef | Buildings coef | Level coef | Claims coef |
|---|---|---|---|---|---|
| **Developpement** | 5 | 0.4 | 5 | 2.5 | 0.5 |
| **Population** | 10 | 0.6 | 5 | 1.5 | 0.5 |
| **Expansion** | 5 | 0.4 | 8 | 1.5 | 2 |
| **Gestion** | 5 | 1.0 | 6 | 1.5 | 0.5 |
| **Metropole** | 5 | 0.4 | 12 | 1.0 | 1 |

**Component formulas:**
- `Pop` = Population × popCoef
- `Bonheur` = PNJ × Bonheur × happinessCoef
- `Bat` = Buildings × buildingCoef
- `Niveau` = NivMoyen × Buildings × levelCoef
- `Claims` = Claimed Chunks × claimsCoef

#### 3. Custom Preset (NEW mode)
Choose `custom` as the preset to define your own coefficients. The 5 coefficients are editable in the Fzzy Config screen or via `/colonyadmin preset custom`.

Defaults match the Developpement preset:
- `customPopCoef` = 5.0
- `customHappinessCoef` = 0.4
- `customBuildingCoef` = 5.0
- `customLevelCoef` = 2.5
- `customClaimsCoef` = 0.5

### Ignored Colonies
You can exclude specific colonies from the ranking, JSON export, and Discord publishing. Ignored colonies are stored in `config/colonyrank/ignored_colonies.json`.

Commands:
- `/colonyadmin ignore <colonyId>` — ignore a colony
- `/colonyadmin unignore <colonyId>` — restore a colony
- `/colonyadmin ignorelist` — list all ignored colonies

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

### Player commands
- `/colonyrank` — show the live colony ranking
- `/colonyscore list` — list all colonies
- `/colonyscore id <colonyId>` — show details for a colony by ID
- `/colonyscore <colonyName>` — show details for a colony by name

### Admin commands (`/colonyadmin`)
| Command | Description |
|---|---|
| `reload` | Reload all colonies from the server |
| `refresh` | Recalculate scores and save |
| `clear` | Clear the colony cache |
| `status` | Show mod status (colonies, scoring mode, ignored count, etc.) |
| `export` | Export colony data to JSON |
| `sendleaderboard` | Send the leaderboard to Discord now |
| `senddaily` | Force a daily Discord send |
| `discordstatus` | Show Discord webhook status |
| `scoringmode <old\|new>` | Switch scoring system |
| `preset <name>` | Set NEW mode preset (`developpement`, `population`, `expansion`, `gestion`, `metropole`, `custom`) |
| `ignore <colonyId>` | Exclude a colony from the ranking |
| `unignore <colonyId>` | Restore an ignored colony |
| `ignorelist` | List all ignored colonies |
| `help` | Show all admin commands |

## Development
```powershell
./gradlew.bat build --no-daemon
```

The project auto-copies the built jar to the configured local modpack mods folder (see `build.gradle`).

## Changelog
See [CHANGELOG.md](CHANGELOG.md).
