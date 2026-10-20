package com.roll_54.roll_mod.compat.mi;

import com.roll_54.roll_mod.RollMod;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared logging for the MI fluid pipe guards ({@code FluidPipeDrainGuardMixin} /
 * {@code FluidPipeFillGuardMixin}). A foreign {@code IFluidHandler} that throws from {@code fill}/{@code drain}
 * would otherwise abort the whole level tick on every tick (seen with Sophisticated Backpacks tank upgrades
 * throwing "Cannot encode empty ItemStack"). The mixins swallow the exception and treat the transfer as
 * zero; this class keeps that from flooding the log.
 */
public final class PipeTransferGuard {
    private PipeTransferGuard() {}

    private static final long COOLDOWN_MS = 60_000L;
    private static final ConcurrentHashMap<String, Long> LAST_LOG = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> SUPPRESSED = new ConcurrentHashMap<>();

    public static void report(String operation, Object handler, RuntimeException e) {
        String key = operation + '|' + handler.getClass().getName() + '|' + e.getClass().getName() + '|' + e.getMessage();
        long now = System.currentTimeMillis();
        Long last = LAST_LOG.get(key);
        if (last != null && now - last < COOLDOWN_MS) {
            SUPPRESSED.merge(key, 1, Integer::sum);
            return;
        }
        LAST_LOG.put(key, now);
        Integer suppressed = SUPPRESSED.remove(key);
        if (last == null) {
            RollMod.LOGGER.warn("[MI pipes] {} on {} threw, transfer skipped", operation, handler.getClass().getName(), e);
        } else {
            RollMod.LOGGER.warn("[MI pipes] {} on {} still throwing: {} ({} more suppressed in the last {}s)",
                    operation, handler.getClass().getName(), e, suppressed == null ? 0 : suppressed, COOLDOWN_MS / 1000);
        }
    }
}
