package work.nemonet.littlemaidneo.maidmodel;

import net.minecraft.resources.Identifier;

public abstract class ModelStabilizerBase extends ModelBase {

    public ModelStabilizerBase() {}

    public Identifier getTexture() { return null; }

    public boolean checkEquipment(String pName) { return true; }

    public abstract String getName();

    public int getExclusive() { return 0; }

    public boolean isLoadAnotherTexture() { return false; }

    public void init(EquippedStabilizer pequipped) {}

    public void render(ModelMultiBase pModel, net.minecraft.world.entity.Entity par1Entity,
                       float par2, float par3, float par4, float par5, float par6, float par7) {}
}
