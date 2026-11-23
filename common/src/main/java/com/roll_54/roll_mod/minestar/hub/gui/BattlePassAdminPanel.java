package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.roll_54.roll_mod.minestar.battlepass.BattlePassConfig;
import com.roll_54.roll_mod.minestar.battlepass.BattlePassService;
import com.roll_54.roll_mod.minestar.battlepass.ClientBattlePassCache;
import com.roll_54.roll_mod.minestar.hub.QuestGate;
import com.roll_54.roll_mod.network.packet.BattlePassPackets;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * The Moderation tab's battle pass editor: the points settings, the quest that opens each block (picked from the
 * quest book), and every reward — shown a block at a time, free or Prime, each as {@code item*count} or
 * {@code money:amount}, with "Hand" to take the item you are holding. Save sends it all; the server checks the
 * ban permission and writes {@code battlepass.json}.
 */
final class BattlePassAdminPanel {

    private static final String[] NUMBERS = {"pointsPerLevel", "pointsPerPrimeLevel", "pointsDaily", "pointsEasy",
            "pointsHard", "pointsPrimeTask", "easyMultiplier", "hardMultiplier"};

    private final boolean client;
    private final QuestPicker picker;
    private final ScrollerView body = StaffUi.scroller();
    private CompoundTag numbers = new CompoundTag();
    private final List<String> gates = new ArrayList<>();
    /** Per block, the quest the stage table puts there instead ("" where the pass's own setting counts). */
    private final List<String> tableGates = new ArrayList<>();
    private final List<String> free = new ArrayList<>();
    private final List<String> prime = new ArrayList<>();
    private int block;
    private boolean primeTrack;
    private int shownVersion = -1;
    private boolean rebuild;
    private boolean dirty;
    /** The reward being edited in place of the list: which track and level, or null for none. */
    private List<String> editTrack;
    private int editIndex = -1;

    BattlePassAdminPanel(UIElement pane, QuestPicker picker, boolean client) {
        this.client = client;
        this.picker = picker;
        pane.addChild(body);
    }

    void enter() {
        if (!client) return;
        dirty = false;
        PacketDistributor.sendToServer(new BattlePassPackets.Act("admin/get", new CompoundTag()));
    }

    void tick() {
        if (!client) return;
        if (ClientBattlePassCache.ADMIN_VERSION != shownVersion) {
            shownVersion = ClientBattlePassCache.ADMIN_VERSION;
            if (!dirty) load(ClientBattlePassCache.ADMIN);
            rebuild = true;
        }
        if (!rebuild) return;
        rebuild = false;
        build();
    }

    private void load(CompoundTag tag) {
        numbers = tag.getCompound("numbers").copy();
        copy(tag, "gates", gates, BattlePassConfig.BLOCKS);
        copy(tag, "tableGates", tableGates, BattlePassConfig.BLOCKS);
        copy(tag, "free", free, BattlePassConfig.FREE_LEVELS);
        copy(tag, "prime", prime, BattlePassConfig.PRIME_LEVELS);
    }

    private static void copy(CompoundTag tag, String key, List<String> into, int size) {
        into.clear();
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < size; i++) into.add(i < list.size() ? list.getString(i) : "");
    }

    private void build() {
        body.clearAllScrollViewChildren();
        if (editTrack != null) {
            List<String> track = editTrack;
            int index = editIndex;
            UIElement holder = new UIElement();
            holder.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100));
            holder.setOverflowVisible(true);
            body.addScrollViewChild(holder);
            new BattlePassRewardEditor(holder, Component.translatable(track == prime
                    ? "gui.roll_mod.battlepass.level.prime" : "gui.roll_mod.battlepass.level", index + 1),
                    track.get(index), true, spec -> {
                track.set(index, spec);
                dirty = true;
                editTrack = null;
                rebuild = true;
            }, () -> {
                editTrack = null;
                rebuild = true;
            }).show();
            return;
        }
        body.addScrollViewChild(StaffUi.header("gui.roll_mod.battlepass.admin.title"));
        body.addScrollViewChild(StaffUi.dim("gui.roll_mod.battlepass.admin.hint"));

        // Points.
        body.addScrollViewChild(StaffUi.header("gui.roll_mod.battlepass.admin.points"));
        for (String key : NUMBERS) {
            UIElement row = StaffUi.row();
            Label label = StaffUi.text(Component.translatable("gui.roll_mod.battlepass.admin.n." + key));
            label.layout(l -> l.flexBasis(0).flexGrow(1).height(StaffUi.ROW_H));
            label.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
            TextField field = new TextField();
            field.setText(String.valueOf(numbers.getInt(key)));
            field.layout(l -> l.width(70).height(StaffUi.ROW_H));
            field.setTextResponder(t -> {
                try {
                    numbers.putInt(key, Integer.parseInt(t.strip()));
                    dirty = true;
                } catch (NumberFormatException ignored) {
                }
            });
            row.addChildren(label, field);
            body.addScrollViewChild(row);
        }

        // Block quests: read-only, from the quest stage table (Moderation → Settings → Leaderboards).
        body.addScrollViewChild(StaffUi.header("gui.roll_mod.battlepass.admin.gates"));
        body.addScrollViewChild(StaffUi.dim("gui.roll_mod.battlepass.admin.from_table"));
        for (int b = 0; b < BattlePassConfig.BLOCKS; b++) {
            UIElement row = StaffUi.row();
            long quest = b < tableGates.size() ? parse(tableGates.get(b)) : 0;
            Component name = quest == 0 ? Component.translatable("gui.roll_mod.battlepass.admin.no_quest").withStyle(ChatFormatting.GRAY)
                    : QuestGate.title(true, quest).copy().withStyle(ChatFormatting.YELLOW);
            Label label = StaffUi.text(Component.translatable("gui.roll_mod.battlepass.admin.block", b + 1,
                    b * BattlePassConfig.FREE_PER_BLOCK + 1, (b + 1) * BattlePassConfig.FREE_PER_BLOCK).append(": ").append(name));
            label.layout(l -> l.flexBasis(0).flexGrow(1).height(StaffUi.ROW_H));
            label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
            row.addChild(label);
            body.addScrollViewChild(row);
        }

        // Rewards, a block at a time.
        body.addScrollViewChild(StaffUi.header("gui.roll_mod.battlepass.admin.rewards"));
        UIElement pickRow = StaffUi.row();
        for (int b = 0; b < BattlePassConfig.BLOCKS; b++) {
            int index = b;
            Button blockButton = StaffUi.button(Component.literal(String.valueOf(b + 1))
                    .withStyle(b == block ? ChatFormatting.YELLOW : ChatFormatting.WHITE), 20, () -> {
                block = index;
                rebuild = true;
            });
            pickRow.addChild(blockButton);
        }
        pickRow.setOverflowVisible(true);
        body.addScrollViewChild(pickRow);
        // The track toggle on a row of its own: beside the eight block buttons it ran into the last one.
        UIElement trackRow = StaffUi.row();
        trackRow.setOverflowVisible(true);
        trackRow.addChild(StaffUi.button(Component.translatable(primeTrack ? "gui.roll_mod.battlepass.admin.track.prime"
                : "gui.roll_mod.battlepass.admin.track.free"), 140, () -> {
            primeTrack = !primeTrack;
            rebuild = true;
        }));
        body.addScrollViewChild(trackRow);

        List<String> track = primeTrack ? prime : free;
        int per = primeTrack ? BattlePassConfig.PRIME_PER_BLOCK : BattlePassConfig.FREE_PER_BLOCK;
        for (int i = block * per; i < (block + 1) * per && i < track.size(); i++) {
            int index = i;
            UIElement row = StaffUi.row();
            Label level = StaffUi.text(Component.literal(String.valueOf(i + 1))
                    .withStyle(primeTrack ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY));
            level.layout(l -> l.width(26).height(StaffUi.ROW_H).flexShrink(0));
            level.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
            UIElement preview = new UIElement();
            preview.layout(l -> l.width(16).height(16).flexShrink(0));
            preview.style(s -> s.background(BattlePassRewardEditor.icon(
                    com.roll_54.roll_mod.minestar.battlepass.BattlePassReward.options(track.get(index)).stream().findFirst().orElse(""))));
            // The reward itself, as a button: it opens the editor (an item, money, a skin, or a choice of several).
            Button reward = StaffUi.button(com.roll_54.roll_mod.minestar.battlepass.BattlePassReward.describe(track.get(i)), 0, () -> {
                editTrack = track;
                editIndex = index;
                rebuild = true;
            });
            reward.layout(l -> l.flexBasis(0).flexGrow(1));
            StaffUi.leftCaption(reward);
            row.setOverflowVisible(true);
            row.addChildren(level, preview, reward);
            body.addScrollViewChild(row);
        }

        UIElement saveRow = StaffUi.row();
        saveRow.layout(l -> l.marginTop(6).marginBottom(2));
        saveRow.setOverflowVisible(true);
        Button revert = StaffUi.button("gui.roll_mod.staff.commands.revert", 70, () -> {
            dirty = false;
            load(ClientBattlePassCache.ADMIN);
            rebuild = true;
        });
        Button save = StaffUi.button("gui.roll_mod.hub.moderation.save", 70, () -> {
            CompoundTag args = new CompoundTag();
            args.put("numbers", numbers.copy());
            args.put("gates", strings(gates));
            args.put("free", strings(free));
            args.put("prime", strings(prime));
            dirty = false;
            PacketDistributor.sendToServer(new BattlePassPackets.Act("admin/set", args));
        });
        saveRow.addChildren(revert, save);
        body.addScrollViewChild(saveRow);
    }

    private static ListTag strings(List<String> values) {
        ListTag list = new ListTag();
        values.forEach(v -> list.add(StringTag.valueOf(v == null ? "" : v)));
        return list;
    }

    private static long parse(String hex) {
        if (hex == null || hex.isBlank()) return 0;
        try {
            return Long.parseUnsignedLong(hex.strip(), 16);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
