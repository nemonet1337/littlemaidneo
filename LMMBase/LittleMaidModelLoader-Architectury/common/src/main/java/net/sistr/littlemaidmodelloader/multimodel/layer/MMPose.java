package net.sistr.littlemaidmodelloader.multimodel.layer;

import com.google.common.collect.ImmutableBiMap;
import net.minecraft.world.entity.Pose;

public class MMPose {
    private static final ImmutableBiMap<Pose, MMPose> POSE_MAP;
    public static final MMPose STANDING = new MMPose("Standing");
    public static final MMPose FALL_FLYING = new MMPose("FallFlying");
    public static final MMPose SLEEPING = new MMPose("Sleeping");
    public static final MMPose SWIMMING = new MMPose("Swimming");
    public static final MMPose SPIN_ATTACK = new MMPose("SpinAttack");
    public static final MMPose CROUCHING = new MMPose("Crouching");
    public static final MMPose DYING = new MMPose("Dying");

    private final String name;

    public MMPose(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static MMPose convertPose(Pose pose) {
        return POSE_MAP.getOrDefault(pose, STANDING);
    }

    public static Pose convertPose(MMPose pose) {
        return POSE_MAP.inverse().getOrDefault(pose, Pose.STANDING);
    }

    static {
        ImmutableBiMap.Builder<Pose, MMPose> builder = new ImmutableBiMap.Builder<>();
        builder.put(Pose.STANDING, STANDING);
        builder.put(Pose.FALL_FLYING, FALL_FLYING);
        builder.put(Pose.SLEEPING, SLEEPING);
        builder.put(Pose.SWIMMING, SWIMMING);
        builder.put(Pose.SPIN_ATTACK, SPIN_ATTACK);
        builder.put(Pose.CROUCHING, CROUCHING);
        builder.put(Pose.DYING, DYING);
        POSE_MAP = builder.build();
    }
}
