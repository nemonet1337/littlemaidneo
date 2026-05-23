package work.nemonet.littlemaidneo.maidmodel;

import java.util.Map;

public class EquippedStabilizer {

    public ModelStabilizerBase stabilizer;
    public ModelRenderer equipPoint;
    public String equipPointName;
    public Map<String, Object> localValues;

    public boolean updateEquippedPoint(ModelBase pmodel) {
        for (int li = 0; li < pmodel.boxList.size(); li++) {
            ModelRenderer lmr = pmodel.boxList.get(li);
            if (lmr.boxName != null && lmr.boxName.equalsIgnoreCase(equipPointName)) {
                equipPoint = lmr;
                return true;
            }
        }
        equipPoint = null;
        return false;
    }
}
