package work.nemonet.littlemaidneo.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractSkullBlock;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.config.LMNConfig;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;
public class MaidModelRenderer extends MobRenderer<LittleMaidEntity, MaidRenderState, LMMultiModel<MaidRenderState>> {

    private static final Identifier NULL_TEXTURE = Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "null");

    public MaidModelRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LMMultiModel<>(), 0.5F);
        this.addLayer(new LMSkinLayer<>(this));
        this.addLayer(new LMArmorLayer<>(this));
        this.addLayer(new LMHeldItemLayer<>(this));
        this.addLayer(new LMLightLayer<>(this));
        this.addLayer(new LMHeadFeatureRenderer<>(this, ctx.getModelSet(), ctx.getPlayerSkinRenderCache()));
    }

    @Override
    public MaidRenderState createRenderState() {
        return new MaidRenderState();
    }

    @Override
    public void extractRenderState(LittleMaidEntity entity, MaidRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.fillFrom(entity, entity, partialTick);
        state.maidEntity = entity;
        applyHeadCosmetic(entity, state, partialTick);

        float swingProgress = entity.getAttackAnim(partialTick);
        if (entity.swingingArm == net.minecraft.world.InteractionHand.MAIN_HAND) {
            if (entity.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT) {
                state.swingProgressRight = swingProgress;
            } else {
                state.swingProgressLeft = swingProgress;
            }
        } else {
            if (entity.getMainArm() != net.minecraft.world.entity.HumanoidArm.RIGHT) {
                state.swingProgressRight = swingProgress;
            } else {
                state.swingProgressLeft = swingProgress;
            }
        }

        state.interestedAngle = entity.getInterestedAngle(partialTick);
        state.isBegging = entity.isBegging();
        state.isFreedomMode = entity.getMaidMode() == work.nemonet.littlemaidneo.entity.util.MaidMode.FREEDOM;
        state.isTracerMode = entity.getMaidMode() == work.nemonet.littlemaidneo.entity.util.MaidMode.TRACER;
        state.isPlayingSnow = entity.isPlayingSnow();
        state.isWorking = !entity.getActiveJobName().equals("none");
        state.isPlanter = false;
        state.isOverdrive = entity.getAccelerationTicks() > 0;
        state.activeJobName = entity.getActiveJobName();

        state.isWait = TameableUtil.isWait(entity)
                && (LMNConfig.get().client.enableWaitPoseOnMoving
                        || entity.getDeltaMovement().lengthSqr() < 0.01);
        state.isContract = entity.isContract();
        state.isBloodSuck = entity.isBloodSuck();
        state.isHoldingClock = entity.getMainHandItem().getItem() == net.minecraft.world.item.Items.CLOCK
                || entity.getOffhandItem().getItem() == net.minecraft.world.item.Items.CLOCK;
        state.isAimingBow = entity.isAimingBow();
        state.mainArm = entity.getMainArm();
        state.roll = entity.getFallFlyingTicks();
        state.isFallFlying = entity.isFallFlying();
        state.isSwimming = entity.isSwimming();
        state.isBlocking = entity.isBlocking();
        state.isLeashed = entity.isLeashed();

        float swimAmount = Mth.lerp(partialTick, entity.getSwimAmount(0F), entity.getSwimAmount(1F));
        state.leaningPitch = swimAmount;
    }

    /**
     * 頭飾り専用スロットを {@link MaidRenderState#headItem} / {@code wornHeadType} に載せる。
     * ヘルメットは {@link EquipmentSlot#HEAD} のまま防具レイヤが描く。
     */
    private void applyHeadCosmetic(LittleMaidEntity entity, MaidRenderState state, float partialTick) {
        ItemStack cosmetic = entity.getHeadCosmetic();
        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack display = !cosmetic.isEmpty() ? cosmetic
                : (LittleMaidEntity.isHeadArmorItem(head) ? ItemStack.EMPTY : head);

        state.wornHeadType = null;
        state.wornHeadProfile = null;
        if (display.isEmpty()) {
            state.headItem.clear();
            return;
        }
        if (display.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof AbstractSkullBlock skullBlock) {
            state.headItem.clear();
            state.wornHeadType = skullBlock.getType();
            state.wornHeadProfile = display.get(DataComponents.PROFILE);
            state.wornHeadAnimationPos = entity.tickCount + partialTick;
            return;
        }
        this.itemModelResolver.updateForLiving(state.headItem, display, ItemDisplayContext.HEAD, entity);
    }

    @Override
    protected void scale(MaidRenderState state, PoseStack matrices) {
        matrices.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    public Identifier getTextureLocation(MaidRenderState state) {
        if (state.maidEntity == null) return NULL_TEXTURE;
        return state.maidEntity.getTexture(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD, false)
                .orElse(NULL_TEXTURE);
    }
}
