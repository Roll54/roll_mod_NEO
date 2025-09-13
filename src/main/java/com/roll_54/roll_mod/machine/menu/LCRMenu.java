package com.roll_54.roll_mod.machine.menu;

import com.roll_54.roll_mod.net.C2SSwitchModePayload;
import com.roll_54.roll_mod.registry.ModMenus;
import com.roll_54.roll_mod.machine.LargeChemicalReactorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.neoforged.neoforge.network.PacketDistributor;

public class LCRMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private int mode = 0; // 0=LCR, 1=UCR

    public LCRMenu(int id, Inventory inv, net.minecraft.world.level.Level level, BlockPos pos) {
        super(ModMenus.LARGE_CHEMICAL_REACTOR_MENU.get(), id);
        this.pos = pos;

        var be = level.getBlockEntity(pos);
        if (be instanceof LargeChemicalReactorBlockEntity lcr) {
            this.mode = lcr.getModeInt();
        }

        addDataSlot(new DataSlot() {
            @Override public int get() { return mode; }
            @Override public void set(int value) { mode = value; }
        });

        // TODO: додай слоти інвентарю/машини за потреби
    }

    @Override
    public boolean stillValid(Player p) {
        return p.distanceToSqr(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5) <= 64.0;
    }

    public int mode() { return mode; }
    public BlockPos pos() { return pos; }

    public void requestCycleMode() {
        PacketDistributor.sendToServer(new C2SSwitchModePayload(pos));
    }
}
