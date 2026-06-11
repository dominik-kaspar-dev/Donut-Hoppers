# Donut Hoppers

Donut Hoppers is a premium Paper plugin for Minecraft 1.21+ that makes every vanilla hopper transfer items faster without breaking vanilla automation rules.

## Features

- Converts all vanilla hoppers into optimized fast hoppers automatically.
- Default 12x transfer speed, configurable in `config.yml`.
- Preserves vanilla furnace behavior for top/side/bottom transfers.
- Supports Crafter inventories while respecting blocked slots.
- Chunk placement limit to prevent hopper spam and lag.
- Includes `/hoppers info` and `/hoppers reload` commands.
- Uses bStats for anonymous usage tracking.

## Installation

1. Place the plugin jar into your server's `plugins/` folder.
2. Start or restart your Paper server.
3. Edit `plugins/Donut Hoppers/config.yml` for speed and chunk limit settings.

## Configuration

The plugin supports the following options in `config.yml`:

- `hopper-speed-multiplier`: default 12.0
- `chunk-limit-enabled`: `true` or `false`
- `chunk-placement-limit`: max hoppers allowed per chunk
- `chunk-limit-message`: custom limit warning message
- `hopper-process-interval-ticks`: how often hopper updates run

## Commands

- `/hoppers info` - shows current multiplier and chunk limit status
- `/hoppers reload` - reloads configuration and messages

## Permissions

- `hoppers.info`
- `hoppers.reload`
