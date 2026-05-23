package net.sistr.littlemaidrebirth.util;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 視界に関するユーティリティ
 */
public class SightUtil {

    public static List<Entity> getInSightEntities(Level world, Entity entity, Vec3 viewPos, Vec3 lookFor,
            float distance, float yawFov, float pitchFov, float targetExpand,
            Predicate<Entity> predicate) {
        Vec3 lookTo = lookFor.normalize().scale(distance);
        var bb = new AABB(
                viewPos.x(),
                viewPos.y(),
                viewPos.z(),
                viewPos.x() + lookTo.x(),
                viewPos.y() + lookTo.y(),
                viewPos.z() + lookTo.z())
                .inflate(1);
        var sightChecker = getSightChecker(viewPos, lookFor, getYawPitch(lookFor)[0], yawFov, pitchFov);
        return world.getEntities(entity, bb, inBB -> {
            if (sightChecker.check(inBB.getBoundingBox().inflate(targetExpand)) == SightState.HIDE) {
                return false;
            }
            return predicate.test(inBB);
        });
    }

    // rollなしの視錐台を作成し、それとチェックする
    // z軸方向はチェックしない
    public static SightChecker getSightChecker(Vec3 viewPos, Vec3 lookFor, float yaw, float yawFov, float pitchFov) {
        lookFor = lookFor.normalize();
        // x軸をy軸に回して視線方向Pitch軸
        // 視線方向と視線方向Pitch軸の外積から視線方向Yaw軸が得られる
        // 視線方向から視線方向Pitch軸にΘ - 90度回すと上面の法線(内向き)が得られる
        var lookForPitchAxis = rotate(new Vec3(1, 0, 0), new Vec3(0, 1, 0), -yaw);
        var lookForYawAxis = lookFor.cross(lookForPitchAxis);
        var upNorm = rotate(lookFor, lookForPitchAxis, pitchFov - 90f);
        var downNorm = rotate(lookFor, lookForPitchAxis, -pitchFov + 90f);
        var rightNorm = rotate(lookFor, lookForYawAxis, yawFov - 90f);
        var leftNorm = rotate(lookFor, lookForYawAxis, -yawFov + 90f);
        return new SightChecker() {
            @Override
            public boolean check(Vec3 targetPos) {
                Vec3 targetFor = targetPos.subtract(viewPos).normalize();
                // 内積がマイナス=成す角が鈍角だったらダメ
                return 0 < upNorm.dot(targetFor)
                        && 0 < downNorm.dot(targetFor)
                        && 0 < rightNorm.dot(targetFor)
                        && 0 < leftNorm.dot(targetFor);
            }

            @Override
            public SightState check(AABB box) {
                boolean upP = 0 < upNorm.dot(positive(box, upNorm));
                boolean upN = 0 < upNorm.dot(negative(box, upNorm));
                boolean downP = 0 < downNorm.dot(positive(box, downNorm));
                boolean downN = 0 < downNorm.dot(negative(box, downNorm));
                boolean rightP = 0 < rightNorm.dot(positive(box, rightNorm));
                boolean rightN = 0 < rightNorm.dot(negative(box, rightNorm));
                boolean leftP = 0 < leftNorm.dot(positive(box, leftNorm));
                boolean leftN = 0 < leftNorm.dot(negative(box, leftNorm));
                if (upP && upN && downP && downN && rightP && rightN && leftP && leftN) {
                    return SightState.ALL;
                }
                if ((upP || upN)
                        && (downP || downN)
                        && (rightP || rightN)
                        && (leftP || leftN)) {
                    return SightState.PARTIAL;
                }
                return SightState.HIDE;
            }

            private Vec3 positive(AABB box, Vec3 norm) {
                double x = box.minX;
                double y = box.minY;
                double z = box.minZ;
                if (0 < norm.x()) {
                    x = box.maxX;
                }
                if (0 < norm.y()) {
                    y = box.maxY;
                }
                if (0 < norm.z()) {
                    z = box.maxZ;
                }
                return new Vec3(x, y, z);
            }

            private Vec3 negative(AABB box, Vec3 norm) {
                double x = box.minX;
                double y = box.minY;
                double z = box.minZ;
                if (norm.x() < 0) {
                    x = box.maxX;
                }
                if (norm.y() < 0) {
                    y = box.maxY;
                }
                if (norm.z() < 0) {
                    z = box.maxZ;
                }
                return new Vec3(x, y, z);
            }
        };
    }

    public static SightState check(Entity viewer, Entity target, float yawFov, float pitchFov) {
        Vec3 view = viewer.getEyePosition(1F);
        Vec3 lookFor = viewer.getLookAngle();
        return getSightChecker(view, lookFor, getYawPitch(lookFor)[0], yawFov, pitchFov)
                .check(target.getBoundingBox());
    }

    public static boolean isInFrustum(Entity viewer, Entity target, float yawFov, float pitchFov) {
        Vec3 view = viewer.getEyePosition(1F);
        Vec3 lookFor = viewer.getLookAngle();
        return isInFrustum(view, target.getEyePosition(1F), lookFor, yawFov, pitchFov);
    }

    public static boolean isInFrustum(Vec3 viewPos, Vec3 targetPos, Vec3 lookFor, float yawFov, float pitchFov) {
        return getSightChecker(viewPos, lookFor, getYawPitch(lookFor)[0], yawFov, pitchFov)
                .check(targetPos);
    }

    public static Vec3 rotate(Vec3 vec, Vec3 axis, float angle) {
        throw new AssertionError();
        /*
         * var point = new Quaternion((float) vec.x, (float) vec.y, (float) vec.z, 0);
         * var rotate = new Quaternion(new Vec3f(axis), angle, true);
         * var rotateBar = rotate.copy();
         * rotateBar.conjugate();
         * rotate.hamiltonProduct(point);
         * rotate.hamiltonProduct(rotateBar);
         * return new Vec3d(rotate.getX(), rotate.getY(), rotate.getZ());
         */
    }

    // getYawPitch->getVecの場合、こちらのyawとpitchの値をマイナスにすること
    public static Vec3 getVec(float yaw, float pitch) {
        float pitchRad = pitch * ((float) Math.PI / 180F);
        float yawRad = -yaw * ((float) Math.PI / 180F);
        float yawCos = Mth.cos(yawRad);
        float yawSin = Mth.sin(yawRad);
        float pitchCos = Mth.cos(pitchRad);
        float pitchSin = Mth.sin(pitchRad);
        return new Vec3(yawSin * pitchCos, -pitchSin, yawCos * pitchCos);
    }

    public static float[] getYawPitch(Vec3 vec) {
        double lookAtHorizontal = Math.sqrt(vec.x * vec.x + vec.z * vec.z);
        float lookAtYaw = (float) (-Mth.atan2(vec.x, vec.z) * (180D / (float) Math.PI));
        float lookAtPitch = (float) (-Mth.atan2(vec.y, lookAtHorizontal) * (180D / (float) Math.PI));
        return new float[] { lookAtYaw, lookAtPitch };
    }

    /**
     * 透明ブロックを貫通して、点から点が見えるかどうか
     */
    public static boolean canSee(Level world, @Nullable Entity entity, Vec3 view, Vec3 point) {
        Vec3 toEnd = point.subtract(view).normalize();
        for (int i = 0; i < 8; i++) {
            BlockHitResult result = world.clip(
                    new ClipContext(view, point,
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
            if (result.getType() == HitResult.Type.MISS) {
                return true;
            }
            var blockState = world.getBlockState(result.getBlockPos());
            if (blockState.getLightBlock(world, result.getBlockPos()) == 0) {
                view = result.getLocation().add(toEnd);
                continue;
            }
            return false;
        }
        return false;
    }

    public static void faceTo(Entity owner, Vec3 to, float maxYawIncrease, float maxPitchIncrease) {
        double x = to.x() - owner.getX();
        double z = to.z() - owner.getZ();
        double y = to.y() - owner.getEyeY();
        double horizon = Math.sqrt(x * x + z * z);
        float pitch = (float) (-(Mth.atan2(y, horizon) * (180D / Math.PI)));
        float yaw = (float) (Mth.atan2(z, x) * (180D / Math.PI)) - 90.0F;
        owner.setXRot(updateRotation(owner.getXRot(), pitch, maxPitchIncrease));
        owner.setYRot(updateRotation(owner.getYRot(), yaw, maxYawIncrease));
    }

    private static float updateRotation(float angle, float targetAngle, float maxIncrease) {
        float f = Mth.wrapDegrees(targetAngle - angle);
        if (f > maxIncrease) {
            f = maxIncrease;
        }

        if (f < -maxIncrease) {
            f = -maxIncrease;
        }

        return angle + f;
    }

    public static Vec3[] getEight(AABB box) {
        return new Vec3[] {
                new Vec3(box.minX, box.minY, box.minZ),
                new Vec3(box.minX, box.minY, box.maxZ),
                new Vec3(box.minX, box.maxY, box.minZ),
                new Vec3(box.minX, box.maxY, box.maxZ),
                new Vec3(box.maxX, box.minY, box.minZ),
                new Vec3(box.maxX, box.minY, box.maxZ),
                new Vec3(box.maxX, box.maxY, box.minZ),
                new Vec3(box.maxX, box.maxY, box.maxZ)
        };
    }

    public interface SightChecker {
        boolean check(Vec3 targetPos);

        // デフォルト実装では8点全部が外になるくらいデカいBoxはダメ
        default SightState check(AABB box) {
            var eight = getEight(box);
            var bbb = check(eight[0]);
            var bbt = check(eight[1]);
            var btb = check(eight[2]);
            var btt = check(eight[3]);
            var tbb = check(eight[4]);
            var tbt = check(eight[5]);
            var ttb = check(eight[6]);
            var ttt = check(eight[7]);
            if (bbb && bbt && btb && btt && tbb && tbt && ttb && ttt) {
                return SightState.ALL;
            }
            if (bbb || bbt || btb || btt || tbb || tbt || ttb || ttt) {
                return SightState.PARTIAL;
            }
            return SightState.HIDE;
        }
    }

    public enum SightState {
        ALL(true),
        PARTIAL(true),
        HIDE(false);

        private final boolean canSee;

        SightState(boolean canSee) {
            this.canSee = canSee;
        }

        public boolean isCanSee() {
            return canSee;
        }
    }

}
