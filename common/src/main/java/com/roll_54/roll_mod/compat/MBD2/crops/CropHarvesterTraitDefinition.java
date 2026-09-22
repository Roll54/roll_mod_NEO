package com.roll_54.roll_mod.compat.MBD2.crops;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderTypes;
import com.lowdragmc.lowdraglib2.client.utils.RenderBufferUtils;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.IUIProviderTrait;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import com.lowdragmc.mbd2.common.trait.TraitDefinitionType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.roll_54.roll_mod.registry.ItemRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.appliedenergistics.yoga.YogaPositionType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * MBD2 trait that harvests AgriCraft crops in a box behind the machine and clears their weeds with
 * Herbicide, paying for each operation out of the machine's energy buffer.
 *
 * <p>MBD2 offers no machine class to subclass and {@code MachineEvent.postCustomEvent()} only reaches
 * KubeJS, so {@link com.lowdragmc.mbd2.common.trait.ITrait#serverTick()} is the one supported place
 * for custom Java machine logic. This is that hook; the work itself lives in {@link CropHarvesterTrait}.
 *
 * <p>The trait owns no capability of its own — it drives the machine's <em>other</em> traits, which it
 * looks up by the names configured below. Those names must match the {@code setName(...)} given to the
 * item-slot and energy trait definitions on the machine (see {@code RollMBD2Machines}).
 *
 * <p>Registered explicitly from {@code RollMBD2Plugin}, like {@code MIEnergyTraitDefinition}.
 */
public class CropHarvesterTraitDefinition extends TraitDefinition implements IUIProviderTrait {

    public static final TraitDefinitionType<CropHarvesterTraitDefinition> TYPE =
            new TraitDefinitionType<>("crop_harvester", "trait") {
                @Override
                public CropHarvesterTraitDefinition createDefinition() {
                    return new CropHarvesterTraitDefinition();
                }
            };

    // ── Area ─────────────────────────────────────────────────────────────
    // The scanned box starts at the block directly behind the machine and extends backwards. With the
    // defaults that is the 5x5x1 footprint: never the machine's own column, never anything in front.

    @Configurable(name = "config.definition.trait.crop_harvester.area_width",
            tips = {"config.definition.trait.crop_harvester.area_width.tooltip"})
    @ConfigNumber(range = {1.0, 31.0})
    private int areaWidth = 5;

    @Configurable(name = "config.definition.trait.crop_harvester.area_depth",
            tips = {"config.definition.trait.crop_harvester.area_depth.tooltip"})
    @ConfigNumber(range = {1.0, 31.0})
    private int areaDepth = 5;

    @Configurable(name = "config.definition.trait.crop_harvester.area_height",
            tips = {"config.definition.trait.crop_harvester.area_height.tooltip"})
    @ConfigNumber(range = {1.0, 31.0})
    private int areaHeight = 1;

    // ── Cost ─────────────────────────────────────────────────────────────

    @Configurable(name = "config.definition.trait.crop_harvester.energy_per_harvest")
    @ConfigNumber(range = {0.0, 2.147483647E9})
    private int energyPerHarvest = 500;

    @Configurable(name = "config.definition.trait.crop_harvester.energy_per_weed_removal")
    @ConfigNumber(range = {0.0, 2.147483647E9})
    private int energyPerWeedRemoval = 500;

    @Configurable(name = "config.definition.trait.crop_harvester.tick_interval",
            tips = {"config.definition.trait.crop_harvester.tick_interval.tooltip"})
    @ConfigNumber(range = {1.0, 1200.0})
    private int tickInterval = 1;

    // ── Wiring to the machine's other traits ─────────────────────────────

    @Configurable(name = "config.definition.trait.crop_harvester.herbicide_slot_name",
            tips = {"config.definition.trait.crop_harvester.herbicide_slot_name.tooltip"})
    private String herbicideSlotName = "herbicide";

    @Configurable(name = "config.definition.trait.crop_harvester.output_slot_name",
            tips = {"config.definition.trait.crop_harvester.output_slot_name.tooltip"})
    private String outputSlotName = "output";

    @Configurable(name = "config.definition.trait.crop_harvester.energy_trait_name",
            tips = {"config.definition.trait.crop_harvester.energy_trait_name.tooltip"})
    private String energyTraitName = "energy";

    @Override
    public CropHarvesterTrait createTrait(MBDMachine machine) {
        return new CropHarvesterTrait(machine, this);
    }

    @Override
    public TraitDefinitionType<?> type() {
        return TYPE;
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(ItemRegistry.HERBICIDE.get());
    }

    // ── Settings panel (IUIProviderTrait) ────────────────────────────────

    /** Widget id suffixes. Non-numeric on purpose: the item-slot binder claims {@code ui:<name>_<digits>}. */
    public static final String SETTING_COLLECT_CROPS = "collect_crops";
    public static final String SETTING_APPLY_HERBICIDE = "apply_herbicide";
    public static final String SETTING_VOID_BIOMASS = "void_biomass";
    public static final String SETTING_RENDER_BOX = "render_box";
    /** The collapsible settings window, and the gear that opens it. */
    public static final String SETTINGS_PANEL = "settings_panel";
    public static final String SETTINGS_TOGGLE = "settings_toggle";

    private static final int ROW_HEIGHT = 16;
    private static final int SWITCH_WIDTH = 20;
    private static final int SWITCH_HEIGHT = 10;
    private static final int LABEL_X = 4 + SWITCH_WIDTH + 4;
    /**
     * The width of the roll, not of the name. {@code HOVER_ROLL} walks a long label from this box's
     * right edge to its left, so this is both how much of the name is legible at once and how far it
     * travels; the settings window leaves room for more, but a longer run only means a longer wait to
     * see the end of the name.
     */
    private static final int LABEL_WIDTH = 60;

    public String settingId(String setting) {
        return uiId() + "_" + setting;
    }

    /**
     * Fills {@code container} with one row per setting. Called both by {@code CropManagerUI} for the
     * hand-built layout and by MBD2 when it generates a default layout in the editor, so the two can
     * never drift apart.
     */
    @Override
    public void createTraitUITemplate(UIElement container) {
        addRow(container, 0, SETTING_COLLECT_CROPS, "gui.roll_mod.crop_manager.collect_crops");
        addRow(container, 1, SETTING_APPLY_HERBICIDE, "gui.roll_mod.crop_manager.apply_herbicide");
        addRow(container, 2, SETTING_VOID_BIOMASS, "gui.roll_mod.crop_manager.void_biomass");
        addRow(container, 3, SETTING_RENDER_BOX, "gui.roll_mod.crop_manager.render_box");
    }

    private void addRow(UIElement container, int index, String setting, String langKey) {
        int top = index * ROW_HEIGHT;

        Switch toggle = new Switch();
        toggle.setId(settingId(setting));
        toggle.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(4).top(top + 3).width(SWITCH_WIDTH).height(SWITCH_HEIGHT));
        container.addChild(toggle);

        Label label = new Label();
        label.setText(Component.translatable(langKey));
        label.textStyle(style -> style
                // adaptiveWidth(false) pins the label to LABEL_WIDTH instead of letting it grow to fit
                // the string -- that growth is what pushed the longer names (and most of the Ukrainian
                // ones) outside the panel. HOVER_ROLL scrolls whatever is still too long on hover.
                .adaptiveWidth(false)
                .textWrap(TextWrap.HOVER_ROLL)
                .textAlignVertical(Vertical.CENTER));
        label.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(LABEL_X).top(top + 3).width(LABEL_WIDTH).height(SWITCH_HEIGHT));
        // TextElement starts a roll at the right edge of the label and draws the whole line from
        // there, so without this the part that has not scrolled in yet is painted straight over the
        // machine window and the screen behind it -- overflow defaults to VISIBLE, and only
        // UIElement.drawContents' scissor keeps a roll inside its own box.
        label.setOverflowVisible(false);
        container.addChild(label);
    }

    /**
     * Binds each switch to the live trait. Two-way, so a change made by one player reaches the server
     * and then every other client.
     */
    @Override
    public void initTraitUI(ITrait trait, UI ui) {
        if (!(trait instanceof CropHarvesterTrait harvester)) {
            return;
        }
        bindSetting(ui, SETTING_COLLECT_CROPS, harvester::isCollectCrops, harvester::setCollectCrops);
        bindSetting(ui, SETTING_APPLY_HERBICIDE, harvester::isApplyHerbicide, harvester::setApplyHerbicide);
        bindSetting(ui, SETTING_VOID_BIOMASS, harvester::isVoidBiomass, harvester::setVoidBiomass);
        bindSetting(ui, SETTING_RENDER_BOX, harvester::isRenderBoundingBox, harvester::setRenderBoundingBox);
        bindSettingsToggle(ui);
    }

    /**
     * Makes the gear open and close the settings window.
     *
     * <p>This has to happen here rather than where the widgets are built, because a
     * {@link com.lowdragmc.lowdraglib2.gui.ui.UITemplate} is <em>data</em>: {@code UITemplate.of(root)}
     * serialises the tree to NBT and {@code createUI()} deserialises a brand-new one. Element types,
     * ids, layout and styles survive that round trip; Java listeners do not. A {@code setOnClick}
     * attached while building the template is silently dropped, which looks exactly like a dead
     * button with nothing in the log. Everything behavioural must be re-attached by id on the live UI.
     */
    private void bindSettingsToggle(UI ui) {
        UIElement panel = ui.selectId(settingId(SETTINGS_PANEL), UIElement.class).findFirst().orElse(null);
        if (panel == null) {
            return;
        }
        panel.setDisplay(false); // Always start collapsed, whatever the template happened to serialise.
        ui.selectId(settingId(SETTINGS_TOGGLE), Button.class)
                .forEach(gear -> gear.setOnClick(event -> panel.setDisplay(!panel.isDisplayed())));
    }

    private void bindSetting(UI ui, String setting, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        ui.selectId(settingId(setting), Switch.class).forEach(toggle -> {
            // notify = false: setOn(v) defaults to true, which would fire the change listener on open
            // and bounce a write straight back at the server.
            toggle.setOn(Boolean.TRUE.equals(getter.get()), false);
            toggle.bind(DataBindingBuilder.bool(getter, setter).name(setting).build());
        });
    }

    // ── Working-area preview (magenta box) ───────────────────────────────

    /** Lazily built so the class is never loaded on a dedicated server. */
    @Nullable
    private IRenderer areaRenderer;

    @Override
    public IRenderer getBESRenderer(IMachine machine) {
        if (areaRenderer == null) {
            areaRenderer = new AreaRenderer();
        }
        return areaRenderer;
    }

    /**
     * Draws the machine's working area as a magenta wireframe when its {@code render_box} setting is
     * on.
     *
     * <p>One instance is shared by every machine of this definition, so everything it draws must come
     * from the {@link BlockEntity} handed to {@link #render}, never from the definition.
     */
    private class AreaRenderer implements IRenderer {

        /**
         * Deliberately constant, and <em>not</em> conditional on the toggle.
         *
         * <p>LDLib's {@code BlockEntityRendererDispatcherMixin} makes vanilla's
         * {@code BlockEntityRenderDispatcher.getRenderer(be)} return null when this is false, and
         * vanilla calls that <b>while compiling a chunk section</b> to decide whether a block entity
         * is renderable at all. Gate on mutable state here and the machine gets baked out of the
         * section entirely: switching the option on then does nothing until something forces a chunk
         * rebuild -- i.e. until you place or break a block nearby. The per-frame decision belongs in
         * {@link #render}, which is re-evaluated every frame.
         */
        @Override
        public boolean hasBlockEntityRenderer(BlockEntity blockEntity) {
            return true;
        }

        /**
         * Also sampled at chunk-compile time, which is fine because it is constant. True puts the
         * machine in the level's global block-entity list, so it is drawn without a frustum check --
         * that is what keeps the outline visible while the player stands inside the area with the
         * machine itself behind them.
         */
        @Override
        public boolean shouldRenderOffScreen(BlockEntity blockEntity) {
            return true;
        }

        @Override
        public int getViewDistance() {
            // Bounds the cost of being in the global list: beyond this the box is simply not drawn.
            return 64;
        }

        @Override
        public AABB getRenderBoundingBox(BlockEntity blockEntity) {
            int reach = Math.max(areaWidth, areaDepth) + 1;
            return new AABB(blockEntity.getBlockPos()).inflate(reach, areaHeight, reach);
        }

        @Override
        public void render(BlockEntity blockEntity, float partialTicks, PoseStack poseStack,
                           MultiBufferSource buffer, int light, int overlay) {
            CropHarvesterTrait harvester = harvesterOf(blockEntity);
            if (harvester == null) {
                return;
            }
            Direction front = harvester.getMachine().getFrontFacing().orElse(null);
            if (front == null || front.getAxis().isVertical()) {
                return;
            }

            // Same geometry as CropHarvesterTrait#serverTick, expressed relative to the block origin.
            Direction back = front.getOpposite();
            Direction right = front.getClockWise();
            int halfWidth = areaWidth / 2;
            BlockPos origin = BlockPos.ZERO.relative(back);
            BlockPos near = origin.relative(right, -halfWidth);
            BlockPos far = origin.relative(back, areaDepth - 1).relative(right, halfWidth).above(areaHeight - 1);

            float minX = Math.min(near.getX(), far.getX());
            float minY = Math.min(near.getY(), far.getY());
            float minZ = Math.min(near.getZ(), far.getZ());
            float maxX = Math.max(near.getX(), far.getX()) + 1f;
            float maxY = Math.max(near.getY(), far.getY()) + 1f;
            float maxZ = Math.max(near.getZ(), far.getZ()) + 1f;

            RenderBufferUtils.drawCubeFrame(poseStack, buffer.getBuffer(LDLibRenderTypes.noDepthLines()),
                    minX, minY, minZ, maxX, maxY, maxZ, 1f, 0f, 1f, 1f);
        }

        @Nullable
        private CropHarvesterTrait harvesterOf(BlockEntity blockEntity) {
            CropHarvesterTrait harvester = IMachine.ofMachine(blockEntity)
                    .filter(MBDMachine.class::isInstance)
                    .map(MBDMachine.class::cast)
                    .map(machine -> machine.getTraitByName(CropHarvesterTrait.class, getName()))
                    .orElse(null);
            return harvester != null && harvester.isRenderBoundingBox() ? harvester : null;
        }
    }

    @Override
    public boolean allowMultiple() {
        // Two harvesters on one machine would double-scan the same area and double-charge for it.
        return false;
    }

    public int getAreaWidth() {
        return areaWidth;
    }

    public void setAreaWidth(int areaWidth) {
        this.areaWidth = areaWidth;
    }

    public int getAreaDepth() {
        return areaDepth;
    }

    public void setAreaDepth(int areaDepth) {
        this.areaDepth = areaDepth;
    }

    public int getAreaHeight() {
        return areaHeight;
    }

    public void setAreaHeight(int areaHeight) {
        this.areaHeight = areaHeight;
    }

    public int getEnergyPerHarvest() {
        return energyPerHarvest;
    }

    public void setEnergyPerHarvest(int energyPerHarvest) {
        this.energyPerHarvest = energyPerHarvest;
    }

    public int getEnergyPerWeedRemoval() {
        return energyPerWeedRemoval;
    }

    public void setEnergyPerWeedRemoval(int energyPerWeedRemoval) {
        this.energyPerWeedRemoval = energyPerWeedRemoval;
    }

    public int getTickInterval() {
        return tickInterval;
    }

    public void setTickInterval(int tickInterval) {
        this.tickInterval = tickInterval;
    }

    public String getHerbicideSlotName() {
        return herbicideSlotName;
    }

    public void setHerbicideSlotName(String herbicideSlotName) {
        this.herbicideSlotName = herbicideSlotName;
    }

    public String getOutputSlotName() {
        return outputSlotName;
    }

    public void setOutputSlotName(String outputSlotName) {
        this.outputSlotName = outputSlotName;
    }

    public String getEnergyTraitName() {
        return energyTraitName;
    }

    public void setEnergyTraitName(String energyTraitName) {
        this.energyTraitName = energyTraitName;
    }
}
