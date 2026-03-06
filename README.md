# HyperRewards

[![Latest Release](https://img.shields.io/github/v/release/HyperSystems-Development/HyperRewards?label=version)](https://github.com/HyperSystems-Development/HyperRewards/releases)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)
[![Discord](https://img.shields.io/badge/Discord-Join%20Us-7289DA?logo=discord&logoColor=white)](https://discord.com/invite/aZaa5vcFYh)
[![GitHub Stars](https://img.shields.io/github/stars/HyperSystems-Development/HyperRewards?style=social)](https://github.com/HyperSystems-Development/HyperRewards)

**Advanced playtime tracking with rewards, milestones, leaderboards, and live stats for Hytale servers.** Track every session down to the millisecond, reward your players automatically, and give them reasons to keep coming back.

**[Discord](https://discord.com/invite/aZaa5vcFYh)** | **[Releases](https://github.com/HyperSystems-Development/HyperRewards/releases)** | **[Issues](https://github.com/HyperSystems-Development/HyperRewards/issues)**

## Features

**Playtime Rewards** — Automatically grant rewards when players hit playtime thresholds. Run any console command as a reward: give items, currency, titles, or anything your server supports. Rewards can be one-time or **repeatable** per period, so daily/weekly incentives just work.

**Per-World Rewards** — Scope rewards to specific worlds. A reward set to `world: "survival"` only triggers for players in that world. Leave it blank to apply everywhere.

**Milestones** — Long-term progression system separate from rewards. Milestones can grant **HyperPerms permissions**, add players to **permission groups**, execute commands, send private messages, and broadcast announcements. Repeatable milestones reset each period.

**Live Leaderboards** — Real-time rankings that combine database history with active session data. Available as chat text or an interactive GUI. Supports **daily**, **weekly**, **monthly**, and **all-time** periods.

**Player Lookup** — Check any player's total playtime with `/playtime check <player>`. Works for offline players too.

**Rest Reminders** — Configurable periodic messages reminding players to take breaks after extended sessions. Interval and message are fully customizable.

**Crash-Safe Sessions** — Active sessions are saved periodically (~1 minute intervals) so player data survives unexpected server crashes or restarts. No more lost playtime.

**HyperPerms Integration** — Optional deep integration with [HyperPerms](https://github.com/HyperSystems-Development/HyperPerms). Grant permissions, assign groups, and check permission nodes through milestones. Fully reflection-based — runs standalone without HyperPerms installed.

**Dual Database** — SQLite out of the box, MySQL for networks. HikariCP connection pooling with automatic retry logic and exponential backoff. Database indexes on all hot query paths.

**Fully Configurable** — Every message, command name, alias, color, period label, and GUI string is customizable. Full `&` color code support with hex colors.

## Quick Start

1. Drop `HyperRewards.jar` in your `mods/` folder
2. Start your server
3. Run `/playtime` to check your stats, or `/playtime help` for all commands

```
/playtime                        # Check your playtime
/playtime top daily              # Daily leaderboard
/playtime rewards                # View available rewards
/playtime milestones             # Track milestone progress
/playtime check Steve            # Look up another player
```

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/playtime` | `playtime.check` | Check your total playtime |
| `/playtime check <player>` | `playtime.check.others` | View another player's playtime |
| `/playtime rewards` | `playtime.check` | List rewards and your claim status |
| `/playtime milestones` | `playtime.milestones` | View milestone progress |
| `/playtime top [period]` | `playtime.top` | Leaderboard (daily/weekly/monthly/all) |
| `/playtime menu` | `playtime.gui` | Open the GUI leaderboard |
| `/playtime version` | — | Show plugin and integration info |
| `/playtime help` | — | Show command help |
| `/playtime reload` | `playtime.reload` | Reload configuration |
| `/playtime admin` | `playtime.admin` | Show reward creation guide |
| `/playtime admin addReward <id> <period> <time> <cmd>` | `playtime.admin` | Add a new reward |
| `/playtime admin removeReward <id>` | `playtime.admin` | Remove a reward |
| `/playtime admin listRewards` | `playtime.admin` | List all configured rewards |
| `/playtime admin listMilestones` | `playtime.admin` | List all configured milestones |

**Aliases:** `/pt`, `/play`, `/time` (configurable)

<details>
<summary><strong>All Permissions</strong></summary>

| Permission | Description |
|------------|-------------|
| `playtime.check` | Check own playtime and view rewards |
| `playtime.check.others` | Look up another player's playtime |
| `playtime.top` | View leaderboards |
| `playtime.gui` | Open the GUI leaderboard |
| `playtime.milestones` | View milestone progress |
| `playtime.reload` | Reload configuration |
| `playtime.admin` | Full admin access (add/remove rewards, list config) |

</details>

## Configuration

Config file: `mods/HyperRewards/config.json` (auto-generated on first run)

<details>
<summary><strong>Database</strong></summary>

SQLite (default) requires zero configuration. For MySQL:

```json
{
  "database": {
    "type": "mysql",
    "host": "localhost",
    "port": 3306,
    "databaseName": "hyperrewards",
    "username": "root",
    "password": "",
    "useSSL": false
  }
}
```

</details>

<details>
<summary><strong>Rewards</strong></summary>

Add rewards via `/playtime admin addReward` or directly in config:

```json
{
  "rewards": [
    {
      "id": "daily_gold",
      "period": "daily",
      "timeRequirement": 3600000,
      "commands": ["give %player% gold 100"],
      "broadcastMessage": "&6%player% &eclaimed the &6%reward% &ereward!",
      "repeatable": true,
      "world": ""
    },
    {
      "id": "survival_veteran",
      "period": "all",
      "timeRequirement": 36000000,
      "commands": ["give %player% diamond_sword 1"],
      "broadcastMessage": "&6%player% &eis a Survival Veteran!",
      "repeatable": false,
      "world": "survival"
    }
  ]
}
```

- **`period`** — `daily`, `weekly`, `monthly`, or `all`
- **`timeRequirement`** — Milliseconds (1h = 3600000, 1d = 86400000)
- **`repeatable`** — `true` resets each period, `false` is one-time
- **`world`** — Restrict to a specific world, or `""` / `null` for all worlds
- **`%player%`** — Replaced with the player's username in commands and messages

</details>

<details>
<summary><strong>Milestones</strong></summary>

Milestones are progression goals with rich actions:

```json
{
  "milestones": {
    "enabled": true,
    "list": [
      {
        "id": "10h_veteran",
        "timeRequirement": 36000000,
        "period": "all",
        "grantPermissions": ["server.veteran.tag"],
        "addToGroup": "veteran",
        "broadcastMessage": "&6%player% &ehas reached &610 hours &eof playtime!",
        "privateMessage": "&aCongratulations! You've earned the Veteran rank!",
        "commands": ["give %player% emerald 64"],
        "repeatable": false
      }
    ]
  }
}
```

- **`grantPermissions`** — Grants permissions via HyperPerms (if installed)
- **`addToGroup`** — Adds the player to a HyperPerms group
- **`broadcastMessage`** — Public announcement (null to disable)
- **`privateMessage`** — Message only the player sees (null to disable)
- **`commands`** — Console commands to execute
- **`repeatable`** — If `true`, can be re-triggered each period reset

</details>

<details>
<summary><strong>Rest Reminders</strong></summary>

```json
{
  "restReminder": {
    "enabled": true,
    "intervalMs": 7200000,
    "message": "&e[HyperRewards] &7You've been playing for &e%session_time%&7. Consider taking a break!"
  }
}
```

Sends a reminder after every `intervalMs` of continuous play (default: 2 hours).

</details>

<details>
<summary><strong>Messages & GUI</strong></summary>

Every player-facing string is configurable with `&` color codes:

```json
{
  "messages": {
    "selfCheck": "&dTotal Playtime: &e%time%",
    "otherCheck": "&d%player%'s Playtime: &e%time%",
    "leaderboardHeader": "&6--- Playtime Leaderboard (&e%period_name%&6) ---",
    "leaderboardEntry": "&6#%rank% &e%player% &7: &f%time%",
    "rewardBroadcast": "&6%player% &ehas played for &6%time% &eand claimed the &6%reward% &ereward!"
  },
  "command": {
    "name": "playtime",
    "aliases": ["pt", "play", "time"],
    "topStyle": "text"
  },
  "gui": {
    "title": "LEADERBOARD",
    "buttonAll": "ALL TIME",
    "buttonDaily": "DAILY",
    "buttonWeekly": "WEEKLY",
    "buttonMonthly": "MONTHLY"
  }
}
```

Set `"topStyle": "gui"` to open the interactive leaderboard GUI when players run `/playtime top`.

</details>

<details>
<summary><strong>HyperPerms Integration</strong></summary>

```json
{
  "integrations": {
    "hyperPermsEnabled": true
  }
}
```

When [HyperPerms](https://github.com/HyperSystems-Development/HyperPerms) is installed, milestones can grant permissions and assign groups automatically. The integration is reflection-based and fully optional — HyperRewards runs standalone without it.

Check integration status in-game with `/playtime version`.

</details>

## For Developers

<details>
<summary><strong>API Usage</strong></summary>

```java
HyperRewardsAPI api = HyperRewardsAPI.get();

// Get player playtime (returns milliseconds)
long total = api.getPlaytime(uuid, "all");
long daily = api.getPlaytime(uuid, "daily");

// Format time for display
String formatted = api.formatTime(total); // "2h 15m 30s"
```

</details>

<details>
<summary><strong>Building from Source</strong></summary>

**Requirements:** Java 25, Gradle 9.2+

All dependencies resolve automatically. The Hytale Server API comes from `maven.hytale.com`.

```bash
./gradlew shadowJar
# Output: build/libs/HyperRewards-<version>.jar
```

Build variants:

```bash
./gradlew buildRelease       # Build against latest Hytale release
./gradlew buildPreRelease    # Build against latest Hytale pre-release
./gradlew buildDev           # Dev build with version 0.0.0
```

</details>

## Acknowledgements

HyperRewards is a continuation of [**Advanced Playtime**](https://github.com/ItsZib/AdvancedPlaytime) by [**ItsZib**](https://github.com/ItsZib). The original project laid an amazing foundation — session tracking, leaderboards, rewards, the GUI system, and MySQL support were all built by ItsZib. We're grateful for the privilege of continuing this project and building on that work.

Contributors to the original project: [shreyjain14](https://github.com/shreyjain14) (MySQL support fix).

## Links

- [Discord](https://discord.com/invite/aZaa5vcFYh) — Support & community
- [Issues](https://github.com/HyperSystems-Development/HyperRewards/issues) — Bug reports & feature requests
- [Releases](https://github.com/HyperSystems-Development/HyperRewards/releases) — Downloads
- [Original Project](https://github.com/ItsZib/AdvancedPlaytime) — Advanced Playtime by ItsZib

---

Part of the **[HyperSystems](https://github.com/HyperSystems-Development)** suite: [HyperPerms](https://github.com/HyperSystems-Development/HyperPerms) | [HyperRewards](https://github.com/HyperSystems-Development/HyperRewards) | [HyperEssentials](https://github.com/HyperSystems-Development/HyperEssentials) | [HyperFactions](https://github.com/HyperSystems-Development/HyperFactions)
