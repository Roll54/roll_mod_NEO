package com.roll_54.roll_mod_client.mixin.radio;

import earth.terrarium.adastra.client.radio.audio.RadioStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Stops Ad Astra's radio stream from throwing when it is closed after its download has failed.
 *
 * <p>{@code RadioStream.close()} joins the download future, and {@code join()} rethrows the
 * download's own failure: here, a station that reset the connection mid-song. The close runs
 * inside vanilla's {@code Channel.destroy()}, which only catches {@code IOException}. So the
 * exception escapes {@code Library.releaseChannel} after the pool has already forgotten the
 * channel, but before {@code ChannelAccess} has dropped its handle. Every sound tick after that
 * releases the same handle again ("Tried to release unknown channel"). Then the next
 * {@code SoundEngine.stopAll()}, on Disconnect from the pause menu or on a resource reload, does
 * it on the render thread and crashes the game.
 *
 * <p>By close time the stream is being thrown away, and the download error has already ended
 * playback. Waiting for the download thread to finish is all {@code join()} is there for, so its
 * result can be ignored.
 */
@Mixin(RadioStream.class)
public abstract class RadioStreamCloseMixin {

    @Redirect(method = "close", at = @At(value = "INVOKE",
            target = "Ljava/util/concurrent/CompletableFuture;join()Ljava/lang/Object;"))
    private Object roll_mod$joinQuietly(CompletableFuture<?> getter) {
        try {
            return getter.join();
        } catch (CompletionException | CancellationException ignored) {
            return null;
        }
    }
}
