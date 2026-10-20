package com.roll_54.roll_mod.compat.createcybernetics.mixin;

import com.perigrine3.createcybernetics.entity.custom.RipperEntity;
import com.roll_54.roll_mod.compat.createcybernetics.ConfigurableHostility;
import com.roll_54.roll_mod.registry.ModConfigs;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes the Ripper's hostility a live config switch ({@code createCybernetics.ripperHostile}).
 *
 * <p>The Ripper extends {@code AbstractIllager}, so it is a {@code Monster} and an {@code Enemy}.
 * Its own registration is {@code MobCategory.CREATURE} and it keeps itself persistent, but the class
 * hierarchy still drags in the vanilla monster behaviour. When the switch is off this mixin undoes
 * the parts that live on the entity:
 * <ul>
 *   <li>{@code isPreventingPlayerRest} — a Ripper next to a bed no longer gives "You may not rest
 *       now; there are monsters nearby".</li>
 *   <li>{@code shouldDespawnInPeaceful} — Peaceful difficulty no longer deletes it.</li>
 *   <li>{@code getSoundSource} — its sounds play on the Friendly Creatures slider, not Hostile.</li>
 * </ul>
 * The parts that live on other entities (golems, conduits, foxes) are in
 * {@code mixin.hostility}, keyed off {@link ConfigurableHostility} so they never load this mod.
 *
 * <p>None of these methods is declared by {@code RipperEntity}, so the ones here become its
 * overrides; extending {@code AbstractIllager} is what lets them fall back to {@code super}.
 */
@Mixin(value = RipperEntity.class, remap = false)
public abstract class RipperEntityMixin extends AbstractIllager implements ConfigurableHostility {

    protected RipperEntityMixin(EntityType<? extends AbstractIllager> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean roll_mod$isHostile() {
        return ModConfigs.MAIN.createCybernetics.ripperHostile.get();
    }

    @Override
    public boolean isPreventingPlayerRest(Player player) {
        return roll_mod$isHostile() && super.isPreventingPlayerRest(player);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return roll_mod$isHostile() && super.shouldDespawnInPeaceful();
    }

    @Override
    public SoundSource getSoundSource() {
        return roll_mod$isHostile() ? super.getSoundSource() : SoundSource.NEUTRAL;
    }
}
