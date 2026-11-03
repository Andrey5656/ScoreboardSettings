# ScoreboardSettings

A sidebar scoreboard where every player chooses which rows they see, from a dialog menu.

Players open the menu with `/sbsettings` and turn rows on and off with one click. Hidden rows are removed and the rows
below move up. Each player's choices are saved in SQLite or MySQL.

## Features

- Scoreboard title and up to 15 lines, with `&` colors, `&#RRGGBB` hex colors and PlaceholderAPI placeholders
- A settings dialog (Paper dialog API) with one button per row, or per group of rows
- A button that turns the whole scoreboard off
- Configurable command name, aliases, button texts, ON/OFF texts, button width and columns
- Optional command when the player presses Close
- Per-player settings in SQLite or MySQL
- Folia support
- `/sbsettings reload` to reload the config without a restart

## Requirements

- Paper 1.21.7 or newer (tested on 1.21.7, 26.1.2, 26.2 and 26.3)
- Folia 26.x (tested on Canvas 26.3, a Folia fork)
- Java 21 or newer
- [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) for placeholders (optional)

The default config uses these PlaceholderAPI expansions:

```
/papi ecloud download Player
/papi ecloud download Vault
/papi ecloud download Statistic
/papi reload
```

## Installation

1. Put `ScoreboardSettings-<version>.jar` in your `plugins` folder.
2. Start the server. The plugin downloads its libraries (Kotlin, HikariCP, SQLite and MySQL drivers) on first start.
3. Edit `plugins/ScoreboardSettings/config.yml` and run `/sbsettings reload`.

## Command and permission

| Command | Permission | Description |
|---|---|---|
| `/sbsettings` | none | Opens the settings dialog |
| `/sbsettings reload` | `scoreboardsettings.reload` (op) | Reloads the config |

The command name and aliases come from `config.yml`. Changing them, or changing `storage`, needs a restart.

## Configuration

```yaml
command: sbsettings
aliases: []

storage:
  type: sqlite
  mysql:
    host: localhost
    port: 3306
    database: minecraft
    username: root
    password: ""

scoreboard:
  update-ticks: 20
  title: "&f%player_name%"
  lines:
    - "&#00FF11$ &f%vault_eco_balance_formatted%"
    - "&#FF0000🗡 &f%statistic_player_kills%"
    - "&#FA7600☠ &f%statistic_deaths%"
    - "&#FFE400⌚ &f%statistic_time_played%"

dialog:
  title: "Settings - Scoreboard"
  on: "&aON"
  off: "&cOFF"
  columns: 1
  button-width: 250
  buttons:
    scoreboard:
      text: "Scoreboard: %state%"
      row: all
    money:
      text: "Show Money: %state%"
      row: 1
    kills:
      text: "Show Kills: %state%"
      row: 2
    deaths:
      text: "Show Deaths: %state%"
      row: 3
    playtime:
      text: "Show Playtime: %state%"
      row: 4
  close:
    text: "Close"
    command: ""

messages:
  reloaded: "&aScoreboardSettings reloaded."
```

| Key | Description |
|---|---|
| `storage.type` | `sqlite` (saved in `plugins/ScoreboardSettings/data.db`) or `mysql` |
| `scoreboard.update-ticks` | How often the scoreboard updates. 20 ticks = 1 second |
| `scoreboard.lines` | The first line is row 1, the second is row 2 and so on |
| `dialog.buttons.<id>.text` | Button text. `%state%` becomes the `on` or `off` text |
| `dialog.buttons.<id>.row` | The rows the button hides: a number (`1`), a list (`[2, 3]`) or `all` for the whole scoreboard |
| `dialog.close.command` | A command the player runs when they press Close, without the slash. Empty only closes the dialog |

Each button's id (`money`, `kills`, ...) is what gets saved for the player. If you rename an id, players lose that
setting.

## Building

Requires JDK 21 or newer and Maven.

```
mvn package
```

The jar is written to `target/`.

## Support

Ask on my Discord: https://discord.gg/nRjDw9k2vJ

## License

[MIT](LICENSE)
