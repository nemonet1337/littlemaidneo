package work.nemonet.littlemaidneo.entity.targeting;

import java.util.Map;
import java.util.Set;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public interface TargetTagManager {
    Set<TargetingSystem.TargetTag> getTargetTag(TargetIdentifier id);

    void writeTargetTags(ValueOutput output);

    void readTargetTags(ValueInput input);

    Sync getTargetTagsSync();

    interface Sync {
        int hash();
        Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> getData();
        void syncFrom(Sync source);
    }

}
