# ME hatches

Four MI hatches that connect straight to an AE2 network, so an MI multiblock takes its inputs from ME and
sends its outputs to ME with no buses, interfaces or pipes in between. They work in any MI multiblock that
takes item or fluid hatches, including this mod's parallel multiblocks.

| Block | MI hatch type | Slots |
|---|---|---|
| `roll_mod:me_item_input_hatch` | item input | 16 config + 16 stock |
| `roll_mod:me_fluid_input_hatch` | fluid input | 16 config + 16 stock |
| `roll_mod:me_item_output_hatch` | item output | 3 buffer slots (27 hidden receiving slots) |
| `roll_mod:me_fluid_output_hatch` | fluid output | 3 tanks |

Code: `common/src/main/java/com/roll_54/roll_mod/compat/mi/machines/me/`. There are no recipes for them;
the modpack adds those.

## On the network

- Each hatch is an AE2 grid node. It **needs a channel**, like an ME interface, and draws AE2's default
  1 AE/t idle power. Cables connect on every side.
- All movement is powered (`StorageHelper.poweredInsert/poweredExtraction`): it costs AE energy as an
  interface's would, and stops without power.
- The screen shows the state: **online**, **no channel** or **offline**. Nothing moves unless it is online.
- Placed by a player, the node belongs to that player, as AE2's own blocks do.

## Input hatches: an ME interface for the multiblock

The screen is laid out like an ME interface. The top grid (**Keep in stock**) says what to stock. The
bottom grid (**In stock**) shows what is stocked.

- **Click a config slot with an item** to keep that item in stock. The amount is what you hold, up to a
  stack. For the fluid hatch, click with a filled container (bucket, cell, tank…): the amount is what it
  holds, in mB.
- **Right-click** a config slot to clear it.
- **JEI/EMI ghost drags** work. A dragged item starts at 1; a dragged fluid at 1 bucket.
- **Exact amount:** click a set slot with an empty hand, type the amount (items, or mB for fluids) and press
  **Set**. 0 clears the slot. This is how an item slot is set above a stack (up to 1024).

Every 5 ticks, each slot follows the plan of AE2's own `InterfaceLogic`:
1. Stock that is not what the slot is set to (or the slot is unset) goes back to ME.
2. Stock above the set amount: the extra goes back to ME.
3. Stock below it: the difference is taken from ME.

The multiblock consumes from the stock like from any MI input hatch. Each change makes the controller look
for a recipe again at once. Only ME fills the stock; pipes and players cannot put anything in.

**Limits**

- An item slot holds up to **1024** of its item, whatever the item's stack size (`MeHatches.ITEM_INPUT_CAPACITY`).
  That is more than an ME interface or an MI item slot allows: AE2's config runs with overstacking on, and the
  hatch fills the slot itself, past MI's one-stack insert limit. MI's recipe check reads the full amount. A
  machine that needs even more per run gets the same item in several slots.
- A fluid slot holds up to 2,147,483,647 mB.
- There are no crafting or fuzzy cards: a missing input is not crafted, only waited for.

**Breaking:** the stock goes back to ME first. If ME cannot take some, the items drop split into normal
stacks (a slot of 1024 cobblestone drops 16 stacks), and any fluid is lost, as with MI's own fluid hatches.

## Output hatches: straight into ME

Every 20 ticks (1 s) the hatch pushes what it holds into ME. Whatever ME does not take (full or offline)
stays for the next push.

### Item output hatch

- **3 buffer slots**, each holding one item type in **any amount**. The hatch keeps taking output while ME
  is full or offline, so the multiblock keeps running.
- MI only ever puts one stack into an item slot per insert. So behind the 3 buffer slots MI sees **27
  ordinary output slots**, emptied into the buffer every tick: one recipe completion can hand over up to
  27 stacks. Emptying them keeps MI's reservation for running recipes, so an output that was reserved
  always fits.
- A **4th item type** waits in its receiving slot while three other types fill the buffer and have not
  been sent yet. That only happens while ME is full or offline. If it does not clear, those slots fill
  and the multiblock waits.
- The screen shows the buffer with its exact amounts on hover, and says **ME full or offline: holding the
  output** while a push leaves something behind.

**Breaking:** the buffer is pushed into ME first. Whatever ME cannot take is **kept on the dropped hatch
item** (data component `roll_mod:me_hatch_buffer`, listed in its tooltip). Placing that hatch again
restores the buffer, and it carries on sending to ME. Nothing spills as item entities and nothing is lost.
The exception is a break in creative mode, which drops no item.

### Fluid output hatch

- **3 tanks of unlimited size**. MI fills them directly; a 4th fluid waits while three others fill them.
- They keep filling while ME is full or offline, and send everything once ME takes fluid again.
- **Breaking:** the tanks are pushed into ME first. Fluid that is still left is lost, as with MI's own
  fluid hatches.

## Looks

- Casing `roll_mod:me_hatch`: texture `textures/block/me_hatch/casing.png`, with its model at
  `models/machine_casing/me_hatch.json`.
- Overlays: `textures/block/me_hatch/{item,fluid}/{overlay_front,overlay_side,output_*}.png`.
- As with every MI hatch, a hatch shows its own casing on its own and its multiblock's casing once part of
  one. The overlays are always drawn.
- The output overlay marks the output side, which the wrench turns. It is only a mark: ME hatches never
  auto-extract.
- Block models come from `RollMachineModelProvider` (`./gradlew :client:runData`).

## Side effects

- The MBD2 greenhouse accepts every block whose id ends in `_item_output_hatch` / `_fluid_input_hatch`
  (`GreenhouseMachines.hatches`), so `me_item_output_hatch` and `me_fluid_input_hatch` are accepted there.
- They register MI's own item/fluid capability, as MI's hatches do, so other mods see them as MI hatches.
  Input stock cannot be piped in or out. The item output's receiving slots can be pulled from like any MI
  output hatch, but they are emptied every tick.

## Tests

`MeHatchGameTests` (`./gradlew :server:runGameTestServer`, or `/test runall` in a dev world) covers:
- input stocking, refilling and returning;
- fluid stocking;
- returning stock that isn't configured;
- item and fluid output flushing;
- buffering offline, including the 4th-type wait;
- the buffer travelling on the dropped item.
