package com.roll_54.roll_mod.mixin;

import com.roll_54.roll_mod.minestar.CommandBlockLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Logs every command block execution to minecraft_root/minestar/logs/commandBlockUsed.txt
 * Uses CommandBlockLogger utility class for all logging operations.
 *
 * @author roll_54
 */
@Mixin(BaseCommandBlock.class)
public abstract class CommandBlockLoggingMixin {

    @Shadow
    public abstract String getCommand();

    @Shadow
    public abstract Vec3 getPosition();

    @Inject(method = "performCommand", at = @At("HEAD"))
    private void roll_mod$logCommandBlockExecution(Level level, CallbackInfoReturnable<Boolean> cir) {
        if (level.isClientSide) {
            return;
        }

        try {
            BlockPos pos = BlockPos.containing(this.getPosition());

            // Only log for actual CommandBlockEntity instances
            if (!(level.getBlockEntity(pos) instanceof CommandBlockEntity)) {
                return;
            }

            CommandBlockLogger.logBlockCommand(level, pos, this.getCommand());
        } catch (Exception e) {
            System.err.println("[CommandBlockLoggingMixin] Failed to log command block: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


