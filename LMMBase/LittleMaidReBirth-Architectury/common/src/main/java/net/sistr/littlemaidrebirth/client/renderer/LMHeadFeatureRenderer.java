/*
 * Decompiled with CFR 0.1.1 (FabricMC 57d88659).
 */
package net.sistr.littlemaidrebirth.client.renderer;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

import java.util.Map;

/**
 * メイドさんの頭飾りレンダラ
 */
@Environment(value = EnvType.CLIENT)
public class LMHeadFeatureRenderer<T extends LittleMaidEntity, M extends EntityModel<T>>
        extends RenderLayer<T, M> {
    private final float scaleX;
    private final float scaleY;
    private final float scaleZ;
    private final Map<SkullBlock.Type, SkullModelBase> headModels;

    public LMHeadFeatureRenderer(RenderLayerParent<T, M> context, EntityModelSet loader) {
        this(context, loader, 1.0f, 1.0f, 1.0f);
    }

    public LMHeadFeatureRenderer(RenderLayerParent<T, M> context, EntityModelSet loader, float scaleX, float scaleY, float scaleZ) {
        super(context);
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
        this.headModels = SkullBlockRenderer.createSkullRenderers(loader);
    }

    @Override
    public void render(PoseStack matrixStack, MultiBufferSource vertexConsumerProvider,
                       int light, T livingEntity,
                       float animationProgress, float g, float h, float j, float k, float l) {
        var lastStack = livingEntity.getInventory().getItem(17);
        var lastItem = lastStack.getItem();
        boolean showLastItem = !lastStack.isEmpty()
                && lastItem instanceof BlockItem
                && ((BlockItem) lastItem).getBlock() instanceof BushBlock;
        ItemStack itemStack = ((LivingEntity) livingEntity).getItemBySlot(EquipmentSlot.HEAD);
        boolean showHeadItem = !itemStack.isEmpty();
        if (!showLastItem && !showHeadItem) {
            return;
        }
        matrixStack.pushPose();
        matrixStack.scale(this.scaleX, this.scaleY, this.scaleZ);
        ((HeadedModel) this.getParentModel()).getHead().translateAndRotate(matrixStack);
        if (showLastItem) {
            matrixStack.pushPose();
            translate(matrixStack, false);
            matrixStack.translate(-0.5, 0.35, -0.5);
            Minecraft.getInstance().getBlockRenderer()
                    .renderSingleBlock(((BlockItem) lastItem).getBlock().defaultBlockState(),
                            matrixStack,
                            vertexConsumerProvider,
                            light,
                            OverlayTexture.NO_OVERLAY);
            matrixStack.popPose();
        }

        if (showHeadItem) {
            Item item = itemStack.getItem();
            if (item instanceof BlockItem && ((BlockItem) item).getBlock() instanceof AbstractSkullBlock) {
                matrixStack.scale(1.1875f, -1.1875f, -1.1875f);
                ResolvableProfile profile = itemStack.get(DataComponents.PROFILE);
                matrixStack.translate(-0.5, 0.0, -0.5);
                SkullBlock.Type skullType = ((AbstractSkullBlock) ((BlockItem) item).getBlock()).getType();
                SkullModelBase skullBlockEntityModel = this.headModels.get(skullType);
                RenderType renderLayer = SkullBlockRenderer.getRenderType(skullType, profile);
                SkullBlockRenderer.renderSkull(null, 180.0f, animationProgress, matrixStack, vertexConsumerProvider, light, skullBlockEntityModel, renderLayer);
            } else if (!(item instanceof ArmorItem) || ((ArmorItem) item).getEquipmentSlot() != EquipmentSlot.HEAD) {
                translate(matrixStack, false);
                Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer()
                        .renderItem(livingEntity, itemStack, ItemDisplayContext.HEAD,
                                false, matrixStack, vertexConsumerProvider, light);
            }
        }
        matrixStack.popPose();
    }

    public static void translate(PoseStack matrices, boolean villager) {
        matrices.translate(0.0, -0.25, 0.0);
        matrices.mulPose(Axis.YP.rotationDegrees(180.0f));
        matrices.scale(0.625f, -0.625f, -0.625f);
        if (villager) {
            matrices.translate(0.0, 0.1875, 0.0);
        }
    }
}

