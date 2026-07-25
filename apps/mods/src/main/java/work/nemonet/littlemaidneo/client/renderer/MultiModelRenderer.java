package work.nemonet.littlemaidneo.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.common.LMNLib;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.maidmodel.LMModel;

public class MultiModelRenderer<T extends LivingEntity & IHasMultiModel>
        extends LivingEntityRenderer<T, MultiModelRenderState, LMMultiModel<MultiModelRenderState>> {

    private static final Identifier NULL_TEXTURE = Identifier.fromNamespaceAndPath(LMNLib.MODID, "null");

    public MultiModelRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LMMultiModel<>(), 0.5F);
        this.addLayer(new LMSkinLayer<>(this));
        this.addLayer(new LMArmorLayer<>(this));
        this.addLayer(new LMHeldItemLayer<>(this));
        this.addLayer(new LMLightLayer<>(this));
    }

    @Override
    public MultiModelRenderState createRenderState() {
        return new MultiModelRenderState();
    }

    @Override
    public void extractRenderState(T entity, MultiModelRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        if (entity instanceof work.nemonet.littlemaidneo.common.MultiModelHolder holder) {
            holder.getMultiModel().updateArmor();
        }
        state.multiModel = entity;
        state.entity = entity;
        state.mainArm = entity.getMainArm();
        state.mainHandItem = entity.getMainHandItem();
        state.offHandItem = entity.getOffhandItem();
        state.walkAnimationPos = entity.walkAnimation.position(partialTick);
        state.walkAnimationSpeed = entity.walkAnimation.speed(partialTick);

        state.skinModel = entity.getModel(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD).orElse(null);
        state.skinTexture = entity.getTexture(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD, false).orElse(null);
        state.skinTextureLight = entity.getTexture(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD, true).orElse(null);

        state.armorsVisible.clear();
        state.armorsGlint.clear();
        state.innerModels.clear();
        state.outerModels.clear();
        state.innerTextures.clear();
        state.innerTexturesLight.clear();
        state.outerTextures.clear();
        state.outerTexturesLight.clear();

        for (IHasMultiModel.Part part : IHasMultiModel.Part.values()) {
            state.armorsVisible.setArmor(entity.isArmorVisible(part), part);
            state.armorsGlint.setArmor(entity.isArmorGlint(part), part);

            state.innerModels.setArmor(entity.getModel(IHasMultiModel.Layer.INNER, part).orElse(null), part);
            state.outerModels.setArmor(entity.getModel(IHasMultiModel.Layer.OUTER, part).orElse(null), part);

            state.innerTextures.setArmor(entity.getTexture(IHasMultiModel.Layer.INNER, part, false).orElse(null), part);
            state.innerTexturesLight.setArmor(entity.getTexture(IHasMultiModel.Layer.INNER, part, true).orElse(null), part);

            state.outerTextures.setArmor(entity.getTexture(IHasMultiModel.Layer.OUTER, part, false).orElse(null), part);
            state.outerTexturesLight.setArmor(entity.getTexture(IHasMultiModel.Layer.OUTER, part, true).orElse(null), part);

            state.armorStates[part.getIndex()] = new MultiModelRenderState.ArmorRenderState(
                    state.innerModels.getArmor(part).orElse(null),
                    state.outerModels.getArmor(part).orElse(null),
                    state.innerTextures.getArmor(part).orElse(null),
                    state.innerTexturesLight.getArmor(part).orElse(null),
                    state.outerTextures.getArmor(part).orElse(null),
                    state.outerTexturesLight.getArmor(part).orElse(null),
                    entity.isArmorVisible(part),
                    entity.isArmorGlint(part)
            );
        }
    }

    @Override
    protected boolean shouldShowName(T entity, double distance) {
        return super.shouldShowName(entity, distance)
                && entity.hasCustomName()
                && entity == Minecraft.getInstance().crosshairPickEntity;
    }

    @Override
    protected void scale(MultiModelRenderState state, PoseStack poseStack) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    public void submit(MultiModelRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    public Identifier getTextureLocation(MultiModelRenderState state) {
        if (state.multiModel == null) return NULL_TEXTURE;
        return state.multiModel.getTexture(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD, false)
                .orElse(NULL_TEXTURE);
    }
}
