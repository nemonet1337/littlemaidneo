package work.nemonet.littlemaidneo.maidmodel;

import net.minecraft.resources.Identifier;

public abstract class ModelStabilizerBase extends ModelBase {

    public ModelStabilizerBase() {}

    public Identifier getTexture() { return null; }

    public boolean checkEquipment(String pName) { return true; }

    public abstract String getName();

    public int getExclusive() { return 0; }

    public boolean isLoadAnotherTexture() { return false; }

}
