# LenientDeath

Makes deaths more forgiving by letting you keep certain types of items.

## Compatibility

Mods that add gravestones, death chests or the like will most likely have conflicts and are not supported - items may end up being duplicated or disappearing altogether, and they conflict with this mod's idea anyway (pick one or the other).

## Configuration

By default, the mod will detect armor, equipment (tools, weapons, spyglass, etc.) and foodstuffs, in addition to any item matching the config files. You can change what gets kept by editing the list of "safe" items and tags in the mod's config.

The `/ld` command lets you change the config through commands, useful for a dedicated server.
- `/ld list` - List all tags and items in the config to be kept on death.
- `/ld add hand` - Adds the item in your hand to the safe list by ID.
- `/ld add item <id>` - Add an item to the safe list by ID.
- `/ld add tag <id>` - Add a tag to the safe list by ID.
- `/ld remove hand` - Removes the item in your hand from the safe list by ID.
- `/ld remove item <id>` - Remove an item from the safe list by ID.
- `/ld remove tag <id>` - Remove a tag from the safe list by ID.
- `/ld generate` - Generates a datapack containing all items in the game which would be considered safe by the mod's auto-detection.
- `/ld erroredTags [reset]` - Prints all tag IDs that have had an error since game boot. Can be cleared using `/ld erroredTags reset`.
- `/ld autoDetect [true|false]` - Enable or disable the automatic safety detection; if false, only the items in the config will be safe. Use without an argument to query.
- `/ld trinketsSafe [true|false]` - Enable or disable [Trinkets](https://www.curseforge.com/minecraft/mc-mods/trinkets-fabric) safety; if true, all trinkets will be kept automatically, otherwise trinkets will be checked in the item/tag list. Use without an argument to query.
- `/ld listTagItems <tag>` - List all items that fall under a tag including any from sub-tags; useful for configuration.

### Examples

Adding all back slot trinkets to the safe list:
`/ld add tag #trinkets:chest/back`

Adding diamond blocks to the safe list:
`/ld add item minecraft:diamond_block`

Removing the default safe list:
`/ld remove tag #lenientdeath:safe`
