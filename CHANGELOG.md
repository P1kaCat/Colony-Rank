# Changelog

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