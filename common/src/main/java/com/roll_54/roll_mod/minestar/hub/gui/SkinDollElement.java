package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.cosmetics.ItemSkinDefinition;
import com.roll_54.roll_mod.cosmetics.SkinCategory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * The skins tab's paper-doll: the viewing player wearing every skin they have chosen.
 *
 * <p>Unlike {@link PlayerPreviewElement} this does not draw the live player. A skin only renders on
 * an item that fits its slot — a helmet skin needs a helmet on the head, a sword skin a sword in hand
 * — and the player may be wearing nothing and holding a pickaxe. So it draws a client-only stand-in
 * (a {@code RemotePlayer} never added to the level) carrying the player's own UUID and skin, and
 * dresses it in plain iron placeholders for the slots that have a skin chosen. The UUID is what makes
 * it work with no special casing: {@code ItemSkinResolver} and {@code HelmetSkinRenderers} look the
 * holder's skins up by UUID in {@code ClientItemSkinCache}, so the stand-in renders exactly what
 * other players see on the real one.
 *
 * <p>Only the main hand holds anything — {@code heldTool}, the tool slot whose skin changed last —
 * and it is the player's own main hand, left for left-handed players, since the doll copies their
 * main arm. The off hand stays empty.
 *
 * <p>{@link Icon} draws the slot tiles and the picker's rows on the same stand-ins: a slot as the
 * player has it, or any one skin, owned or not, on a second stand-in under a fixed fake UUID whose
 * cache entry is set to just that skin before the draw.
 *
 * <p>Like {@link PlayerPreviewElement}, the 3D draws flush render passes of their own and land on top
 * of the UI regardless of {@code zIndex}, so anything that must cover them has to make
 * {@code suppressed} answer true; and the client-only types stay in the {@link ClientRender} holder,
 * because the hub's tree is also built on the dedicated server.
 *
 * <p>The 3D draws clip through LdLib's own scissor, never vanilla's: see {@code ClientRender.drawEntity}.
 */
public class SkinDollElement extends UIElement {

    private final BooleanSupplier suppressed;
    private final Supplier<SkinCategory> heldTool;

    public SkinDollElement(BooleanSupplier suppressed, Supplier<SkinCategory> heldTool) {
        this.suppressed = suppressed;
        this.heldTool = heldTool;
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        super.drawBackgroundAdditional(context);
        if (FMLEnvironment.dist != Dist.CLIENT || suppressed.getAsBoolean() || collapsed(this)) return;
        ClientRender.drawDoll(context, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(),
                heldTool.get());
    }

    /**
     * Whether this sits inside a collapsed tab. The hub hides tabs by shrinking them to height 0
     * rather than hiding them, and a 3D draw sized by its own box would otherwise still show.
     */
    private static boolean collapsed(UIElement element) {
        for (UIElement e = element; e != null; e = e.getParent()) {
            if (e.getSizeHeight() < 1) return true;
        }
        return false;
    }

    /**
     * A slot's or a skin's picture: a 3D head for the helmet slot, the skinned item for the rest.
     * A {@code locked} skin gets the lock mark in its corner.
     */
    public static final class Icon extends UIElement {

        @Nullable private final SkinCategory slot;
        @Nullable private final ItemSkinDefinition skin;
        private final boolean locked;
        private final BooleanSupplier suppressed;

        private Icon(@Nullable SkinCategory slot, @Nullable ItemSkinDefinition skin, boolean locked,
                     BooleanSupplier suppressed) {
            this.slot = slot;
            this.skin = skin;
            this.locked = locked;
            this.suppressed = suppressed;
        }

        /** The viewing player's {@code slot}, with whatever skin they have chosen for it. */
        public static Icon slot(SkinCategory slot, BooleanSupplier suppressed) {
            return new Icon(slot, null, false, suppressed);
        }

        /** {@code skin} on its own, whether the viewer owns it or not. */
        public static Icon skin(ItemSkinDefinition skin, boolean locked, BooleanSupplier suppressed) {
            return new Icon(null, skin, locked, suppressed);
        }

        @Override
        public void drawBackgroundAdditional(GUIContext context) {
            super.drawBackgroundAdditional(context);
            if (FMLEnvironment.dist != Dist.CLIENT || suppressed.getAsBoolean() || collapsed(this)) return;
            ClientRender.drawIcon(context, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight(),
                    slot, skin, locked);
        }
    }

    /** Isolated holder for the client-only rendering. */
    private static final class ClientRender {

        /** The picker's stand-in. Random once per run; it only ever lives in this client's cache. */
        private static final java.util.UUID PREVIEW = java.util.UUID.randomUUID();

        // TODO(art): placeholder lock mark — swap in a real lock texture.
        private static final net.minecraft.resources.ResourceLocation LOCK =
                RollMod.id("textures/item/error_item.png");

        /** How far a slot tile's 3D head draws past its box, right and bottom. */
        private static final float HEAD_GROW = 5;
        /** How far a slot tile's head is raised off the slot's name under it. */
        private static final float HEAD_LIFT = 3;

        @Nullable private static Doll own;
        @Nullable private static Doll preview;

        /** The player as they look to others, in a fixed pose box. */
        static void drawDoll(GUIContext context, float x, float y, float w, float h, SkinCategory heldTool) {
            Doll doll = own();
            if (doll == null) return;
            doll.dress(com.roll_54.roll_mod.cosmetics.client.ClientItemSkinCache.activeFor(doll.getUUID()),
                    heldTool);
            // The player is 1.8 blocks; drawn smaller than the box so it stands back a little, with
            // room above the head for tall hats and around the arms for implants.
            int scale = (int) Math.max(10, Math.min(h / 3.2F, w / 1.6F));
            drawEntity(context, x, y, w, h, scale, 0.0625F, context.mouseX, context.mouseY, doll);
        }

        static void drawIcon(GUIContext context, float x, float y, float w, float h,
                             @Nullable SkinCategory slot, @Nullable ItemSkinDefinition skin, boolean locked) {
            Doll doll;
            SkinCategory category;
            java.util.Map<SkinCategory, ?> skins;
            if (skin != null) {
                doll = preview();
                if (doll == null) return;
                category = skin.category();
                var only = java.util.Map.of(category, skin.skinId());
                com.roll_54.roll_mod.cosmetics.client.ClientItemSkinCache.setPreview(PREVIEW, only);
                skins = only;
            } else {
                doll = own();
                if (doll == null || slot == null) return;
                category = slot;
                skins = com.roll_54.roll_mod.cosmetics.client.ClientItemSkinCache.activeFor(doll.getUUID());
            }

            if (category == SkinCategory.HELMET) {
                // Bare-headed when the slot is empty; the helmet is only there to carry a skin.
                doll.dress(skins, null);
                // A slot tile's head reaches HEAD_GROW past its box to the right and below, so a
                // hat's brim is not cut off there, and sits HEAD_LIFT higher, clear of the slot's
                // name; the picker's small rows keep to their box.
                float grow = skin == null ? HEAD_GROW : 0;
                float lift = skin == null ? HEAD_LIFT : 0;
                drawHead(context, x, y - lift, w + grow, h + grow, doll);
            } else {
                drawTool(context, x, y, w, h, doll, placeholder(category));
            }

            if (locked) {
                var graphics = context.graphics;
                int size = (int) Math.max(6, Math.min(w, h) * 0.45F);
                graphics.pose().pushPose();
                // Above the item, which renders around z 150.
                graphics.pose().translate(0, 0, 400);
                graphics.blit(LOCK, (int) (x + w) - size, (int) (y + h) - size, size, size, 0, 0, 16, 16, 16, 16);
                graphics.pose().popPose();
            }
        }

        /** The doll cropped to its head, turned a little so a hat reads as 3D. */
        private static void drawHead(GUIContext context, float x, float y, float w, float h, Doll doll) {
            float size = Math.min(w, h);
            float cx = x + w / 2F;
            float cy = y + h / 2F;
            // About 0.9 blocks across the box: the head plus a hat's brim.
            int scale = (int) Math.max(4, size / 0.9F);
            // yOffset puts the head's centre (≈1.75 blocks up) at the box's centre instead of the
            // middle of the body; the mouse a little to the left turns the face three-quarters.
            drawEntity(context, x, y, w, h, scale, 0.85F, cx - 15, cy, doll);
        }

        /**
         * Vanilla's {@code InventoryScreen.renderEntityInInventoryFollowsMouse}, clipped through
         * LdLib's scissor instead of {@code GuiGraphics}'.
         *
         * <p>Vanilla's scissor knows nothing of LdLib's: closing it switches scissoring off
         * altogether, dropping whatever clip LdLib had open (a scroller's, the tile's batched
         * background then cut to the head's box), and it measures in the main window's GUI scale —
         * in a picker opened as a desktop window of its own the clip lands elsewhere and nothing
         * of the head is left. {@link GUIContext#enableScissor} nests in LdLib's stack and knows
         * which window it is drawing into.
         */
        private static void drawEntity(GUIContext context, float x, float y, float w, float h, int scale,
                                       float yOffset, float mouseX, float mouseY,
                                       net.minecraft.world.entity.LivingEntity entity) {
            float cx = x + w / 2F;
            float cy = y + h / 2F;
            float turnX = (float) Math.atan((cx - mouseX) / 40.0F);
            float turnY = (float) Math.atan((cy - mouseY) / 40.0F);
            var pose = new org.joml.Quaternionf().rotateZ((float) Math.PI);
            var camera = new org.joml.Quaternionf().rotateX(turnY * 20.0F * (float) (Math.PI / 180.0));
            pose.mul(camera);

            float bodyRot = entity.yBodyRot;
            float yRot = entity.getYRot();
            float xRot = entity.getXRot();
            float headRotO = entity.yHeadRotO;
            float headRot = entity.yHeadRot;
            entity.yBodyRot = 180.0F + turnX * 20.0F;
            entity.setYRot(180.0F + turnX * 40.0F);
            entity.setXRot(-turnY * 20.0F);
            entity.yHeadRot = entity.getYRot();
            entity.yHeadRotO = entity.getYRot();

            float entityScale = entity.getScale();
            var offset = new org.joml.Vector3f(0.0F, entity.getBbHeight() / 2.0F + yOffset * entityScale, 0.0F);
            context.graphics.flush();
            context.enableScissor(x, y, w, h);
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventory(
                    context.graphics, cx, cy, scale / entityScale, offset, pose, camera, entity);
            context.disableScissor();

            entity.yBodyRot = bodyRot;
            entity.setYRot(yRot);
            entity.setXRot(xRot);
            entity.yHeadRotO = headRotO;
            entity.yHeadRot = headRot;
        }

        /**
         * An item drawn as {@code holder} would hold it. Going through {@code renderStatic} with a
         * holder is what lets {@code ItemRendererSkinMixin} swap in the skin; {@code GuiGraphics}'
         * own item draw skips that path on purpose.
         */
        private static void drawTool(GUIContext context, float x, float y, float w, float h,
                                     Doll holder, net.minecraft.world.item.ItemStack stack) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            var graphics = context.graphics;
            var renderer = mc.getItemRenderer();
            float size = Math.min(w, h);
            // Lit like the item underneath: the skins are flat item models, as the iron tools are.
            boolean flat = !renderer.getModel(stack, mc.level, holder, 0).usesBlockLight();

            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(x + w / 2F, y + h / 2F, 150);
            pose.scale(size, -size, size);
            if (flat) com.mojang.blaze3d.platform.Lighting.setupForFlatItems();
            renderer.renderStatic(holder, stack, net.minecraft.world.item.ItemDisplayContext.GUI, false, pose,
                    graphics.bufferSource(), mc.level, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0);
            graphics.flush();
            if (flat) com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            pose.popPose();
        }

        @Nullable
        private static Doll own() {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player == null) return null;
            if (own == null || own.level() != player.level() || !own.getUUID().equals(player.getUUID())) {
                own = new Doll(player.clientLevel, player.getGameProfile());
            }
            own.copyLook(player);
            return own;
        }

        @Nullable
        private static Doll preview() {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player == null) return null;
            if (preview == null || preview.level() != player.level()) {
                preview = new Doll(player.clientLevel,
                        new com.mojang.authlib.GameProfile(PREVIEW, player.getGameProfile().getName()));
            }
            preview.copyLook(player);
            return preview;
        }

        private static net.minecraft.world.item.ItemStack placeholder(SkinCategory category) {
            return new net.minecraft.world.item.ItemStack(switch (category) {
                case HELMET -> net.minecraft.world.item.Items.IRON_HELMET;
                case PICKAXE -> net.minecraft.world.item.Items.IRON_PICKAXE;
                case AXE -> net.minecraft.world.item.Items.IRON_AXE;
                case SWORD -> net.minecraft.world.item.Items.IRON_SWORD;
            });
        }

        /**
         * The stand-in. Wears the viewer's skin texture and model layers whatever its UUID, and is
         * kept well away from the camera so the nametag vanilla draws on other players never shows.
         */
        private static final class Doll extends net.minecraft.client.player.RemotePlayer {

            @Nullable private net.minecraft.client.player.AbstractClientPlayer look;

            Doll(net.minecraft.client.multiplayer.ClientLevel level, com.mojang.authlib.GameProfile profile) {
                super(level, profile);
            }

            void copyLook(net.minecraft.client.player.LocalPlayer player) {
                look = player;
                getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION,
                        player.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION));
                setMainArm(player.getMainArm());
                setPos(player.getX(), player.getY() + 1000, player.getZ());
                copyAttachments(player);
            }

            /**
             * Shares every data attachment the player has — the very same objects, not copies — so
             * render layers keyed on them draw on the doll too. That is how Create Cybernetics'
             * implants (its {@code CYBERWARE} attachment) and anything else attachment-driven show
             * up here. All registered types rather than a list of mods, so a new one needs no edit.
             */
            @SuppressWarnings({"unchecked", "rawtypes"})
            private void copyAttachments(net.minecraft.client.player.LocalPlayer player) {
                for (net.neoforged.neoforge.attachment.AttachmentType type
                        : net.neoforged.neoforge.registries.NeoForgeRegistries.ATTACHMENT_TYPES) {
                    if (player.hasData(type)) {
                        Object value = player.getData(type);
                        if (!hasData(type) || getData(type) != value) setData(type, value);
                    } else if (hasData(type)) {
                        removeData(type);
                    }
                }
            }

            /** A helmet if {@code active} has a head skin, and {@code heldTool} in the main hand if it has that one. */
            void dress(java.util.Map<SkinCategory, ?> active, @Nullable SkinCategory heldTool) {
                put(net.minecraft.world.entity.EquipmentSlot.HEAD,
                        active.containsKey(SkinCategory.HELMET) ? placeholder(SkinCategory.HELMET) : null);
                put(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                        heldTool != null && active.containsKey(heldTool) ? placeholder(heldTool) : null);
                put(net.minecraft.world.entity.EquipmentSlot.OFFHAND, null);
            }

            private void put(net.minecraft.world.entity.EquipmentSlot slot,
                             @Nullable net.minecraft.world.item.ItemStack stack) {
                var next = stack == null ? net.minecraft.world.item.ItemStack.EMPTY : stack;
                if (!net.minecraft.world.item.ItemStack.isSameItem(getItemBySlot(slot), next)) {
                    setItemSlot(slot, next);
                }
            }

            @Override
            public net.minecraft.client.resources.PlayerSkin getSkin() {
                return look != null ? look.getSkin() : super.getSkin();
            }
        }
    }
}
