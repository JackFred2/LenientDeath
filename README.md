# LenientDeath

Makes deaths more forgiving by letting you keep certain types of items.

## Compatibility

Mods that add gravestones, death chests or the like will most likely have conflicts and are not supported - items may end up being duplicated or disappearing altogether, and they conflict with this mod's idea anyway (pick one or the other).

## Configuration

By default, the mod will detect armor, equipment (tools, weapons, spyglass, etc.) and foodstuffs, in addition to any item matching the config files. You can change what gets kept by editing the list of "safe" items and tags in the mod's config.

The `/ld` command lets you change the config through commands, useful for a dedicated server.

  - `/ld list` - List all tags and items in the config to be kept on death.
  - `/ld add <item|tag>` - Add a tag or item to the mod config.
  - `/ld remove <item|tag>` - Remove a tag or item from the mod config.
  - `/ld generate` - Generate a set of tag files for equipment, food and armor based off the same criteria as the automatic detection. Useful if you're using manual detecting.
  - `/ld resetErroredTags` - To prevent log spam and performance issues, if LenientDeath can't find a tag in the config then it won't be checked again until game restart. You can clear this list manually if you have changed your datapacks (and tags).

### Examples

Adding all back [trinkets](https://www.curseforge.com/minecraft/mc-mods/trinkets-fabric) to the safe list:
`/ld add #trinkets:chest/back`

Adding diamond blocks to the safe list:
`/ld add minecraft:diamond_block`

Removing food from the safe list:
`/ld remove #lenientdeath:foods`
