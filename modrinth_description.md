# Donut Hoppers

Donut Hoppers is a premium-grade Paper plugin for Minecraft 1.21+ that upgrades all vanilla hoppers into faster, optimized transfer machines without breaking vanilla automation rules.

## Features

- All vanilla hopper blocks work as fast Donut Hoppers automatically.
- Default speed multiplier is 12x, configurable in `config.yml`.
- Preserves strict vanilla furnace insertion/extraction rules:
  - top insertion only into source/input slot
  - side insertion only into fuel slot
  - bottom extraction only from output slot
- Supports Crafter inventories while respecting open/blocked slots.
- Configurable chunk placement limit to prevent hopper spam and lag.
- Includes `/hoppers reload` and `/hoppers info` commands.
- Uses bStats for anonymous usage metrics.

## Why use Donut Hoppers?

This plugin is designed for servers that need fast item transfer without the common automation bugs found in other "fast hopper" plugins. It focuses on performance and vanilla-compatible inventory interactions.

## Installation

1. Download the plugin jar.
2. Place it in your server's `plugins/` directory.
3. Start or restart your Paper server.
4. Edit `plugins/Donut Hoppers/config.yml` to adjust the speed multiplier and chunk limit.

## Configuration

The config supports:

- `hopper-speed-multiplier` — speed multiplier for hopper transfers.
- `chunk-limit-enabled` — enable or disable the per-chunk hopper placement limit.
- `chunk-placement-limit` — maximum number of hoppers allowed per chunk.
- `chunk-limit-message` — chat message when a player exceeds the hopper limit.
- `hopper-process-interval-ticks` — internal processing interval for hopper updates.

## Commands

- `/hoppers info` — displays current multiplier and chunk limit settings.
- `/hoppers reload` — reloads the plugin config and messages.

## Permissions

- `hoppers.info` — allows use of `/hoppers info`
- `hoppers.reload` — allows use of `/hoppers reload`
