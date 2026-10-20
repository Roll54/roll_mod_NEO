# Changelog

## 2.13.3 — unreleased

### Hub

- **Themes.** A gear button among the window controls opens a settings popup with 10 UI themes (Modern, Dusk, Carbon, Mint, Plum, Paper, Latte, MC, Ore, GDP; default GDP). The hub, its info window, the welcome screen, vending blocks, the module installation table, the hydroponic garden bed and the modular drill config all follow the theme.
- **"Use the hub theme everywhere"** (experimental, with a warning): also restyles other mods' LdLib screens (e.g. MBD2 machines) and recolours vanilla container screens and their titles while a world is loaded.
- **Window options:** "Allow moving the hub" toggle (resize and maximize still work when off); bookmarks can sit on the top, bottom, left or right edge.
- **Bookmarks** are closed boxes in the theme's colours with centred icons. When the window is too small for all of them they **scroll** (mouse wheel) instead of wrapping into a second row; the open tab is scrolled into view.
- **Resizable dividers** in Moderation, TPA, Letters, Homes, Warps, Friends and Daily Tasks: drag to resize, double-click to reset, remembered per tab.
- **Moderation tab:** the mode-cycling button is now a drop-down (Players / Warps / Letters); Letters only for players allowed to write letters.
- **Info window** is resizable and opens at 400×400; text rewraps as you resize.
- Every tab uses the same 6px padding, so title rows and the "I" button line up; tall header controls are no longer cut off.
- FTB Library sidebar buttons no longer appear over the hub or the welcome screen.
- **Player heads** are sharp (no blur, whole-pixel aligned, no tint bleed) and always show the real skin: heads of offline players and players never seen on this server are fetched by the server through its authlib (Minestar's on the live server).

### Hueri statue

- Right-click opens a page of text about Hueri, in the hub theme. The text is `assets/roll_mod/texts/hueri_statue/<language>.json` (vanilla JSON text: `{"title": …, "body": [paragraphs…]}`, or just the array of paragraphs; `<item:…>` shows the item's icon), falling back to `en_us`.
- Until a player opens a statue for the first time, every statue shows sparkles, a bobbing "!" over its head and a "Right-click to read" hint when looked at.

### Skins (new tab, off by default)

- Hidden until `hub.skinsTab` is turned on in the roll_mod config.
- Your player in the middle wearing every chosen skin; slot tiles around it (head and sword left, pickaxe and axe right) with a 3D picture of the current skin. The doll holds one tool in its main hand (left for left-handed players): the sword at first, then whichever tool skin you changed last, remembered between sessions.
- Clicking a tile opens a picker with every skin for the slot. Owned skins apply at once, "None" takes the skin off. Locked skins carry a lock mark; clicking one shows how to get it.

### Friends (new tab)

- Minestar friends list with online status, last seen and which pack they are playing. Sort/view modes: by pack (this server first), online first, Online-only (everyone here, friends marked — add friends from here), A–Z, Z–A, Team (FTB party first) and Requests.
- Detail panel: face, level and XP to next, group and colour, Prime expiry, playtime and rarity-coloured rewards. Actions: tpa / tpahere, invite to your FTB party, remove friend (click twice to confirm).
- Requests view: accept or decline incoming, cancel outgoing. The bookmark is marked while requests are waiting.
- Works on the dedicated server only; elsewhere the tab says "unavailable".

### Leaderboards (new tab)

- Drop-down of seasonal boards with the top 50 and your own line pinned underneath: **Richest**, **Quests completed** (per FTB team), **Playtime**, **Top earners (7 days)** (auction and vending income, after the auction fee) and **Daily tasks done**.
- **Daily tasks done** counts task rewards each player has claimed (the all-complete bonus does not count). It lives as long as the world and is not reset by the money wipe; it starts empty.

### Chat

- **Channel buttons over the chat box:** All, Global, Local, Friends and **Trade**. A built-in button switches both what the chat shows and where messages go. Global and Local lines are tagged; untagged lines (Discord bridge, advancements, warnings, command feedback) show only under All. The "nobody hears you" notice shows under Local.
- **Friends chat** — `/fc <message>`: to your Minestar friends online here, and yourself. Pink `[F]` tag, LuckPerms prefix/suffix kept.
- **Trade chat** — `/tc <message>`: to everyone on the server. Gold `[T]` tag, yellow text, LuckPerms prefix/suffix kept. Last in the button order and shown by default (also for players with saved chat settings).
- **Custom buttons** (Settings → Chat): a name and a template such as `/msg Steve {msg}`. Picking one sends through it and switches the view to All.
- Buttons can be hidden (at least one always stays) and reordered. Saved per client in `config/roll_mod/chat_channels.json`.
- Fixed: after clicking a channel button, the first Enter could be swallowed and the message needed a second Enter.
- Muted players are refused in every channel.

### Economy

- **Money history** — clock button on the Home tab's title row: the newest 100 changes to your main money with amount, reason, item × count, the other player and how long ago. Filter: All / Sales / Purchases / Vending blocks / Auction house / Rewards. Recorded for auction, vending, playtime rewards, daily tasks, FTB quest rewards and tasks, plots, transfers, warp fees, letter rewards and admin add/take.
- **Auction House seller fee:** sellers receive the price minus the house's cut on fixed-price sales, buyouts and won auctions, with a chat line saying what was kept. Vending blocks never charge it.
- **Vending blocks:** storage doubled from 27 to 54 slots (old saves load with the new slots empty). The owner screen has a settings menu (View trades / Inventory / buy-mode switch) and a title showing the mode. The buy-mode switch is gone from the customer screen.
- Auction House and Plots tabs use the shared hub title row.

### Welcome screen (new)

- Full-screen on first login: three rules (quests, JEI with your actual keybinds, ores), each with a checkbox, plus "I read and agree". Accept unlocks once all four are ticked; Decline, Esc or closing kicks the player and the screen returns on the next login. The inventory key is blocked so a reflexive E does not kick you.

### Daily tasks

- Draggable divider between the task rows and the bonus panel; the title row uses the shared hub header with the reset countdown next to the "I" button; wider reroll caption so «Безкоштовно» fits.

### Items and tools

- New `modpack_logo` item; `minestar_logo` gains a lore line.
- Modular drill: **Ctrl+Shift+scroll** changes the throttle by 100% per notch (Ctrl+scroll is still 5%).
- Fixed: drill drops are handed over in stacks of at most 99 (or the item's max stack size) — larger item entities could not be saved.
- Modular saber: kill drops always go straight into the killer's inventory (at their feet when it is full). The Drop Collector module is removed; installed copies vanish from sabers.

### Admin and server

- **Money audit log** `minestar/logs/currency_logs.txt`: every balance change (deposit, withdraw, set, transfer, wipe, offer hold/claim/expiry) writes a request line and a result line, with who started it (player, console or system job), the other side, the reason and the Java caller. The money API now requires a cause on every write; the old cause-less methods are removed.
- `/rollmod money admin wipe` — deletes every main balance, open main-currency offers and all main money history, zeroes online balances and resets the leaderboards. Restricted to one hard-coded player UUID; not runnable from the console.
- `/rollmod welcome open <targets>` (permission level 2) and `/rollmod welcome reset <targets>`. Accepts and declines are logged.
- `/fc` and `/tc` messages are logged as `[FRIENDS]` / `[TRADE]`; free daily-task rerolls are logged.
- New config:
  - `ahSellerFeeEnabled` (default `true`) and `ahSellerFee` (default `0.10`) — server.
  - `chat_changes.hideAdvancementMessages` (default `false`) — hides advancement announcements.
  - Client files `config/roll_mod/ui_theme.json` and `chat_channels.json`; `hub_window.json` moves to version 2 (bookmark side, movable, per-tab dividers) and still reads version 1.
- Fixed: buffered FTB quest progress no longer throws on server stop if FTB Quests has already unloaded.

### Technical

- DB migration `V3__Currency_ledger.sql`: table `currency_ledger` with indexes on `(uuid, created_at)` and `(created_at)`.
- New packets: history request/sync, leaderboard request/sync, skin request/sync, chat channel, friend action, friends sync, friend detail sync, welcome response.
- New mixins: `PlayerListBroadcastMixin` (common); `theme.GuiGraphicsRecolorMixin` and `theme.AbstractContainerScreenLabelMixin` (client). New core shaders `gui_recolor` and `gui_recolor_color`.
- New saved data: `tasksDone` in the daily-tasks state; player attachment `roll_mod:new_player`.
- Minestar dependency `1.1.0` → `1.5.0` (friends API); `mod_version` 2.12.2 → 2.13.3.
