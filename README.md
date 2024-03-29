# Lenient Death

For information on the legacy 0.x version, see the [branch README](https://github.com/JackFred2/LenientDeath/tree/legacy-1.x).

Server-sided alternative to gravestone mods, for the Fabric  & Quilt loaders.

![A GIF showing a player dying and keeping their armour and tools, but not ores](https://i.imgur.com/dVXWpMb.gif)

![An example of the item glow up close](https://i.imgur.com/Ps0mbnO.png)

## Features

All features are toggleable and highly configurable. Out of the box, you'll get:

- Preserve only some items on death - think of it as a _filtered_ `/gamerule keepInventory`.
  - Multiple criteria, including a manual safe list, by item type, ad-hoc soulbound system, and optionally by pure chance.
  - For more details, see [the wiki page](https://github.com/JackFred2/LenientDeath/wiki/Preserve-Items-on-Death).
  - Inspired by Starbound's death system.
- Give dropped items a glowing outline, helping you find your way back. Recommended to increase your Entity View Distance!
- Void Immunity for items - they will be teleported to a safe place instead of despawning.
- Moving of your items to their original slots.
- Announcement of your death coordinates.
- Small API for working with Lenient Death in your own mod.

Default disabled, but can be enabled:

- Several config presets to get you started - use `/ld config presets`. See [the wiki page](https://github.com/JackFred2/LenientDeath/wiki#presets)
- Preserve some or all of your experience on death.
- Item & XP drops can work on a per-player basis too - see [the wiki page on Per-Player mode](https://github.com/JackFred2/LenientDeath/wiki/Per-Player)
- Give items an extended (or infinite!) lifespan, putting less pressure on players.
- Add immunities to your items for fire, lava, explosions and cacti.

## Planned

- Client-side GUI for configuring the mod - for now, use the command or a text editor.

- Immunities for item entities: Mob Pickups (maybe)

## TODO (rough priority)

- Death Coordinates: send to team

- Icon, current one is bad
