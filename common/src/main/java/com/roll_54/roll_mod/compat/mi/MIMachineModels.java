package com.roll_54.roll_mod.compat.mi;

import aztech.modern_industrialization.MI;
import aztech.modern_industrialization.datagen.model.MachineModelProperties;
import aztech.modern_industrialization.datagen.model.MachineModelsToGenerate;
import aztech.modern_industrialization.machines.models.MachineCasing;
import aztech.modern_industrialization.machines.models.MachineCasings;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Declares the model a Modern Industrialization machine renders with.
 *
 * <p>MI builds machine models from a JSON document behind its own {@code modern_industrialization:machine}
 * geometry loader: a casing plus a map of overlay textures keyed by face. Registering an entry in
 * {@link MachineModelsToGenerate} makes MI's runtime datagen emit that document during
 * {@code AddPackFindersEvent}, into {@code <gamedir>/modern_industrialization/generated_resources},
 * which MI then serves as a top-priority built-in pack. Both of those are switched on in this
 * pack's {@code modern_industrialization-startup.toml} ({@code datagenOnStartup},
 * {@code loadRuntimeGeneratedResources}).
 *
 * <h2>Why this exists rather than MI's own helper</h2>
 *
 * <p>{@code MachineRegistrationHelper.addMachineModel} — the call KubeJS makes for you, and the only
 * one its {@code registerMachines} event exposes — takes three booleans and derives every texture
 * path through {@code MI.id(...)}. Overlays therefore <em>have</em> to live in
 * {@code assets/modern_industrialization/textures/block/machines/&lt;folder&gt;/}, and only the
 * {@code front}, {@code top} and {@code side} faces can be addressed at all.
 *
 * <p>Going through {@link MachineModelProperties.Builder} directly lifts both limits: any of the
 * {@linkplain #OVERLAY_KEYS 33 overlay keys} MI understands can be set, each pointing at a texture
 * in whatever namespace you like — {@code roll_mod}'s included.
 *
 * <h2>Timing</h2>
 *
 * <p>Register from the mod constructor. MI's runtime datagen reads the map at
 * {@code AddPackFindersEvent}, which is later, and resolving a casing by name needs MI's own
 * constructor to have run first — hence the {@code ordering = "AFTER"} dependency on
 * {@code modern_industrialization} in the mods.toml.
 *
 * <h2>The machine has to be MI's</h2>
 *
 * <p>MI's provider looks the block up as {@code BuiltInRegistries.BLOCK.get(MI.id(internalName))}
 * and emits the blockstate and item model alongside the block model, so an entry only takes effect
 * for a machine registered as {@code modern_industrialization:<internalName>}. That covers every
 * machine MI ships and every machine KubeJS declares, both of which go through MI's own
 * {@code DeferredRegister}. A machine registered into this mod's namespace would need its model
 * written by this mod's own datagen — MI would never see it.
 */
public final class MIMachineModels {

    /**
     * Every overlay key MI's model loader recognises, mirroring the fields of
     * {@code MachineUnbakedModel.OverlaysJson}. Anything else is silently dropped when the model is
     * parsed, so {@link Builder#overlay} rejects it up front instead.
     */
    public static final Set<String> OVERLAY_KEYS = Set.of(
            "top", "top_active", "side", "side_active", "bottom", "bottom_active",
            "front", "front_active", "left", "left_active", "right", "right_active",
            "back", "back_active",
            "top_s", "top_s_active", "top_w", "top_w_active",
            "top_n", "top_n_active", "top_e", "top_e_active",
            "bottom_s", "bottom_s_active", "bottom_w", "bottom_w_active",
            "bottom_n", "bottom_n_active", "bottom_e", "bottom_e_active",
            "output", "item_auto", "fluid_auto");

    /** The faces {@link Builder#face} understands, each of which also has an {@code _active} twin. */
    private static final Set<String> FACES = Set.of(
            "top", "side", "bottom", "front", "left", "right", "back");

    private MIMachineModels() {}

    /**
     * Starts a model for the machine registered under {@code internalName} — the id MI knows it by,
     * which for a KubeJS-declared machine is the second argument of its {@code craftingSingleBlock}
     * or {@code simple*MultiBlock} call.
     *
     * @param casing the casing id; bare names resolve in MI's namespace, exactly as
     *               {@code MachineCasings.get} does, so {@code "solid_titanium_machine_casing"} and
     *               {@code "roll_mod:my_casing"} both work
     */
    public static Builder machine(String internalName, String casing) {
        return machine(internalName, MachineCasings.get(casing));
    }

    public static Builder machine(String internalName, MachineCasing casing) {
        return new Builder(internalName, casing);
    }

    /** Collects overlays for one machine, then hands the finished model to MI. */
    public static final class Builder {

        private final String internalName;
        private final MachineModelProperties.Builder properties;
        /** Kept alongside the MI builder, which exposes no way to read back what it holds. */
        private final Set<String> keys = new LinkedHashSet<>();

        private ResourceLocation folder;
        private boolean autoOverlays = true;

        private Builder(String internalName, MachineCasing casing) {
            this.internalName = internalName;
            this.properties = new MachineModelProperties.Builder(casing);
        }

        /**
         * The texture folder {@link #face} draws from, as a full path in this mod's namespace —
         * {@code "block/machines/srs"} becomes {@code roll_mod:block/machines/srs}. This is the part
         * KubeJS cannot express: its own folder argument is always resolved against MI.
         */
        public Builder folder(String path) {
            return folder(RollMod.id(path));
        }

        public Builder folder(ResourceLocation path) {
            this.folder = path;
            return this;
        }

        /**
         * Adds {@code <face>} and {@code <face>_active} from the {@link #folder}, following MI's
         * {@code overlay_<face>} / {@code overlay_<face>_active} naming.
         */
        public Builder face(String face) {
            if (!FACES.contains(face)) {
                throw new IllegalArgumentException(
                        "Not an overlay face: '%s'. Expected one of %s.".formatted(face, FACES));
            }
            if (folder == null) {
                throw new IllegalStateException(
                        "folder(...) must be set before face('%s') on machine '%s'."
                                .formatted(face, internalName));
            }
            overlay(face, folder.withSuffix("/overlay_" + face));
            overlay(face + "_active", folder.withSuffix("/overlay_" + face + "_active"));
            return this;
        }

        /** {@link #face} for several faces at once. */
        public Builder faces(String... faces) {
            for (String face : faces) {
                face(face);
            }
            return this;
        }

        /** One overlay, pointed wherever you like. */
        public Builder overlay(String key, ResourceLocation texture) {
            if (!OVERLAY_KEYS.contains(key)) {
                throw new IllegalArgumentException(
                        "'%s' is not an overlay key MI understands (machine '%s'). Valid keys: %s."
                                .formatted(key, internalName, OVERLAY_KEYS));
            }
            keys.add(key);
            properties.addOverlay(key, texture);
            return this;
        }

        /** {@link #overlay(String, ResourceLocation)} with a path in this mod's namespace. */
        public Builder overlay(String key, String path) {
            return overlay(key, RollMod.id(path));
        }

        /**
         * Drops the {@code output}, {@code item_auto} and {@code fluid_auto} overlays that MI draws
         * on an auto-extracting face. They are added by default, as MI's own helper does.
         */
        public Builder noAutoOverlays() {
            this.autoOverlays = false;
            return this;
        }

        /** Leaves the output face bare rather than drawing the machine's overlay over it. */
        public Builder noOverlayOnOutputSide() {
            properties.noOverlayOnOutputSide();
            return this;
        }

        /**
         * Hands the model to MI. Registering a name MI already has — which every KubeJS-declared
         * machine does, since its own registration adds one — replaces it, so this is also how you
         * override a model KubeJS generated.
         */
        public void register() {
            if (autoOverlays) {
                // Always MI's, not ours: these three are shared across every machine in the game.
                properties.addOverlay("output", MI.id("block/overlays/output"));
                properties.addOverlay("item_auto", MI.id("block/overlays/item_auto"));
                properties.addOverlay("fluid_auto", MI.id("block/overlays/fluid_auto"));
            }
            if (keys.isEmpty()) {
                throw new IllegalStateException(
                        "Machine model '%s' declares no overlays; it would render as a bare casing."
                                .formatted(internalName));
            }

            boolean replaced = MachineModelsToGenerate.props.containsKey(internalName);
            MachineModelsToGenerate.register(internalName, properties.build());
            RollMod.LOGGER.debug("[MI] {} machine model '{}' with overlays {}.",
                    replaced ? "Replaced" : "Registered", internalName, keys);
        }
    }
}
