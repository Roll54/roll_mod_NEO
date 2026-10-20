package com.roll_54.roll_mod_client.client.radio;

import com.google.common.hash.Hashing;
import earth.terrarium.adastra.client.radio.audio.Mp3AudioStream;
import earth.terrarium.adastra.client.radio.audio.RadioHandler;
import earth.terrarium.adastra.client.config.RadioConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.world.entity.Entity;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;

/**
 * An Ad Astra radio stream played by the radio implant: either straight into the listener's ears
 * ({@link #inHead}) or from a player who broadcasts it, following them as they move ({@link
 * #following}).
 *
 * <p>Ad Astra's {@code RadioSoundInstance} is sealed, and its {@code tick} stops every instance that
 * is not its {@code RadioHandler}'s current station, so this is a sound of its own built from the
 * same parts: {@code RadioHandler.getRadioStream} for the download, {@code Mp3AudioStream} for the
 * decoding, and Ad Astra's radio volume setting. It never goes through {@code RadioHandler.play},
 * which would stop the radios in Ad Astra's blocks and rovers.
 */
final class ImplantRadioSound extends AbstractSoundInstance implements TickableSoundInstance {

    /** How far a broadcast carries, in blocks; it fades out linearly to nothing at this distance. */
    static final int RANGE = 24;

    private final String url;
    /** The broadcaster to follow, or -1 for a station in the listener's own head. */
    private int entityId;
    private boolean stopped;

    private ImplantRadioSound(String url, int entityId) {
        // A stable id per url, as Ad Astra names its own; the location is never looked up.
        super(ResourceLocation.fromNamespaceAndPath("roll_mod",
                "radio_implant/" + Hashing.sha1().hashUnencodedChars(url)), SoundSource.MASTER, RandomSource.create());
        this.url = url;
        this.entityId = entityId;
        this.looping = false;
        if (entityId < 0) {
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.x = 0;
            this.y = 0;
            this.z = 0;
        } else {
            this.relative = false;
            this.attenuation = Attenuation.LINEAR;
        }
    }

    static ImplantRadioSound inHead(String url) {
        return new ImplantRadioSound(url, -1);
    }

    static ImplantRadioSound following(String url, Entity entity) {
        ImplantRadioSound sound = new ImplantRadioSound(url, entity.getId());
        sound.moveTo(entity);
        return sound;
    }

    /** Switches which broadcaster this follows, for when a nearer one takes over the same station. */
    void follow(Entity entity) {
        entityId = entity.getId();
        moveTo(entity);
    }

    void stop() {
        stopped = true;
    }

    @Override
    public WeighedSoundEvents resolve(SoundManager manager) {
        // As Ad Astra's, but with an attenuation distance, so a broadcast fades over RANGE.
        WeighedSoundEvents events = new WeighedSoundEvents(getLocation(), null);
        events.addSound(new Sound(getLocation(), ConstantFloat.of(1f), ConstantFloat.of(1f), 1,
                Sound.Type.FILE, true, false, entityId < 0 ? 16 : RANGE));
        sound = events.getSound(random);
        return events;
    }

    @Override
    public float getVolume() {
        return RadioConfig.volume / 100f;
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary library, Sound sound, boolean looping) {
        return RadioHandler.getRadioStream(url).thenApplyAsync(in -> {
            try {
                return (AudioStream) new Mp3AudioStream(in);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, Util.backgroundExecutor());
    }

    @Override
    public void tick() {
        if (entityId < 0 || stopped) return;
        Minecraft mc = Minecraft.getInstance();
        Entity entity = mc.level == null ? null : mc.level.getEntity(entityId);
        if (entity == null || entity.isRemoved()) {
            stopped = true;
            return;
        }
        moveTo(entity);
    }

    private void moveTo(Entity entity) {
        x = entity.getX();
        y = entity.getEyeY();
        z = entity.getZ();
    }
}
