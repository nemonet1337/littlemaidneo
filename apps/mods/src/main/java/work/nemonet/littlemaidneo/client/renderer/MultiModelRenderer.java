package work.nemonet.littlemaidneo.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import work.nemonet.littlemaidneo.common.LMNLib;
import work.nemonet.littlemaidneo.common.MultiModelHolder;
import work.nemonet.littlemaidneo.entity.compound.IHasMultiModel;
import work.nemonet.littlemaidneo.entity.compound.MultiModelView;

public class MultiModelRenderer<T extends LivingEntity & MultiModelView>
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
        if (entity instanceof MultiModelHolder holder) {
            holder.getMultiModel().updateArmor();
        }
        state.fillFrom(entity, entity, partialTick);
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
    public Identifier getTextureLocation(MultiModelRenderState state) {
        if (state.multiModel == null) return NULL_TEXTURE;
        return state.multiModel.getTexture(IHasMultiModel.Layer.SKIN, IHasMultiModel.Part.HEAD, false)
                .orElse(NULL_TEXTURE);
    }
}
