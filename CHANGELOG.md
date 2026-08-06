# Changelog

## [2.0.0] - 2026-08-06
- Complete scoring system overhaul with two independent modes: OLD and NEW.
- OLD mode: Preserves the historical multiplier-based system (quota: 2x x5, 2x x10, 1x x100).
- NEW mode: Cross-product scoring system linking stats (Population x Happiness, AvgLevel x Buildings).
  - Score formula: Score = (Population + Happiness + Buildings + Level + Claims) x 5
  - Cross-products: Bonheur = PNJ x Bonheur x coef, Niveau = Niveau moyen x Bâtiments x coef
- Added 5 NEW presets: Developpement, Population, Expansion, Gestion, Metropole.
- Added 5 OLD presets: Original, Population, Expansion, Gestion, Metropole.
- Integrated scoring mode and preset selection via Fzzy Config.
- Updated score breakdown display for both modes.
- Updated localization (EN/FR) for new config entries and scoring messages.
- Updated jar filename to ColonyRank-1.21.1-2.0.0.jar.

## [1.1.0] - 2026-06-23
- Added an in-game Fzzy Config screen for language and score settings.
- Added configurable score multipliers for population, buildings, average building level, claimed chunks, and overall happiness.
- Enforced multiplier quota across the five slots: exactly `2x5`, `2x10`, and `1x100`.
- Wired score calculation and score breakdown display to the new config values.
- Added localized FR/EN labels and descriptions for the new config entries.
- Updated the built jar filename to `ColonyRank-1.21.1-1.1.0.jar`.

## [1.0.1] - 2026-05-07
- Fixed dedicated server startup crash caused by loading the client-only config screen on the server.
- Changed the built jar filename to `ColonyRank-1.21.1-1.0.1.jar`.

## [Initial Release] - 2026-04-17
### Initial Release
- Added colony ranking command: `/colonyrank`
- Added colony detail commands: `/colonyscore list`, `/colonyscore id <id>`, `/colonyscore <name>`
- Added admin command suite: `/colonyadmin` (`reload`, `refresh`, `clear`, `status`, `export`, `discordstatus`, `sendleaderboard`, `senddaily`)
- Implemented scoring model with population, building count, normalized building levels, claimed chunks, and overall happiness
- Added colony age display in ranking/details and Discord output
- Added Discord webhook integration with:
  - update mode
  - daily scheduling mode (`dailyTime` + timezone)
  - daily debug/status helpers
- Added FR/EN localization for in-game messages and Discord output
- Added Fzzy Config in-game config screen and language option (`colonyrank:settings`)
- Set default language to English (`en`)
- Added Gradle build auto-copy task for local mods folder workflow
