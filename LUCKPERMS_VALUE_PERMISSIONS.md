# LuckPerms value-bearing permissions (e.g. `home.can_hold: <number>`)

How to define, store, and read permissions that **carry a numeric value** (limits, quotas,
multipliers) for the `roll_mod` server — the kind you wrote as `permission.home.can_hold: some number`.

This project already depends on LuckPerms (`server/build.gradle` → `jars/luckperms-neoforge.jar`)
and uses the LuckPerms API directly (see `server/src/main/java/com/roll_54/roll_mod_server/minestar/PrimeManager.java`).
Everything below uses that same `net.luckperms.api` 5.x API.

---

## 1. The key idea: a permission node is a boolean, not a number

In LuckPerms a permission node is fundamentally **on/off** (`true`/`false`). It cannot *literally*
store `home.can_hold = 5`. To attach a number you use one of two supported idioms:

| Idiom | Looks like | Storage shape | Best for |
|-------|-----------|---------------|----------|
| **Meta** (recommended) | `home.can_hold` → `5` | a key/value pair | arbitrary values: limits, names, multipliers, decimals |
| **Weighted / stacking permission** | `home.can_hold.5` | a boolean node ending in a number | "highest tier wins" integer limits granted per-group |

Your written form `home.can_hold: <number>` is exactly the **meta** shape (`key: value`), so that is
the primary approach documented here. The weighted-permission alternative is in §6.

> Do **not** prefix meta keys with `permission.` — meta and permissions are separate namespaces.
> Use a plain dotted key like `home.can_hold`.

---

## 2. Meta: setting the value (admin side)

Meta is set with `/lp ... meta set <key> <value>`. It can live on a **user** or a **group**
(groups are preferred — assign the value once to a rank, every member inherits it).

```text
# Per group (recommended — every member of "vip" gets 10)
/lp group default meta set home.can_hold 3
/lp group vip     meta set home.can_hold 10
/lp group prime   meta set home.can_hold 25

# Per user (overrides the group value for that player)
/lp user Steve meta set home.can_hold 5

# Inspect / remove
/lp user Steve meta info
/lp group vip  meta unset home.can_hold
```

### Contexts (per-world / per-dimension / per-server)

Append context flags to scope a value. Useful if e.g. the home limit differs in `spawn_dim`:

```text
/lp group vip meta set home.can_hold 0  world=roll_mod:spawn_dim
/lp group vip meta set home.can_hold 10 world=minecraft:overworld
```

### Stacking & priority (when a player has several values)

A player can inherit the same meta key from multiple groups. LuckPerms resolves a **single**
winning value by node weight/priority (highest-weight group wins; user-level beats group-level).
If instead you want them summed or max-ed, configure meta stacking in LuckPerms `config.yml`:

```yaml
# luckperms/config.yml
meta-formatting:
  # not for numbers; that's prefix/suffix
# Numeric stacking is done with meta stacking elements, e.g.:
meta-value-selection-mode: inheritance   # default: closest/highest-priority wins
```

For most "max homes" use cases the default (highest-priority value wins) is what you want — give
each rank a flat number and let inheritance pick the strongest.

---

## 3. Where the value is actually stored (the "configuration file")

LuckPerms keeps data in whatever storage backend `luckperms/config.yml` selects
(`storage-method:` — default `h2`, file backends are `yaml`/`json`/`hocon`). With a **file backend**
the meta you set above is written into the group/user files, e.g.:

`luckperms/yaml-storage/groups/vip.yml`
```yaml
name: vip
permissions:
  - group: default
  # value-bearing entries are stored as meta, NOT permissions:
  - meta:
      home.can_hold: "10"
```

Or with the JSON backend (`luckperms/json-storage/groups/vip.json`):
```json
{
  "name": "vip",
  "permissions": [
    { "key": "meta.home.can_hold.10", "value": true }
  ]
}
```

> Note how LuckPerms internally encodes meta as a `meta.<key>.<value>` node. You normally never
> edit these files by hand — use the `/lp` commands or the web editor (`/lp editor`). Editing the
> raw file requires a `/lp sync` (or restart) afterward.

---

## 4. Reading the value in mod code (the important part)

Use the LuckPerms API exactly like `PrimeManager.java` does. The clean accessor is
`CachedMetaData#getMetaValue`, which can parse the string into a number for you and return a default.

Create a small helper in the server module, e.g.
`server/src/main/java/com/roll_54/roll_mod_server/minestar/PermissionValues.java`:

```java
package com.roll_54.roll_mod_server.minestar;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.server.level.ServerPlayer;

/** Reads value-bearing LuckPerms meta for online players. */
public final class PermissionValues {

    private PermissionValues() {}

    /**
     * Reads an integer meta value (e.g. "home.can_hold") for the player, honouring the player's
     * current contexts (world, server, etc.). Returns {@code fallback} if unset or unparsable.
     */
    public static int getInt(ServerPlayer player, String metaKey, int fallback) {
        LuckPerms lp = LuckPermsProvider.get();
        User user = lp.getUserManager().getUser(player.getUUID());
        if (user == null) return fallback;

        // Resolve contexts for THIS player (so world=spawn_dim overrides apply).
        QueryOptions options = lp.getContextManager()
                .getQueryOptions(player)
                .orElse(user.getQueryOptions());

        CachedMetaData meta = user.getCachedData().getMetaData(options);
        return meta.getMetaValue(metaKey, Integer::parseInt).orElse(fallback);
    }

    /** Convenience for the home limit specifically. */
    public static int maxHomes(ServerPlayer player) {
        return getInt(player, "home.can_hold", 1); // default 1 home if nothing is set
    }
}
```

Usage at a home-creation site:

```java
int limit   = PermissionValues.maxHomes(player);
int current = /* however you count this player's existing homes */;
if (current >= limit) {
    player.sendSystemMessage(Component.literal(
        "You can only have " + limit + " homes."));
    return;
}
// ...create the home...
```

Notes:
- `getMetaValue(key, Integer::parseInt)` returns `Optional<Integer>` — `.orElse(fallback)` gives a safe default.
- For decimals use `Double::parseDouble`; for raw text use `meta.getMetaValue(key)` (returns `String`).
- `getCachedData()` is fully cached/async-safe and fine to call on the server thread per action.
- This mirrors the existing `LuckPermsProvider.get()` usage in `PrimeManager#hasPrime`.

---

## 5. Providing defaults

Set the baseline on the `default` group so *everyone* has a value and the code's `fallback` is only
a last resort:

```text
/lp group default meta set home.can_hold 1
```

Because every player inherits `default`, `PermissionValues.maxHomes` will then return `1` for plain
players and the higher number for ranked players via group inheritance — no code change needed.

---

## 6. Alternative: weighted ("stacking") permission nodes

If you prefer the limit to be a real permission node (e.g. so it shows in `/lp ... permission info`
and is granted like any other perm), use a numeric suffix and take the **highest** the player has:

```text
/lp group default permission set home.can_hold.1  true
/lp group vip     permission set home.can_hold.10 true
/lp group prime   permission set home.can_hold.25 true
```

Read it by scanning the player's resolved permissions for the prefix and parsing the largest suffix:

```java
public static int maxHomesWeighted(ServerPlayer player, int fallback) {
    User user = LuckPermsProvider.get().getUserManager().getUser(player.getUUID());
    if (user == null) return fallback;
    String prefix = "home.can_hold.";
    return user.getCachedData().getPermissionData().getPermissionMap().entrySet().stream()
            .filter(e -> e.getValue())                       // only granted nodes
            .map(java.util.Map.Entry::getKey)
            .filter(k -> k.startsWith(prefix))
            .map(k -> k.substring(prefix.length()))
            .map(s -> { try { return Integer.parseInt(s); } catch (NumberFormatException ex) { return null; } })
            .filter(java.util.Objects::nonNull)
            .max(Integer::compareTo)
            .orElse(fallback);
}
```

**Meta vs weighted — pick one:**
- **Meta** — arbitrary/decimal values, easy override per user, one value resolved by priority. ✅ matches your `key: value` form.
- **Weighted** — integer tiers, naturally "highest rank wins", but you must pre-create every tier you want to grant.

---

## 7. End-to-end checklist for `home.can_hold`

1. Decide approach → **meta** (recommended).
2. Set defaults & tiers:
   ```text
   /lp group default meta set home.can_hold 1
   /lp group vip     meta set home.can_hold 10
   /lp group prime   meta set home.can_hold 25
   ```
3. Add `PermissionValues` helper (§4) to the server module.
4. Call `PermissionValues.maxHomes(player)` wherever the home count is enforced.
5. Test in-game:
   ```text
   /lp user <you> meta set home.can_hold 2
   ```
   then verify the 3rd home is rejected; raise it and verify it's allowed.
   Use `/lp user <you> meta info` to confirm the resolved value and which group/context it came from.

---

## 8. References

- LuckPerms Meta docs: <https://luckperms.net/wiki/Meta-Commands>
- LuckPerms API (meta): <https://luckperms.net/wiki/Developer-API#meta>
- Contexts: <https://luckperms.net/wiki/Context>
- API entry point used here: `net.luckperms.api.LuckPermsProvider` (same as `PrimeManager.java`).

---

## 9. Creating your own commands that take values / read & write meta

Your server registers commands with **Brigadier** (`com.mojang.brigadier`). Each command class has a
`register(CommandDispatcher<CommandSourceStack>)` method, and they are all wired up in
`server/src/main/java/com/roll_54/roll_mod_server/registry/CommandRegistry.java` from the
NeoForge `RegisterCommandsEvent`. See `commands/AutoGiveCommands.java` and `commands/NetherstormCommand.java`
for the existing style this section follows.

### 9.1 Brigadier building blocks

A command is a tree of nodes:

| Piece | Method | Purpose |
|-------|--------|---------|
| Fixed word | `Commands.literal("homes")` | the command name / sub-keywords |
| **Value the user types** | `Commands.argument("amount", IntegerArgumentType.integer(0))` | the number/string "put into" the command |
| Permission gate | `.requires(src -> src.hasPermission(2))` | who may run it (op level 2, like your admin commands) |
| Tab-complete | `.suggests((ctx, b) -> ...)` | suggestions for an argument |
| Run it | `.executes(ctx -> { ...; return 1; })` | the action; return `1` = success, `0` = fail |

**Argument types for "values you put in":**
- `IntegerArgumentType.integer(min, max)` → `IntegerArgumentType.getInteger(ctx, "amount")`
- `DoubleArgumentType.doubleArg(min, max)` → `DoubleArgumentType.getDouble(ctx, "x")`
- `BoolArgumentType.bool()` → `BoolArgumentType.getBool(ctx, "value")`
- `StringArgumentType.word()` / `.string()` / `.greedyString()` → `StringArgumentType.getString(ctx, "name")`
- `EntityArgument.player()` → `EntityArgument.getPlayer(ctx, "target")` (real player arg with built-in tab-complete — better than the `StringArgumentType.word()` + manual suggester used in `AutoGiveCommands`).

### 9.2 Full example: `/homes limit` — get and set the `home.can_hold` meta value

Create `server/src/main/java/com/roll_54/roll_mod_server/commands/HomesLimitCommands.java`:

```java
package com.roll_54.roll_mod_server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod_server.minestar.PermissionValues; // helper from section 4
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.MetaNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class HomesLimitCommands {

    private static final String META_KEY = "home.can_hold";

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(
            Commands.literal("homes")
                // /homes limit            -> shows YOUR resolved limit (any player)
                .then(Commands.literal("limit")
                    .executes(ctx -> {
                        ServerPlayer self = ctx.getSource().getPlayerOrException();
                        int limit = PermissionValues.maxHomes(self);
                        ctx.getSource().sendSuccess(
                            () -> Component.literal("Your home limit: " + limit), false);
                        return 1;
                    })

                    // /homes limit get <player>     (admin)
                    .then(Commands.literal("get")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(ctx -> {
                                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                int limit = PermissionValues.maxHomes(target);
                                ctx.getSource().sendSuccess(
                                    () -> Component.literal(
                                        target.getName().getString() + " home limit: " + limit), false);
                                return 1;
                            })))

                    // /homes limit set <player> <amount>   (admin)
                    .then(Commands.literal("set")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.player())
                            .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    setMeta(target.getUUID(), META_KEY, Integer.toString(amount));
                                    ctx.getSource().sendSuccess(
                                        () -> Component.literal(
                                            "Set " + META_KEY + " for "
                                            + target.getName().getString() + " = " + amount), true);
                                    return 1;
                                }))))));
    }

    /** Offline-safe meta write: clears the old value for the key, then adds the new one. */
    private static void setMeta(UUID uuid, String key, String value) {
        LuckPerms lp = LuckPermsProvider.get();
        lp.getUserManager().modifyUser(uuid, user -> {
            // remove any existing meta nodes with this key so the value doesn't stack
            user.data().clear(NodeType.META.predicate(mn -> mn.getMetaKey().equalsIgnoreCase(key)));
            user.data().add(MetaNode.builder(key, value).build());
        }).whenComplete((v, err) -> {
            if (err != null) RollMod.LOGGER.error("Failed to set meta {}={}", key, value, err);
        });
        // modifyUser saves automatically (same pattern as PrimeManager#setPrimeGroup).
    }
}
```

Register it in `CommandRegistry.onRegisterCommands` next to the others:

```java
HomesLimitCommands.register(dispatcher);
```

### 9.3 What this gives you

```text
/homes limit                 -> "Your home limit: 10"        (reads meta, any player)
/homes limit get Steve       -> "Steve home limit: 3"        (admin)
/homes limit set Steve 5     -> writes meta home.can_hold=5  (admin, persists in LuckPerms)
```

Reading uses the §4 helper (`CachedMetaData#getMetaValue`); writing uses `modifyUser` +
`MetaNode` — the offline-safe, auto-saving pattern already used by `PrimeManager#setPrimeGroup`.
The same skeleton works for **any** value: swap `META_KEY`, the argument type
(`DoubleArgumentType`, `StringArgumentType`, …), and the read helper.

### 9.4 Tips

- **Permissions:** `requires(src -> src.hasPermission(2))` matches your existing admin commands.
  You can also gate on a LuckPerms *permission* node instead — check
  `user.getCachedData().getPermissionData().checkPermission("roll_mod.command.homeslimit").asBoolean()`.
- **Players vs offline:** `EntityArgument.player()` only targets online players. To edit offline
  players, take a name/UUID string and use `lp.getUserManager().modifyUser(uuid, ...)` (it loads,
  edits, and saves even when the player is offline).
- **Meta vs attachment data:** use **LuckPerms meta** for values that should follow ranks / be
  shared with other plugins; use NeoForge **data attachments** (like `AUTO_GIVE` in
  `AutoGiveCommands`) for purely mod-internal per-player state.
- **Return value:** return `1` (or a positive count) on success, `0` on failure; call
  `sendFailure(...)` for errors so the message shows in red.
- **Tab-completion** comes for free with `EntityArgument.player()`; only write a manual `.suggests(...)`
  for custom string arguments.
