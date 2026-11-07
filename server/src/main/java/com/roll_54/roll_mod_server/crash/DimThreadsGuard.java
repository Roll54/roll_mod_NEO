package com.roll_54.roll_mod_server.crash;

import me.srrapero720.dimthread.api.DimThreadsApi;

/**
 * The per-level tick guard while DimThreads ticks the dimensions.
 *
 * <p>{@code MinecraftServerLevelTickGuardMixin} wraps the level calls in vanilla's {@code tickChildren} loop. DimThreads
 * hands that loop an empty array and ticks every dimension on its own worker thread instead, so the mixin never ran.
 * DimThreads guards the same three steps itself (level-tick pre event, {@code ServerLevel.tick}, post event) and passes
 * an exception to the handler registered here: it goes to {@link CrashReporter} and the dimension carries on with the
 * next step, exactly as with the vanilla loop. With {@code /gamerule dimthread_active false} vanilla's loop runs again
 * and the mixin takes over.
 */
public final class DimThreadsGuard {
    private DimThreadsGuard() {}

    public static void register() {
        DimThreadsApi.setLevelTickExceptionHandler((level, phase, throwable) ->
                CrashReporter.report(level.getServer(), throwable));
    }
}
