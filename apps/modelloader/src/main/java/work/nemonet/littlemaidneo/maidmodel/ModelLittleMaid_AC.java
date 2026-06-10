package work.nemonet.littlemaidneo.maidmodel;

public class ModelLittleMaid_AC extends ModelMultiMMMBase {

    public ModelRenderer bipedHead;
    public ModelRenderer bipedBody;
    public ModelRenderer bipedRightArm;
    public ModelRenderer bipedLeftArm;
    public ModelRenderer bipedRightLeg;
    public ModelRenderer bipedLeftLeg;
    public ModelRenderer Skirt;

    public ModelRenderer ChignonR;
    public ModelRenderer ChignonL;
    public ModelRenderer ChignonB;
    public ModelRenderer Tail;
    public ModelRenderer SideTailR;
    public ModelRenderer SideTailL;

    public ModelLittleMaid_AC() { super(); }
    public ModelLittleMaid_AC(float psize) { super(psize); }
    public ModelLittleMaid_AC(float psize, float pyoffset) { super(psize, pyoffset, 64, 32); }

    @Override
    public void initModel(float psize, float pyoffset) {
        // Arms commented out pending full implementation
    }

    @Override
    public float[] getArmorModelsSize() {
        return new float[]{0.1F, 0.5F};
    }

    @Override
    public float getHeight() { return 1.35F; }

    @Override
    public float getWidth() { return 0.5F; }

    @Override
    public float getyOffset() { return 1.215F; }

    @Override
    public float getMountedYOffset() { return 0.35F; }

    @Override
    public void renderItems(IModelCaps pEntityCaps) {}

    @Override
    public void renderFirstPersonHand(IModelCaps pEntityCaps) {}
}
