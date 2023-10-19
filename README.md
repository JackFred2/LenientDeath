# Lenient Death

Server-sided Fabric & Quilt mod to make death less frustrating.

## Planned

Client-side GUI config

Presets

Immunities for item entities: Void, Fire

Do Wiki Documentation

Icon!

Update Modrinth & Curse Pages

## Command

Base command is `/lenientdeath`, default alias is `/ld`.

Where noted, 'Operators' mean anyone with a permission level greater than or equal to 4.

### Per Player

**Command**: `/lenientdeath perPlayer`

Manages a player's per-player item preservation setting. Only enabled if `preserveItemsOnDeath.perPlayer.enabled` is true.

#### Check Self

**Command**: `/lenientdeath perPlayer check`

**Permission**: `lenientdeath.command.checkSelf`

**Non-permissions access**: Everyone

**Description**: Allows a player to check their own per-player status.

#### Check Others

**Command**: `/lenientdeath perPlayer check <player>`

**Permission**: `lenientdeath.command.checkOthers`

**Non-permissions access**: Operators

**Description**: Allows a player to check other players' per-player status.

**Implies**: Check Self

#### Change Self

**Command**: `/lenientdeath perPlayer <enable|disable>`

**Permission**: `lenientdeath.command.changeSelf`

**Non-permissions access**: Operators, or everyone if `preserveItemsOnDeath.perPlayer.playersCanChangeTheirOwnSetting` is true.

**Description**: Allows a player to change their own per-player status.

**Implies**: Check Self

#### Change Others

**Command**: `/lenientdeath perPlayer <enable|disable> <player>`

**Permission**: `lenientdeath.command.changeOthers`

**Non-permissions access**: Operators (permission >= 4)

**Description**: Allows a player to change other players' per-player status.

**Implies**: Change Self, Check Others