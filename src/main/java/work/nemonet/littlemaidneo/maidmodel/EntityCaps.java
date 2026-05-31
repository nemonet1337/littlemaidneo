package work.nemonet.littlemaidneo.maidmodel;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.HumanoidArm;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import work.nemonet.littlemaidneo.multimodel.layer.MMPose;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityCaps implements IModelCaps {
    protected static final Setter EMPTY_SETTER = (entity, arg) -> false;
    private static final Map<String, Integer> caps = new HashMap<>();
    private static final Int2ObjectOpenHashMap<Getter> capGetter = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectOpenHashMap<Setter> capSetter = new Int2ObjectOpenHashMap<>();
    protected LivingEntity owner;

    static {
        register("onGround", caps_onGround, (entity, arg) -> entity.onGround());
        register("isRiding", caps_isRiding, (entity, arg) -> entity.isPassenger());
        register("isChild", caps_isChild, (entity, arg) -> entity.isBaby());

        register("heldItemLeft", caps_heldItemLeft, (entity, arg) -> 0F);
        register("heldItemRight", caps_heldItemRight, (entity, arg) -> 0F);
        register("heldItems", caps_heldItems, (entity, arg) -> new float[]{0.0F, 0.0F});
        register("isSneak", caps_isSneak, (entity, arg) -> entity.isShiftKeyDown());
        register("aimedBow", caps_aimedBow, (entity, arg) -> 0 < entity.getTicksUsingItem());

        register("Entity", caps_Entity, (entity, arg) -> entity);
        register("health", caps_health, (entity, arg) -> (int) entity.getHealth());
        register("ticksExisted", caps_ticksExisted, (entity, arg) -> entity.tickCount);
        register("currentEquippedItem", caps_currentEquippedItem, (entity, arg) -> {
            List<ItemStack> items = new ArrayList<>();
            items.add(entity.getMainHandItem());
            items.add(entity.getOffhandItem());
            items.add(entity.getItemBySlot(EquipmentSlot.FEET));
            items.add(entity.getItemBySlot(EquipmentSlot.LEGS));
            items.add(entity.getItemBySlot(EquipmentSlot.CHEST));
            items.add(entity.getItemBySlot(EquipmentSlot.HEAD));
            ItemStack item = items.get((Integer) arg[0]);
            return item.isEmpty() ? null : item;
        });
        register("currentArmor", caps_currentArmor, (entity, arg) -> {
            List<ItemStack> armors = new ArrayList<>();
            armors.add(entity.getItemBySlot(EquipmentSlot.FEET));
            armors.add(entity.getItemBySlot(EquipmentSlot.LEGS));
            armors.add(entity.getItemBySlot(EquipmentSlot.CHEST));
            armors.add(entity.getItemBySlot(EquipmentSlot.HEAD));
            ItemStack armor = armors.get((Integer) arg[0]);
            return armor.isEmpty() ? null : armor;
        });
        register("healthFloat", caps_healthFloat, (entity, arg) -> entity.getHealth());

        register("currentLeftHandItem", caps_currentLeftHandItem, (entity, arg) ->
                entity.getMainArm() == HumanoidArm.LEFT ? entity.getMainHandItem() : entity.getOffhandItem());
        register("currentRightHandItem", caps_currentRightHandItem, (entity, arg) ->
                entity.getMainArm() == HumanoidArm.RIGHT ? entity.getMainHandItem() : entity.getOffhandItem());

        register("isWet", caps_isWet, (entity, arg) -> entity.isInWaterOrRain());
        register("isDead", caps_isDead, (entity, arg) -> !entity.isAlive());
        register("isInWeb", caps_isInWeb, (entity, arg) -> {
            AABB box = entity.getBoundingBox();
            BlockPos min = BlockPos.containing(box.minX + 1.0E-7, box.minY + 1.0E-7, box.minZ + 1.0E-7);
            BlockPos max = BlockPos.containing(box.maxX - 1.0E-7, box.maxY - 1.0E-7, box.maxZ - 1.0E-7);
            if (entity.level().hasChunkAt(min) && entity.level().hasChunkAt(max)) {
                BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
                for (int i = min.getX(); i <= max.getX(); ++i) {
                    for (int j = min.getY(); j <= max.getY(); ++j) {
                        for (int k = min.getZ(); k <= max.getZ(); ++k) {
                            mutable.set(i, j, k);
                            BlockState blockState = entity.level().getBlockState(mutable);
                            return blockState.getBlock() instanceof WebBlock;
                        }
                    }
                }
            }
            return false;
        });
        register("isSwingInProgress", caps_isSwingInProgress, (entity, arg) -> 0 < entity.attackAnim);
        register("isBurning", caps_isBurning, (entity, arg) -> entity.isOnFire());
        register("isInWater", caps_isInWater, (entity, arg) -> entity.isInWater());
        register("isInvisible", caps_isInvisible, (entity, arg) -> entity.isInvisible());
        register("isSprinting", caps_isSprinting, (entity, arg) -> entity.isSprinting());

        register("getRidingName", caps_getRidingName, (entity, arg) -> entity.getVehicle() == null
                ? "" : EntityType.getKey(entity.getVehicle().getType()).toString());

        register("getRidingType", caps_getRidingType, (entity, arg) -> {
            Entity vehicle = entity.getVehicle();
            if (vehicle == null) return "null";
            else if (vehicle instanceof Player) return "player";
            else if (vehicle instanceof Animal) return "animal";
            else if (vehicle instanceof Mob) return "mob";
            else return "entity";
        });

        register("entityName", caps_entityName, (entity, arg) -> entity.getName().getString());

        register("posX", caps_posX, (entity, arg) -> entity.getX());
        register("posY", caps_posY, (entity, arg) -> entity.getY());
        register("posZ", caps_posZ, (entity, arg) -> entity.getZ());
        register("pos", caps_pos, (entity, arg) -> {
            if (arg == null) return new Double[]{entity.getX(), entity.getY(), entity.getZ()};
            return (Integer) arg[0] == 0 ? entity.getX() : (Integer) arg[0] == 1 ? entity.getY() : entity.getZ();
        });
        register("motionX", caps_motionX, (entity, arg) -> entity.getDeltaMovement().x);
        register("motionY", caps_motionY, (entity, arg) -> entity.getDeltaMovement().y);
        register("motionZ", caps_motionZ, (entity, arg) -> entity.getDeltaMovement().z);
        register("motion", caps_motion, (entity, arg) -> {
            Vec3 vec = entity.getDeltaMovement();
            if (arg == null) return new Double[]{vec.x, vec.y, vec.z};
            return (Integer) arg[0] == 0 ? vec.x : (Integer) arg[0] == 1 ? vec.y : vec.z;
        });
        register("boundingBox", caps_boundingBox, (entity, arg) -> {
            if (arg == null) return entity.getBoundingBox();
            return switch ((Integer) arg[0]) {
                case 0 -> entity.getBoundingBox().maxX;
                case 1 -> entity.getBoundingBox().maxY;
                case 2 -> entity.getBoundingBox().maxZ;
                case 3 -> entity.getBoundingBox().minX;
                case 4 -> entity.getBoundingBox().minY;
                case 5 -> entity.getBoundingBox().minZ;
                default -> null;
            };
        });
        register("rotationYaw", caps_rotationYaw, (entity, arg) -> entity.getYRot());
        register("rotationPitch", caps_rotationPitch, (entity, arg) -> entity.getXRot());
        register("prevRotationYaw", caps_prevRotationYaw, (entity, arg) -> entity.yRotO);
        register("prevRotationPitch", caps_prevRotationPitch, (entity, arg) -> entity.xRotO);
        register("renderYawOffset", caps_renderYawOffset, (entity, arg) -> entity.yBodyRot);
        register("renderRidingYOffset", caps_renderRidingYOffset, (entity, arg) -> getRidingYOffset(entity));

        register("PosBlockID", caps_PosBlockID, (entity, arg) ->
                entity.level().getBlockState(new BlockPos(
                        Mth.floor(entity.getX() + (Double) arg[0]),
                        Mth.floor(entity.getY() + (Double) arg[1]),
                        Mth.floor(entity.getZ() + (Double) arg[2]))).getBlock());
        register("PosBlockState", caps_PosBlockState, (entity, arg) ->
                entity.level().getBlockState(new BlockPos(
                        Mth.floor(entity.getX() + (Double) arg[0]),
                        Mth.floor(entity.getY() + (Double) arg[1]),
                        Mth.floor(entity.getZ() + (Double) arg[2]))));
        register("PosBlockAir", caps_PosBlockAir, (entity, arg) -> {
            BlockPos pos = new BlockPos(
                    Mth.floor(entity.getX() + (Double) arg[0]),
                    Mth.floor(entity.getY() + (Double) arg[1]),
                    Mth.floor(entity.getZ() + (Double) arg[2]));
            return entity.level().getBlockState(pos).getCollisionShape(entity.level(), pos).isEmpty();
        });
        register("PosBlockLight", caps_PosBlockLight, (entity, arg) ->
                entity.level().getMaxLocalRawBrightness(new BlockPos(
                        Mth.floor(entity.getX() + (Double) arg[0]),
                        Mth.floor(entity.getY() + (Double) arg[1]),
                        Mth.floor(entity.getZ() + (Double) arg[2]))));
        register("PosBlockPower", caps_PosBlockPower, (entity, arg) ->
                entity.level().getBlockState(new BlockPos(
                        Mth.floor(entity.getX() + (Double) arg[0]),
                        Mth.floor(entity.getY() + (Double) arg[1]),
                        Mth.floor(entity.getZ() + (Double) arg[2]))).isAir());

        register("isRidingPlayer", caps_isRidingPlayer, (entity, arg) -> entity.getVehicle() instanceof Player);

        register("WorldTotalTime", caps_WorldTotalTime, (entity, arg) -> entity.level().getGameTime());
        register("WorldTime", caps_WorldTime, (entity, arg) -> entity.level().getGameTime() % 24000L);
        register("MoonPhase", caps_MoonPhase, (entity, arg) -> (int)(entity.level().getGameTime() / 24000L) % 8);

        register("height", caps_height, (entity, arg) -> entity.getBbHeight());
        register("width", caps_width, (entity, arg) -> entity.getBbWidth());
        register("YOffset", caps_YOffset, (entity, arg) -> entity.getBbHeight() * 0.5);
        register("mountedYOffset", caps_mountedYOffset, (entity, arg) -> getRidingYOffset(entity));
        register("dominantArm", caps_dominantArm, (entity, arg) -> entity.getMainArm() == HumanoidArm.LEFT ? 0 : 1);

        register("isSwimming", caps_isSwimming, (entity, arg) -> entity.isSwimming());
        register("roll", caps_roll, (entity, arg) -> entity.getFallFlyingTicks());
        register("leaningPitch", caps_leaningPitch, (entity, arg) -> entity.getSwimAmount(1F));
        register("lastLeaningPitch", caps_lastLeaningPitch, (entity, arg) -> entity.getSwimAmount(0F));
        register("isUsingRiptide", caps_isUsingRiptide, (entity, arg) -> entity.isAutoSpinAttack());
        register("isFallFlying", caps_isFallFlying, (entity, arg) -> entity.isFallFlying());
        register("pose", caps_pose, (entity, arg) -> MMPose.convertPose(entity.getPose()));
        register("isPoseStanding", caps_isPoseStanding, (entity, arg) -> entity.getPose() == Pose.STANDING);
        register("isPoseFallFlying", caps_isPoseFallFlying, (entity, arg) -> entity.getPose() == Pose.FALL_FLYING);
        register("isPoseSleeping", caps_isPoseSleeping, (entity, arg) -> entity.getPose() == Pose.SLEEPING);
        register("isPoseSwimming", caps_isPoseSwimming, (entity, arg) -> entity.getPose() == Pose.SWIMMING);
        register("isPoseSpinAttack", caps_isPoseSpinAttack, (entity, arg) -> entity.getPose() == Pose.SPIN_ATTACK);
        register("isPoseCrouching", caps_isPoseCrouching, (entity, arg) -> entity.getPose() == Pose.CROUCHING);
        register("isPoseDying", caps_isPoseDying, (entity, arg) -> entity.isDeadOrDying());
        register("sleepingDirection", caps_sleepingDirection, (entity, arg) -> entity.getBedOrientation());
    }

    private static void register(String name, int index, Getter getter) {
        register(name, index, getter, EMPTY_SETTER);
    }

    private static void register(String name, int index, Getter getter, Setter setter) {
        caps.putIfAbsent(name, index);
        capGetter.put(index, getter);
        capSetter.put(index, setter);
    }

    public EntityCaps(LivingEntity pOwner) {
        owner = pOwner;
    }

    @Override
    public Map<String, Integer> getModelCaps() { return caps; }

    @Override
    public Object getCapsValue(int index, Object... arg) {
        return capGetter.computeIfAbsent(index, i -> (entity, arg1) -> null).get(owner, arg);
    }

    @Override
    public boolean setCapsValue(int index, Object... arg) {
        return capSetter.computeIfAbsent(index, i -> (entity, arg1) -> false).set(owner, arg);
    }

    private static double getRidingYOffset(LivingEntity entity) {
        if (entity.getVehicle() instanceof LivingEntity vehicle) {
            return vehicle.getBbHeight() * 0.75 - entity.getBbHeight() * 0.5;
        }
        return entity.getBbHeight() * 0.5;
    }

    public interface Getter {
        Object get(LivingEntity entity, Object... arg);
    }

    public interface Setter {
        boolean set(LivingEntity entity, Object... arg);
    }
}
