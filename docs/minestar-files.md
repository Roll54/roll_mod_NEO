# The `minestar/` folder

Everything this mod keeps for an operator to read or change by hand lives in `<game dir>/minestar/`.
All of it is JSON. (The server rules moderators cite are not here: they are hardcoded in
`RulesStore`, with their wording in the lang files as `rule.roll_mod.<id>`.)
Edit a file, run `/rollmod reload`, and the change is live — no restart.

These replace what FTB Essentials used to hold. That mod is no longer a dependency; the only thing
still read from it is the homes it left in `<world>/ftbessentials/playerdata/*.snbt`, which the
homes tab offers to bring across. Those files are never written to.

| File | What it holds | Written when |
|------|---------------|--------------|
| `kits/<name>.json` | One kit: its cooldown, its permission node and its items | `/kit create`, `/kit delete` |
| `kits/cooldowns.json` | `{ "<player uuid>": { "<kit>": <epoch millis> } }` | A kit is claimed |
| `teleports.json` | Per player: the `/back` position, and when they last used `/rtp` | Any teleport, and on death |
| `mutes.json` | Per player: `until` (0 = indefinite), `by`, `reason` | `/mute`, `/unmute`, the moderation tab |
| `operators.json` | Who is an operator, and who may say so | A switch is flipped, `/rollmod op set` |
| `migrated_homes.json` | Which FTB Essentials homes have already been brought across | A home is migrated |
| `warns.json` | Per player: their name, and every warning ever given | A warning is issued |
| `bans.json` | Per player: `until` (0 = permanent), `by`, `rule`, `reason`, `name` | A ban is issued or lifted |
| `whitelist.json` | Whether the server is closed, and the message it closes with | The toggle is flipped |
| `letters.json` | Every staff letter still in date: text, rewards, who has accepted it | A letter is sent, withdrawn or accepted |
| `moderation.log` | One line per mute, warning, ban, whitelist change, letter and letter command, with who did it | Append-only; never read back |
| `tps.log` | One line each time the server drops below 15 TPS (rate-limited) | Append-only; never read back |

Cooldown **durations** are ticks (20 = one second) everywhere — kit files, `/kit create`, the
LuckPerms meta overrides. Cooldown **stamps** are epoch millis, because a tick count restarts with
the server and would hand everyone a free round of kits after every reboot.

## Bans

Deliberately **not** mirrored into vanilla's `banned-players.json`. Mirroring would get the login
rejection for free, at the cost of two sources of truth that drift apart the moment someone types
`/pardon`. One file, one answer. `until` is epoch millis, `0` means permanent, and a ban that has
run out is dropped the moment anything asks about it — there is no sweeper and none is needed.

## Warnings

`warns.json` never shrinks on its own. Warnings do not expire and are not reset by the ban they
trigger: the third warning, and every warning after it, earns a day's ban. Clearing someone's
history is a deliberate operator act — `/rollmod admin warn clear <player>`, or editing the file.

## Whitelist

Not vanilla's whitelist and not a wrapper over it. Vanilla's is an allow-list of names; this is a
single toggle whose allow-list is a permission — operators and holders of `rollmod.moderation.use`
get in, everybody else is turned away with `kickMessage`. That message carries `&` colour codes and
`\n` line breaks, like every other operator-facing string in this mod.

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

## Letters

A letter has **no recipient list**: it is for every player who has ever joined and everyone who joins
later, from `sendAt` until `expiresAt` (epoch millis; `expiresAt` `0` = never). `sendAt` in the future
makes it a **scheduled** letter: only staff see it until then, and it is announced in chat the moment it
goes out. A missing `sendAt` (files written before scheduling existed) means it went out when written. Each entry in `accepted` is a reader's UUID and
when they took it — a player is offered a letter for as long as they are absent from that map, and a
letter pays each reader once. Expired letters are kept as history — staff can reopen one by saving it
with a new expiry, or send it again as a fresh letter — and pruned on load **30 days after they
expire**, which is what keeps `accepted` from growing forever. Editing a letter keeps `accepted`, so
nobody who already took it is paid twice.

`icon` (optional) is what the letter shows in the list: an item id (`minecraft:diamond`, `create:wrench`)
or a texture path ending in `.png` from any mod or resource pack (`create:textures/item/wrench.png`).
Missing or blank means the default envelope. An unknown item id is dropped when the letter is saved,
and a texture the client does not have shows the envelope.

`rewards` is an array of `{"type":"money","currency":"main","amount":500}`,
`{"type":"item","item":{…}}` (vanilla's item-stack JSON, components included) or
`{"type":"command","command":"give @s diamond 3"}`. A command runs **as the reader at permission level
4** when they accept, so `@s` is always them — and anyone holding `rollmod.moderation.letters` can
therefore run any command as any player. Every run is written to `moderation.log`.

An unreadable reward (a removed mod's item, an unknown currency) is skipped and the rest of the letter
still loads. A letter whose `body` or `title` uses `&` colour codes and `\n` line breaks renders them.

