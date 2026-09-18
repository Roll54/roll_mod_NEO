package com.roll_54.roll_mod.minestar.dailytasks.rewards;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyReward;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyReward;
import com.roll_54.roll_mod.economy.api.CurrencyService;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

/**
 * Server currency, paid straight into the player's balance.
 *
 * <p>This used to run {@code eco give} against another mod, because the economy was a separate mod
 * this one could not depend on. Now that it lives here it is a direct {@link CurrencyService} call —
 * no command parsing, no permission-level-4 console source, and a failure that can actually be
 * reported. The deposit is asynchronous, so the balance lands shortly after the claim returns.
 */
@AutoDailyReward
public final class StarcoinsReward implements DailyReward {

    /**
     * The same star the per-task strip uses for starcoins, so the currency looks the same wherever
     * it turns up. A PNG icon needs an item to stand in on the advancement toast, which can only
     * draw a stack.
     */
    private static final DailyTaskIcon ICON =
            DailyTaskIcon.of(RollMod.id("textures/item/lp.png"), Items.GOLD_NUGGET);

    private static final CurrencyType TYPE = CurrencyType.MAIN;
    private static final long AMOUNT = 250;

    @Override
    public String id() {
        return "starcoins";
    }

    @Override
    public DailyTaskIcon icon() {
        return ICON;
    }

    @Override
    public void grant(ServerPlayer player) {
        CurrencyService.deposit(player, TYPE, AMOUNT, player.server);
    }
}
