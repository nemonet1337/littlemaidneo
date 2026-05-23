package work.nemonet.littlemaidneo.entity.targeting;

import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;

public interface TargetTagManager {
    Set<TargetingSystem.TargetTag> getTargetTag(TargetIdentifier id);

    void writeTargetTags(CompoundTag nbt);

    void readTargetTags(CompoundTag nbt);

    Sync getTargetTagsSync();

    interface Sync {
        int hash();
        Map<TargetIdentifier, Set<TargetingSystem.TargetTag>> getData();
        void syncFrom(Sync source);
    }

}
