package work.nemonet.littlemaidneo.maidmodel;

import net.minecraft.resources.Identifier;
import work.nemonet.littlemaidneo.maidmodel.compat.GLCompat;

public class ModelStabilizer_WitchHat extends ModelStabilizerBase {

    public static final Identifier ftex = Identifier.fromNamespaceAndPath("littlemaidneo", "mob/littleMaid/ALTERNATIVE/Stabilizer_MagicHat.png");
    public final ModelRenderer WitchHat;
    public final ModelRenderer WitchHat1;
    public final ModelRenderer WitchHat2;
    public final ModelRenderer WitchHat3;

    public ModelStabilizer_WitchHat() {
        textureWidth = 64;
        textureHeight = 32;

        WitchHat = new ModelRenderer(this, 0, 0);
        WitchHat1 = new ModelRenderer(this, 0, 0);
        WitchHat2 = new ModelRenderer(this, 0, 0);
        WitchHat3 = new ModelRenderer(this, 0, 0);
        WitchHat.setTextureOffset( 0, 15).addBox(-8F, 0F, -8F, 16, 1, 16, 0.0F);
        WitchHat.setTextureOffset( 0,  0).addBox(-4.5F, -4F, -4.5F, 9, 4, 9);
        WitchHat1.setTextureOffset(40, 4).addBox(-3F, -3F, -3F, 6, 3, 6).setRotationPoint(0F, -4F, 0F);
        WitchHat2.setTextureOffset(28, 0).addBox(-2F, -2F, -2F, 4, 2, 4).setRotationPoint(0F, -3F, 0F);
        WitchHat3.setTextureOffset( 0, 0).addBox(-1F, -2F, -1F, 2, 2, 2).setRotationPoint(0F, -2F, 0F);

        WitchHat.addChild(WitchHat1);
        WitchHat1.addChild(WitchHat2);
        WitchHat2.addChild(WitchHat3);
    }

    public void render(float f5) {
        GLCompat.glTranslatef(0F, -0.1F, 0F);
        WitchHat.render(f5);
    }

    @Override
    public Identifier getTexture() {
        return ftex;
    }

    @Override
    public String getName() {
        return "WitchHat";
    }

    @Override
    public boolean isLoadAnotherTexture() {
        return true;
    }

    @Override
    public float[] getArmorModelsSize() {
        return null;
    }
}
