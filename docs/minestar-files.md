# The `minestar/` folder

Everything this mod keeps for an operator to read or change by hand lives in `<game dir>/minestar/`
as plain JSON. Edit a file, run `/rollmod reload`, and the change is live — no restart.

These replace what FTB Essentials used to hold. That mod is no longer a dependency; the only thing
still read from it is the homes it left in `<world>/ftbessentials/playerdata/*.snbt`, which the
homes tab offers to bring across. Those files are never written to.

| File | What it holds | Written when |
|------|---------------|--------------|
| `kits/<name>.json` | One kit: its cooldown, its permission node and its items | `/kit create`, `/kit delete` |
| `kits/cooldowns.json` | `{ "<player uuid>": { "<kit>": <epoch millis> } }` | A kit is claimed |
| `teleports.json` | Per player: the `/back` position, and when they last used `/rtp` | Any teleport, and on death |
| `mutes.json` | Per player: `until` (0 = indefinite), `by`, `reason` | `/mute`, `/unmute` |
| `operators.json` | Who is an operator, and who may say so | A switch is flipped, `/rollmod op set` |
| `migrated_homes.json` | Which FTB Essentials homes have already been brought across | A home is migrated |

Cooldown **durations** are ticks (20 = one second) everywhere — kit files, `/kit create`, the
LuckPerms meta overrides. Cooldown **stamps** are epoch millis, because a tick count restarts with
the server and would hand everyone a free round of kits after every reboot.

## Kits

```json
{
  "name": "starter",
  "cooldownTicks": 72000,
  "permission": "rollmod.kit.starter",
  "items": [
    { "id": "minecraft:stone_sword", "count": 1 },
    { "id": "minecraft:bread", "count": 16 }
  ]
}
```

`/kit create <name> <cooldownTicks>` writes one of these from the creator's main inventory and
hotbar — armour and the offhand are what they are wearing, not part of the kit. Items use the
vanilla item format, so enchantments and other components round-trip; an item whose id no longer
exists is dropped on load and the rest of the kit still works.

A player sees a kit in the F4 screen, and may claim it, only if they hold its permission node
(`rollmod.kit.<name>` unless the file names another). Without LuckPerms installed, operators get
everything — a single-player world is no place to enforce ranks.

## Operators

```json
{
  "administrators": ["roll_54"],
  "players": [
    { "name": "helper_1", "uuid": "", "op": true, "level": 4 }
  ]
}
```

The file is the authority: it is applied when the server starts and again whenever a listed player
joins, so an `/op` typed by hand lasts only until that player's next login. Leave `uuid` blank and
it is filled in the first time the server sees that name.

Only the names in `administrators` see the switchboard in the F4 screen, and none of them can be
switched off from in game — a permission that can revoke itself is one click away from locking
everybody out. Add or remove an anchor by editing this list.

## LuckPerms keys

See `LUCKPERMS_VALUE_PERMISSIONS.md` for the full table. In short:

| Key | Kind | Effect |
|-----|------|--------|
| `rollmod.kit.<name>` | permission | May claim that kit |
| `rollmod.rtp.bypasscooldown` | permission | The `/rtp` wait does not apply |
| `rollmod.rtp.cooldown` | meta, ticks | Wait between random teleports |

A kit's cooldown is not in that table and has no key of its own: it is the kit file's `cooldownTicks`
for everyone, with no way around it. Give a rank a shorter wait by giving it a separate kit.

`/rtp`'s dimension blacklist and its radius, tries and default cooldown live in the mod config
(`config/roll_mod.json5`, section `rtp`) rather than here, because they describe the world rather
than the people in it.
