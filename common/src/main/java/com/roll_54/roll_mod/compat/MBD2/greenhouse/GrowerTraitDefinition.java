package com.roll_54.roll_mod.compat.MBD2.greenhouse;

import com.agricraft.agricraft.api.AgriClientApi;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.IUIProviderTrait;
import com.lowdragmc.mbd2.common.trait.TraitDefinition;
import com.lowdragmc.mbd2.common.trait.TraitDefinitionType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.roll_54.roll_mod.economy.util.EnergyFormatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * The EV Greenhouse's controller trait: grows the seed ({@link GrowerTrait}), shows what it is doing
 * in the GUI, and draws the growing plant inside the glass.
 *
 * <p>Registered from {@code RollMBD2Plugin}, like the Crop Manager's trait.
 */
public class GrowerTraitDefinition extends TraitDefinition implements IUIProviderTrait {

    public static final TraitDefinitionType<GrowerTraitDefinition> TYPE =
            new TraitDefinitionType<>("greenhouse_grower", "trait") {
                @Override
                public GrowerTraitDefinition createDefinition() {
                    return new GrowerTraitDefinition();
                }
            };

    /** Widget id suffixes, bound by {@link #initTraitUI}. Non-numeric, as slot ids end in digits. */
    public static final String STATUS = "status";
    public static final String STAGE = "stage";
    public static final String PROGRESS = "progress";
    public static final String ENERGY = "energy";
    public static final String UPGRADES = "upgrades";

    public static final int LINE_HEIGHT = 10;
    public static final int WIDTH = 104;
    /** Lines, then the progress bar under them. */
    public static final int HEIGHT = 4 * (LINE_HEIGHT + 2) + 12;

    @Override
    public GrowerTrait createTrait(MBDMachine machine) {
        return new GrowerTrait(machine, this);
    }

    @Override
    public TraitDefinitionType<?> type() {
        return TYPE;
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(Items.WHEAT_SEEDS);
    }

    @Override
    public boolean allowMultiple() {
        return false;
    }

    public String widgetId(String widget) {
        return uiId() + "_" + widget;
    }

    // ── GUI ─────────────────────────────────────────────────────────────────

    /** Status, stage, EU/t and upgrade lines, then the stage's progress bar. */
    @Override
    public void createTraitUITemplate(UIElement container) {
        String[] lines = { STATUS, STAGE, ENERGY, UPGRADES };
        for (int i = 0; i < lines.length; i++) {
            Label label = new Label();
            label.setId(widgetId(lines[i]));
            label.setText(Component.empty());
            container.addChild(place(label, 0, i * (LINE_HEIGHT + 2), WIDTH, LINE_HEIGHT));
        }
        ProgressBar progress = new ProgressBar();
        progress.setId(widgetId(PROGRESS));
        progress.setProgress(0f);
        progress.label.setText(Component.empty());
        container.addChild(place(progress, 0, lines.length * (LINE_HEIGHT + 2), WIDTH, 10));
    }

    private static <T extends UIElement> T place(T element, int x, int y, int width, int height) {
        element.layout(l -> l.positionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE)
                .left(x).top(y).width(width).height(height));
        return element;
    }

    /** Live values, sent from the server every time they change. */
    @Override
    public void initTraitUI(ITrait trait, UI ui) {
        if (!(trait instanceof GrowerTrait grower)) return;
        bindLabel(ui, STATUS, () -> grower.getStatus().text());
        bindLabel(ui, STAGE, () -> grower.getPlantId().isEmpty()
                ? Component.empty()
                : Component.translatable("gui.roll_mod.ev_greenhouse.stage",
                        grower.getStage() + 1, grower.getStageTotal()));
        bindLabel(ui, ENERGY, () -> Component.translatable("gui.roll_mod.ev_greenhouse.energy",
                EnergyFormatUtils.formatEnergyWithUnit(grower.getEuPerTick()),
                EnergyFormatUtils.formatEnergyWithUnit(grower.maxEuPerTick())));
        bindLabel(ui, UPGRADES, () -> grower.getUpgrades().isEmpty()
                ? Component.translatable("gui.roll_mod.ev_greenhouse.no_upgrades")
                : Component.translatable("gui.roll_mod.ev_greenhouse.upgrades",
                        grower.getUpgrades().getCount(), grower.getUpgrades().getHoverName()));
        ui.selectId(widgetId(PROGRESS), ProgressBar.class).forEach(bar ->
                bar.bind(DataBindingBuilder.floatValS2C(grower::stageProgress).build()));
    }

    private void bindLabel(UI ui, String widget, Supplier<Component> text) {
        ui.selectId(widgetId(widget), Label.class).forEach(label ->
                label.bind(DataBindingBuilder.componentS2C(text).build()));
    }

    // ── The plant inside the glass ───────────────────────────────────────────

    /** Lazily built so the class is never loaded on a dedicated server. */
    @Nullable
    private IRenderer cropRenderer;

    @Override
    public IRenderer getBESRenderer(IMachine machine) {
        if (cropRenderer == null) {
            cropRenderer = new CropRenderer();
        }
        return cropRenderer;
    }

    /**
     * Draws AgriCraft's model of the growing plant at its current stage in the crop cell, one block
     * behind and one above the controller, the way AgriCraft's own crop renderer draws it.
     */
    private class CropRenderer implements IRenderer {

        /** Constant, as the Crop Manager's area renderer explains: it is sampled at chunk compile. */
        @Override
        public boolean hasBlockEntityRenderer(BlockEntity blockEntity) {
            return true;
        }

        @Override
        public AABB getRenderBoundingBox(BlockEntity blockEntity) {
            return new AABB(blockEntity.getBlockPos()).inflate(2);
        }

        @Override
        public void render(BlockEntity blockEntity, float partialTicks, PoseStack poseStack,
                           MultiBufferSource buffer, int light, int overlay) {
            MBDMachine machine = IMachine.ofMachine(blockEntity)
                    .filter(MBDMachine.class::isInstance).map(MBDMachine.class::cast).orElse(null);
            if (!(machine instanceof MBDMultiblockMachine multiblock) || !multiblock.isFormed()) return;
            GrowerTrait grower = machine.getTraitByName(GrowerTrait.class, getName());
            if (grower == null || grower.getPlantId().isEmpty() || blockEntity.getLevel() == null) return;
            Direction front = machine.getFrontFacing().orElse(Direction.NORTH);
            if (front.getAxis().isVertical()) return;

            BlockPos offset = BlockPos.ZERO.relative(front.getOpposite()).above();
            BakedModel model = AgriClientApi.getPlantModel(grower.getPlantId(), grower.getStage());
            int cellLight = LevelRenderer.getLightColor(blockEntity.getLevel(), blockEntity.getBlockPos().offset(offset));
            poseStack.pushPose();
            poseStack.translate(offset.getX(), offset.getY(), offset.getZ());
            Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poseStack.last(),
                    buffer.getBuffer(RenderType.cutoutMipped()), null, model, 1f, 1f, 1f, cellLight, overlay);
            poseStack.popPose();
        }
    }
}
