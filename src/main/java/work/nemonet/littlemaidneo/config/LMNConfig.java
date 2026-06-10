package work.nemonet.littlemaidneo.config;

import net.minecraft.tags.BiomeTags;
import net.neoforged.neoforge.common.ModConfigSpec;
import work.nemonet.littlemaidneo.tags.LMTags;

import java.util.Arrays;
import java.util.List;

public class LMNConfig {

    public static final ModConfigSpec SPEC;

    // --- Spawn ---
    static ModConfigSpec.BooleanValue SPAWN_CAN_NATURAL_SPAWN;
    static ModConfigSpec.BooleanValue SPAWN_CAN_DESPAWN;
    static ModConfigSpec.ConfigValue<List<? extends String>> SPAWN_MAID_SPAWN_BIOME_TAGS;
    static ModConfigSpec.ConfigValue<List<? extends String>> SPAWN_MAID_SPAWN_EXCLUDE_BIOME_TAGS;
    static ModConfigSpec.IntValue SPAWN_SPAWN_WEIGHT;
    static ModConfigSpec.IntValue SPAWN_MIN_SPAWN_GROUP_SIZE;
    static ModConfigSpec.IntValue SPAWN_MAX_SPAWN_GROUP_SIZE;
    static ModConfigSpec.BooleanValue SPAWN_SILENT_DEFAULT_VOICE;
    static ModConfigSpec.ConfigValue<String> SPAWN_DEFAULT_SOUND_PACK_NAME;

    // --- Health ---
    static ModConfigSpec.IntValue HEALTH_HEAL_INTERVAL;
    static ModConfigSpec.IntValue HEALTH_HEAL_AMOUNT;
    static ModConfigSpec.DoubleValue HEALTH_HEAL_DELAY_THRESHOLD;
    static ModConfigSpec.BooleanValue HEALTH_DISABLE_MAID_DEATH;
    static ModConfigSpec.DoubleValue HEALTH_GENERAL_DAMAGE_FACTOR;
    static ModConfigSpec.DoubleValue HEALTH_BATTLE_MODE_DAMAGE_FACTOR;
    static ModConfigSpec.DoubleValue HEALTH_NON_BATTLE_MODE_DAMAGE_FACTOR;
    static ModConfigSpec.DoubleValue HEALTH_EMERGENCY_HEALTH_THRESHOLD;
    static ModConfigSpec.BooleanValue HEALTH_ENABLE_WORK_IN_EMERGENCY;
    static ModConfigSpec.BooleanValue HEALTH_ENABLE_FRIENDLY_FIRE;
    static ModConfigSpec.BooleanValue HEALTH_ENABLE_SAFE_MOVE;
    static ModConfigSpec.BooleanValue HEALTH_IMMORTAL;
    static ModConfigSpec.BooleanValue HEALTH_FALL_IMMUNITY;
    static ModConfigSpec.BooleanValue HEALTH_NON_MOB_DAMAGE_IMMUNITY;

    // --- Movement ---
    static ModConfigSpec.DoubleValue MOVEMENT_FREEDOM_SPEED;
    static ModConfigSpec.DoubleValue MOVEMENT_FREEDOM_RANGE;
    static ModConfigSpec.DoubleValue MOVEMENT_TRACER_SPEED;
    static ModConfigSpec.IntValue MOVEMENT_TRACER_HORIZON_RANGE;
    static ModConfigSpec.IntValue MOVEMENT_TRACER_VERTICAL_RANGE;
    static ModConfigSpec.DoubleValue MOVEMENT_FOLLOW_SPEED;
    static ModConfigSpec.DoubleValue MOVEMENT_FOLLOW_START_DISTANCE;
    static ModConfigSpec.DoubleValue MOVEMENT_FOLLOW_END_DISTANCE;
    static ModConfigSpec.DoubleValue MOVEMENT_SPRINT_SPEED;
    static ModConfigSpec.DoubleValue MOVEMENT_SPRINT_START_DISTANCE;
    static ModConfigSpec.DoubleValue MOVEMENT_SPRINT_END_DISTANCE;
    static ModConfigSpec.DoubleValue MOVEMENT_TELEPORT_START_DISTANCE;
    static ModConfigSpec.DoubleValue MOVEMENT_EMERGENCY_TELEPORT_START_DISTANCE;
    static ModConfigSpec.IntValue MOVEMENT_TELEPORT_WIDTH;
    static ModConfigSpec.IntValue MOVEMENT_TELEPORT_HEIGHT;
    static ModConfigSpec.BooleanValue MOVEMENT_CAN_TELEPORT_OWNER_FORWARDS;
    static ModConfigSpec.DoubleValue MOVEMENT_OWNER_FORWARD_RANGE;
    static ModConfigSpec.IntValue MOVEMENT_MAX_TRY_TELEPORT_COUNT;
    static ModConfigSpec.DoubleValue MOVEMENT_PICKUP_ITEM_SPEED;
    static ModConfigSpec.DoubleValue MOVEMENT_PICKUP_ITEM_RANGE;
    static ModConfigSpec.IntValue MOVEMENT_PICKUP_ITEM_FREQUENCY;
    static ModConfigSpec.DoubleValue MOVEMENT_ESCAPE_SPEED;

    // --- Work ---
    static ModConfigSpec.IntValue WORK_DEFAULT_WORK_ITEM_SLOT_SIZE;
    static ModConfigSpec.DoubleValue WORK_MAX_TARGET_RANGE;
    static ModConfigSpec.DoubleValue WORK_FENCER_ATTACK_DISTANCE_FACTOR;
    static ModConfigSpec.DoubleValue WORK_FENCER_ATTACK_RATE_FACTOR;
    static ModConfigSpec.DoubleValue WORK_ARCHER_SHOOT_DISTANCE_FACTOR;
    static ModConfigSpec.DoubleValue WORK_ARCHER_SHOOT_RATE_FACTOR;
    static ModConfigSpec.DoubleValue WORK_ARCHER_SHOOT_VELOCITY_FACTOR;
    static ModConfigSpec.IntValue WORK_TORCHER_LIGHT_LEVEL_THRESHOLD;
    static ModConfigSpec.DoubleValue WORK_SEARCH_CONTAINER_RANGE;

    // --- Contract ---
    static ModConfigSpec.IntValue CONTRACT_CONSUME_SALARY_INTERVAL;
    static ModConfigSpec.IntValue CONTRACT_UNPAID_DAYS_LIMIT;
    static ModConfigSpec.IntValue CONTRACT_MAX_AUTO_SALARY_RECEIPT_SLOT_SIZE;
    static ModConfigSpec.IntValue CONTRACT_START_AUTO_SALARY_RECEIPT_SLOT_THRESHOLD;
    static ModConfigSpec.IntValue CONTRACT_MAX_MEMORY_SALARY_BOX_POS;
    static ModConfigSpec.DoubleValue CONTRACT_MEMORY_SALARY_BOX_DISTANCE;
    static ModConfigSpec.IntValue CONTRACT_MEMORY_SALARY_BOX_INTERVAL;
    static ModConfigSpec.DoubleValue CONTRACT_SEARCH_SALARY_BOX_DISTANCE;
    static ModConfigSpec.IntValue CONTRACT_START_INTERVAL_OF_AUTO_SALARY_RECEIPT;
    static ModConfigSpec.IntValue CONTRACT_FIND_PATH_INTERVAL_OF_AUTO_SALARY_RECEIPT;
    static ModConfigSpec.IntValue CONTRACT_MAX_MOVE_TIME_ON_AUTO_SALARY_RECEIPT;
    static ModConfigSpec.IntValue CONTRACT_MAX_MOVE_TIME_AFTER_AUTO_SALARY_RECEIPT;

    // --- Misc ---
    static ModConfigSpec.BooleanValue MISC_CAN_PICKUP_ITEM;
    static ModConfigSpec.BooleanValue MISC_CAN_PICKUP_EXPERIENCE_ORB;
    static ModConfigSpec.BooleanValue MISC_CAN_PICKUP_ITEM_BY_NO_OWNER;
    static ModConfigSpec.BooleanValue MISC_CAN_MILKING;
    static ModConfigSpec.IntValue MISC_PLAY_SOUND_INTERVAL;
    static ModConfigSpec.DoubleValue MISC_FOLLOW_AT_HELD_SALARY_RANGE;
    static ModConfigSpec.DoubleValue MISC_FOLLOW_AT_HELD_EMPLOY_ITEM_RANGE;
    static ModConfigSpec.DoubleValue MISC_STARE_AT_SALARY_RANGE;
    static ModConfigSpec.DoubleValue MISC_STARE_AT_EMPLOY_ITEM_RANGE;
    static ModConfigSpec.IntValue MISC_MAX_ACCELERATION_STACK;
    static ModConfigSpec.IntValue MISC_ACCELERATION_TICKS_PER_STACK;
    static ModConfigSpec.IntValue MISC_ACCELERATION_MULTIPLE;

    // --- Target ---
    static ModConfigSpec.IntValue TARGET_ALERT_RANGE;
    static ModConfigSpec.IntValue TARGET_TARGETING_FREQUENCY;
    static ModConfigSpec.IntValue TARGET_COMBAT_RANGE;
    static ModConfigSpec.IntValue TARGET_DANGEROUS_AVOID_DISTANCE;
    static ModConfigSpec.DoubleValue TARGET_DISTRIBUTION_RATIO;
    static ModConfigSpec.IntValue TARGET_MAX_ATTACKERS_PER_TARGET;
    static ModConfigSpec.DoubleValue TARGET_INJURED_THRESHOLD;
    static ModConfigSpec.IntValue TARGET_ATTACKED_BY_VALID_TICKS;

    // --- Client ---
    static ModConfigSpec.BooleanValue CLIENT_ENABLE_WAIT_POSE_ON_MOVING;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("spawn");
        SPAWN_CAN_NATURAL_SPAWN = builder.define("canNaturalSpawn", true);
        SPAWN_CAN_DESPAWN = builder.define("canDespawn", false);
        SPAWN_MAID_SPAWN_BIOME_TAGS = builder.defineListAllowEmpty("maidSpawnBiomeTags",
                Arrays.asList(
                        LMTags.Biomes.MAID_SPAWN_BIOME.location().toString(),
                        BiomeTags.HAS_VILLAGE_DESERT.location().toString(),
                        BiomeTags.HAS_VILLAGE_PLAINS.location().toString(),
                        BiomeTags.HAS_VILLAGE_SAVANNA.location().toString(),
                        BiomeTags.HAS_VILLAGE_SNOWY.location().toString(),
                        BiomeTags.HAS_VILLAGE_TAIGA.location().toString()),
                e -> e instanceof String);
        SPAWN_MAID_SPAWN_EXCLUDE_BIOME_TAGS = builder.defineListAllowEmpty("maidSpawnExcludeBiomeTags",
                Arrays.asList(LMTags.Biomes.MAID_SPAWN_EXCLUDE_BIOME.location().toString()),
                e -> e instanceof String);
        SPAWN_SPAWN_WEIGHT = builder.defineInRange("spawnWeight", 5, 0, 1000);
        SPAWN_MIN_SPAWN_GROUP_SIZE = builder.defineInRange("minSpawnGroupSize", 1, 1, 64);
        SPAWN_MAX_SPAWN_GROUP_SIZE = builder.defineInRange("maxSpawnGroupSize", 3, 1, 64);
        SPAWN_SILENT_DEFAULT_VOICE = builder.define("silentDefaultVoice", false);
        SPAWN_DEFAULT_SOUND_PACK_NAME = builder.define("defaultSoundPackName", "");
        builder.pop();

        builder.push("health");
        HEALTH_HEAL_INTERVAL = builder.defineInRange("healInterval", 2, 1, Integer.MAX_VALUE);
        HEALTH_HEAL_AMOUNT = builder.defineInRange("healAmount", 1, 1, Integer.MAX_VALUE);
        HEALTH_HEAL_DELAY_THRESHOLD = builder.defineInRange("healDelayThreshold", 0.75, 0.0, 1.0);
        HEALTH_DISABLE_MAID_DEATH = builder.define("disableMaidDeath", false);
        HEALTH_GENERAL_DAMAGE_FACTOR = builder.defineInRange("generalMaidDamageFactor", 1.0, 0.0, 100.0);
        HEALTH_BATTLE_MODE_DAMAGE_FACTOR = builder.defineInRange("battleModeMaidDamageFactor", 1.0, 0.0, 100.0);
        HEALTH_NON_BATTLE_MODE_DAMAGE_FACTOR = builder.defineInRange("nonBattleModeMaidDamageFactor", 1.0, 0.0, 100.0);
        HEALTH_EMERGENCY_HEALTH_THRESHOLD = builder.defineInRange("emergencyMaidHealthThreshold", 0.5, 0.0, 1.0);
        HEALTH_ENABLE_WORK_IN_EMERGENCY = builder.define("enableWorkInEmergency", false);
        HEALTH_ENABLE_FRIENDLY_FIRE = builder.define("enableFriendlyFire", false);
        HEALTH_ENABLE_SAFE_MOVE = builder.define("enableSafeMove", true);
        HEALTH_IMMORTAL = builder.define("immortal", false);
        HEALTH_FALL_IMMUNITY = builder.define("fallImmunity", false);
        HEALTH_NON_MOB_DAMAGE_IMMUNITY = builder.define("nonMobDamageImmunity", false);
        builder.pop();

        builder.push("movement");
        MOVEMENT_FREEDOM_SPEED = builder.defineInRange("freedomSpeed", 0.65, 0.0, 10.0);
        MOVEMENT_FREEDOM_RANGE = builder.defineInRange("freedomRange", 16.0, 0.0, 256.0);
        MOVEMENT_TRACER_SPEED = builder.defineInRange("tracerSpeed", 0.65, 0.0, 10.0);
        MOVEMENT_TRACER_HORIZON_RANGE = builder.defineInRange("tracerHorizonRange", 4, 0, 64);
        MOVEMENT_TRACER_VERTICAL_RANGE = builder.defineInRange("tracerVerticalRange", 2, 0, 64);
        MOVEMENT_FOLLOW_SPEED = builder.defineInRange("followSpeed", 1.0, 0.0, 10.0);
        MOVEMENT_FOLLOW_START_DISTANCE = builder.defineInRange("followStartDistance", 6.0, 0.0, 256.0);
        MOVEMENT_FOLLOW_END_DISTANCE = builder.defineInRange("followEndDistance", 5.0, 0.0, 256.0);
        MOVEMENT_SPRINT_SPEED = builder.defineInRange("sprintSpeed", 1.2, 0.0, 10.0);
        MOVEMENT_SPRINT_START_DISTANCE = builder.defineInRange("sprintStartDistance", 8.0, 0.0, 256.0);
        MOVEMENT_SPRINT_END_DISTANCE = builder.defineInRange("sprintEndDistance", 6.0, 0.0, 256.0);
        MOVEMENT_TELEPORT_START_DISTANCE = builder.defineInRange("teleportStartDistance", 16.0, 0.0, 256.0);
        MOVEMENT_EMERGENCY_TELEPORT_START_DISTANCE = builder.defineInRange("emergencyTeleportStartDistance", 6.0, 0.0, 256.0);
        MOVEMENT_TELEPORT_WIDTH = builder.defineInRange("teleportWidth", 3, 0, 32);
        MOVEMENT_TELEPORT_HEIGHT = builder.defineInRange("teleportHeight", 1, 0, 32);
        MOVEMENT_CAN_TELEPORT_OWNER_FORWARDS = builder.define("canTeleportOwnerForwards", false);
        MOVEMENT_OWNER_FORWARD_RANGE = builder.defineInRange("ownerForwardRange", 4.0, 0.0, 64.0);
        MOVEMENT_MAX_TRY_TELEPORT_COUNT = builder.defineInRange("maxTryTeleportCount", 10, 1, 100);
        MOVEMENT_PICKUP_ITEM_SPEED = builder.defineInRange("pickupItemSpeed", 1.0, 0.0, 10.0);
        MOVEMENT_PICKUP_ITEM_RANGE = builder.defineInRange("pickupItemRange", 8.0, 0.0, 256.0);
        MOVEMENT_PICKUP_ITEM_FREQUENCY = builder.defineInRange("pickupItemFrequency", 40, 1, 1000);
        MOVEMENT_ESCAPE_SPEED = builder.defineInRange("escapeSpeed", 1.2, 0.0, 10.0);
        builder.pop();

        builder.push("work");
        WORK_DEFAULT_WORK_ITEM_SLOT_SIZE = builder.defineInRange("defaultWorkItemSlotSize", 9, 0, 18);
        WORK_MAX_TARGET_RANGE = builder.defineInRange("maxTargetRange", 16.0, 0.0, 256.0);
        WORK_FENCER_ATTACK_DISTANCE_FACTOR = builder.defineInRange("fencerAttackDistanceFactor", 1.0, 0.0, 10.0);
        WORK_FENCER_ATTACK_RATE_FACTOR = builder.defineInRange("fencerAttackRateFactor", 0.75, 0.0, 10.0);
        WORK_ARCHER_SHOOT_DISTANCE_FACTOR = builder.defineInRange("archerShootDistanceFactor", 1.0, 0.0, 10.0);
        WORK_ARCHER_SHOOT_RATE_FACTOR = builder.defineInRange("archerShootRateFactor", 1.0, 0.0, 10.0);
        WORK_ARCHER_SHOOT_VELOCITY_FACTOR = builder.defineInRange("archerShootVelocityFactor", 1.0, 0.0, 10.0);
        WORK_TORCHER_LIGHT_LEVEL_THRESHOLD = builder.defineInRange("torcherLightLevelThreshold", 7, 0, 15);
        WORK_SEARCH_CONTAINER_RANGE = builder.defineInRange("searchContainerRange", 8.0, 0.0, 256.0);
        builder.pop();

        builder.push("contract");
        CONTRACT_CONSUME_SALARY_INTERVAL = builder.defineInRange("consumeSalaryInterval", 24000, 1, Integer.MAX_VALUE);
        CONTRACT_UNPAID_DAYS_LIMIT = builder.defineInRange("unpaidDaysLimit", 7, 0, Integer.MAX_VALUE);
        CONTRACT_MAX_AUTO_SALARY_RECEIPT_SLOT_SIZE = builder.defineInRange("maxAutoSalaryReceiptSlotSize", 3, 0, 36);
        CONTRACT_START_AUTO_SALARY_RECEIPT_SLOT_THRESHOLD = builder.defineInRange("startAutoSalaryReceiptSlotThreshold", 1, 0, 36);
        CONTRACT_MAX_MEMORY_SALARY_BOX_POS = builder.defineInRange("maxMemorySalaryBoxPos", 4, 0, 64);
        CONTRACT_MEMORY_SALARY_BOX_DISTANCE = builder.defineInRange("memorySalaryBoxDistance", 8.0, 0.0, 256.0);
        CONTRACT_MEMORY_SALARY_BOX_INTERVAL = builder.defineInRange("memorySalaryBoxInterval", 20, 1, Integer.MAX_VALUE);
        CONTRACT_SEARCH_SALARY_BOX_DISTANCE = builder.defineInRange("searchSalaryBoxDistance", 16.0, 0.0, 256.0);
        CONTRACT_START_INTERVAL_OF_AUTO_SALARY_RECEIPT = builder.defineInRange("startIntervalOfAutoSalaryReceipt", 60, 1, Integer.MAX_VALUE);
        CONTRACT_FIND_PATH_INTERVAL_OF_AUTO_SALARY_RECEIPT = builder.defineInRange("findPathIntervalOfAutoSalaryReceipt", 10, 1, Integer.MAX_VALUE);
        CONTRACT_MAX_MOVE_TIME_ON_AUTO_SALARY_RECEIPT = builder.defineInRange("maxMoveTimeOnAutoSalaryReceipt", 200, 1, Integer.MAX_VALUE);
        CONTRACT_MAX_MOVE_TIME_AFTER_AUTO_SALARY_RECEIPT = builder.defineInRange("maxMoveTimeAfterAutoSalaryReceipt", 400, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.push("misc");
        MISC_CAN_PICKUP_ITEM = builder.define("canPickupItem", true);
        MISC_CAN_PICKUP_EXPERIENCE_ORB = builder.define("canPickupExperienceOrb", true);
        MISC_CAN_PICKUP_ITEM_BY_NO_OWNER = builder.define("canPickupItemByNoOwner", false);
        MISC_CAN_MILKING = builder.define("canMilking", false);
        MISC_PLAY_SOUND_INTERVAL = builder.defineInRange("playSoundInterval", 5, 1, Integer.MAX_VALUE);
        MISC_FOLLOW_AT_HELD_SALARY_RANGE = builder.defineInRange("followAtHeldSalaryRange", 1.5, 0.0, 64.0);
        MISC_FOLLOW_AT_HELD_EMPLOY_ITEM_RANGE = builder.defineInRange("followAtHeldEmployItemRange", 1.5, 0.0, 64.0);
        MISC_STARE_AT_SALARY_RANGE = builder.defineInRange("stareAtSalaryRange", 4.0, 0.0, 64.0);
        MISC_STARE_AT_EMPLOY_ITEM_RANGE = builder.defineInRange("stareAtEmployItemRange", 4.0, 0.0, 64.0);
        MISC_MAX_ACCELERATION_STACK = builder.defineInRange("maxAccelerationStack", 8, 0, 64);
        MISC_ACCELERATION_TICKS_PER_STACK = builder.defineInRange("accelerationTicksPerStack", 80, 1, Integer.MAX_VALUE);
        MISC_ACCELERATION_MULTIPLE = builder.defineInRange("accelerationMultiple", 2, 1, 100);
        builder.pop();

        builder.push("target");
        TARGET_ALERT_RANGE = builder.defineInRange("alertRange", 16, 0, 256);
        TARGET_COMBAT_RANGE = builder.defineInRange("combatRange", 8, 0, 256);
        TARGET_DANGEROUS_AVOID_DISTANCE = builder.defineInRange("dangerousAvoidDistance", 8, 0, 256);
        TARGET_DISTRIBUTION_RATIO = builder.defineInRange("distributionRatio", 0.5, 0.0, 1.0);
        TARGET_MAX_ATTACKERS_PER_TARGET = builder.defineInRange("maxAttackersPerTarget", 2, 1, 100);
        TARGET_INJURED_THRESHOLD = builder.defineInRange("injuredThreshold", 0.5, 0.0, 1.0);
        TARGET_ATTACKED_BY_VALID_TICKS = builder.defineInRange("attackedByValidTicks", 200, 1, Integer.MAX_VALUE);
        TARGET_TARGETING_FREQUENCY = builder.defineInRange("targetingFrequency", 10, 1, 100);
        builder.pop();

        builder.push("client");
        CLIENT_ENABLE_WAIT_POSE_ON_MOVING = builder.define("enableWaitPoseOnMoving", false);
        builder.pop();

        SPEC = builder.build();
    }

    // Singleton instance (populated by bake())
    private static final LMNConfig INSTANCE = new LMNConfig();

    public final Spawn spawn = new Spawn();
    public final Health health = new Health();
    public final Movement movement = new Movement();
    public final Work work = new Work();
    public final Contract contract = new Contract();
    public final Misc misc = new Misc();
    public final Target target = new Target();
    public final Client client = new Client();

    public static LMNConfig get() {
        return INSTANCE;
    }

    public static void bake() {
        INSTANCE.spawn.canNaturalSpawn = SPAWN_CAN_NATURAL_SPAWN.get();
        INSTANCE.spawn.canDespawn = SPAWN_CAN_DESPAWN.get();
        INSTANCE.spawn.maidSpawnBiomeTags = SPAWN_MAID_SPAWN_BIOME_TAGS.get().stream().map(Object::toString).toList();
        INSTANCE.spawn.maidSpawnExcludeBiomeTags = SPAWN_MAID_SPAWN_EXCLUDE_BIOME_TAGS.get().stream().map(Object::toString).toList();
        INSTANCE.spawn.spawnWeight = SPAWN_SPAWN_WEIGHT.get();
        INSTANCE.spawn.minSpawnGroupSize = SPAWN_MIN_SPAWN_GROUP_SIZE.get();
        INSTANCE.spawn.maxSpawnGroupSize = SPAWN_MAX_SPAWN_GROUP_SIZE.get();
        INSTANCE.spawn.silentDefaultVoice = SPAWN_SILENT_DEFAULT_VOICE.get();
        INSTANCE.spawn.defaultSoundPackName = SPAWN_DEFAULT_SOUND_PACK_NAME.get();

        INSTANCE.health.healInterval = HEALTH_HEAL_INTERVAL.get();
        INSTANCE.health.healAmount = HEALTH_HEAL_AMOUNT.get();
        INSTANCE.health.healDelayThreshold = HEALTH_HEAL_DELAY_THRESHOLD.get().floatValue();
        INSTANCE.health.disableMaidDeath = HEALTH_DISABLE_MAID_DEATH.get();
        INSTANCE.health.generalMaidDamageFactor = HEALTH_GENERAL_DAMAGE_FACTOR.get().floatValue();
        INSTANCE.health.battleModeMaidDamageFactor = HEALTH_BATTLE_MODE_DAMAGE_FACTOR.get().floatValue();
        INSTANCE.health.nonBattleModeMaidDamageFactor = HEALTH_NON_BATTLE_MODE_DAMAGE_FACTOR.get().floatValue();
        INSTANCE.health.emergencyMaidHealthThreshold = HEALTH_EMERGENCY_HEALTH_THRESHOLD.get().floatValue();
        INSTANCE.health.enableWorkInEmergency = HEALTH_ENABLE_WORK_IN_EMERGENCY.get();
        INSTANCE.health.enableFriendlyFire = HEALTH_ENABLE_FRIENDLY_FIRE.get();
        INSTANCE.health.enableSafeMove = HEALTH_ENABLE_SAFE_MOVE.get();
        INSTANCE.health.immortal = HEALTH_IMMORTAL.get();
        INSTANCE.health.fallImmunity = HEALTH_FALL_IMMUNITY.get();
        INSTANCE.health.nonMobDamageImmunity = HEALTH_NON_MOB_DAMAGE_IMMUNITY.get();

        INSTANCE.movement.freedomSpeed = MOVEMENT_FREEDOM_SPEED.get().floatValue();
        INSTANCE.movement.freedomRange = MOVEMENT_FREEDOM_RANGE.get().floatValue();
        INSTANCE.movement.tracerSpeed = MOVEMENT_TRACER_SPEED.get().floatValue();
        INSTANCE.movement.tracerHorizonRange = MOVEMENT_TRACER_HORIZON_RANGE.get();
        INSTANCE.movement.tracerVerticalRange = MOVEMENT_TRACER_VERTICAL_RANGE.get();
        INSTANCE.movement.followSpeed = MOVEMENT_FOLLOW_SPEED.get().floatValue();
        INSTANCE.movement.followStartDistance = MOVEMENT_FOLLOW_START_DISTANCE.get().floatValue();
        INSTANCE.movement.followEndDistance = MOVEMENT_FOLLOW_END_DISTANCE.get().floatValue();
        INSTANCE.movement.sprintSpeed = MOVEMENT_SPRINT_SPEED.get().floatValue();
        INSTANCE.movement.sprintStartDistance = MOVEMENT_SPRINT_START_DISTANCE.get().floatValue();
        INSTANCE.movement.sprintEndDistance = MOVEMENT_SPRINT_END_DISTANCE.get().floatValue();
        INSTANCE.movement.teleportStartDistance = MOVEMENT_TELEPORT_START_DISTANCE.get().floatValue();
        INSTANCE.movement.emergencyTeleportStartDistance = MOVEMENT_EMERGENCY_TELEPORT_START_DISTANCE.get().floatValue();
        INSTANCE.movement.teleportWidth = MOVEMENT_TELEPORT_WIDTH.get();
        INSTANCE.movement.teleportHeight = MOVEMENT_TELEPORT_HEIGHT.get();
        INSTANCE.movement.canTeleportOwnerForwards = MOVEMENT_CAN_TELEPORT_OWNER_FORWARDS.get();
        INSTANCE.movement.ownerForwardRange = MOVEMENT_OWNER_FORWARD_RANGE.get().floatValue();
        INSTANCE.movement.maxTryTeleportCount = MOVEMENT_MAX_TRY_TELEPORT_COUNT.get();
        INSTANCE.movement.pickupItemSpeed = MOVEMENT_PICKUP_ITEM_SPEED.get().floatValue();
        INSTANCE.movement.pickupItemRange = MOVEMENT_PICKUP_ITEM_RANGE.get().floatValue();
        INSTANCE.movement.pickupItemFrequency = MOVEMENT_PICKUP_ITEM_FREQUENCY.get();
        INSTANCE.movement.escapeSpeed = MOVEMENT_ESCAPE_SPEED.get().floatValue();

        INSTANCE.work.defaultWorkItemSlotSize = WORK_DEFAULT_WORK_ITEM_SLOT_SIZE.get();
        INSTANCE.work.maxTargetRange = WORK_MAX_TARGET_RANGE.get().floatValue();
        INSTANCE.work.fencerAttackDistanceFactor = WORK_FENCER_ATTACK_DISTANCE_FACTOR.get().floatValue();
        INSTANCE.work.fencerAttackRateFactor = WORK_FENCER_ATTACK_RATE_FACTOR.get().floatValue();
        INSTANCE.work.archerShootDistanceFactor = WORK_ARCHER_SHOOT_DISTANCE_FACTOR.get().floatValue();
        INSTANCE.work.archerShootRateFactor = WORK_ARCHER_SHOOT_RATE_FACTOR.get().floatValue();
        INSTANCE.work.archerShootVelocityFactor = WORK_ARCHER_SHOOT_VELOCITY_FACTOR.get().floatValue();
        INSTANCE.work.torcherLightLevelThreshold = WORK_TORCHER_LIGHT_LEVEL_THRESHOLD.get();
        INSTANCE.work.searchContainerRange = WORK_SEARCH_CONTAINER_RANGE.get().floatValue();

        INSTANCE.contract.consumeSalaryInterval = CONTRACT_CONSUME_SALARY_INTERVAL.get();
        INSTANCE.contract.unpaidDaysLimit = CONTRACT_UNPAID_DAYS_LIMIT.get();
        INSTANCE.contract.maxAutoSalaryReceiptSlotSize = CONTRACT_MAX_AUTO_SALARY_RECEIPT_SLOT_SIZE.get();
        INSTANCE.contract.startAutoSalaryReceiptSlotThreshold = CONTRACT_START_AUTO_SALARY_RECEIPT_SLOT_THRESHOLD.get();
        INSTANCE.contract.maxMemorySalaryBoxPos = CONTRACT_MAX_MEMORY_SALARY_BOX_POS.get();
        INSTANCE.contract.memorySalaryBoxDistance = CONTRACT_MEMORY_SALARY_BOX_DISTANCE.get().floatValue();
        INSTANCE.contract.memorySalaryBoxInterval = CONTRACT_MEMORY_SALARY_BOX_INTERVAL.get();
        INSTANCE.contract.searchSalaryBoxDistance = CONTRACT_SEARCH_SALARY_BOX_DISTANCE.get().floatValue();
        INSTANCE.contract.startIntervalOfAutoSalaryReceipt = CONTRACT_START_INTERVAL_OF_AUTO_SALARY_RECEIPT.get();
        INSTANCE.contract.findPathIntervalOfAutoSalaryReceipt = CONTRACT_FIND_PATH_INTERVAL_OF_AUTO_SALARY_RECEIPT.get();
        INSTANCE.contract.maxMoveTimeOnAutoSalaryReceipt = CONTRACT_MAX_MOVE_TIME_ON_AUTO_SALARY_RECEIPT.get();
        INSTANCE.contract.maxMoveTimeAfterAutoSalaryReceipt = CONTRACT_MAX_MOVE_TIME_AFTER_AUTO_SALARY_RECEIPT.get();

        INSTANCE.misc.canPickupItem = MISC_CAN_PICKUP_ITEM.get();
        INSTANCE.misc.canPickupExperienceOrb = MISC_CAN_PICKUP_EXPERIENCE_ORB.get();
        INSTANCE.misc.canPickupItemByNoOwner = MISC_CAN_PICKUP_ITEM_BY_NO_OWNER.get();
        INSTANCE.misc.canMilking = MISC_CAN_MILKING.get();
        INSTANCE.misc.playSoundInterval = MISC_PLAY_SOUND_INTERVAL.get();
        INSTANCE.misc.followAtHeldSalaryRange = MISC_FOLLOW_AT_HELD_SALARY_RANGE.get().floatValue();
        INSTANCE.misc.followAtHeldEmployItemRange = MISC_FOLLOW_AT_HELD_EMPLOY_ITEM_RANGE.get().floatValue();
        INSTANCE.misc.stareAtSalaryRange = MISC_STARE_AT_SALARY_RANGE.get().floatValue();
        INSTANCE.misc.stareAtEmployItemRange = MISC_STARE_AT_EMPLOY_ITEM_RANGE.get().floatValue();
        INSTANCE.misc.maxAccelerationStack = MISC_MAX_ACCELERATION_STACK.get();
        INSTANCE.misc.accelerationTicksPerStack = MISC_ACCELERATION_TICKS_PER_STACK.get();
        INSTANCE.misc.accelerationMultiple = MISC_ACCELERATION_MULTIPLE.get();

        INSTANCE.target.alertRange = TARGET_ALERT_RANGE.get();
        INSTANCE.target.combatRange = TARGET_COMBAT_RANGE.get();
        INSTANCE.target.dangerousAvoidDistance = TARGET_DANGEROUS_AVOID_DISTANCE.get();
        INSTANCE.target.distributionRatio = TARGET_DISTRIBUTION_RATIO.get();
        INSTANCE.target.maxAttackersPerTarget = TARGET_MAX_ATTACKERS_PER_TARGET.get();
        INSTANCE.target.injuredThreshold = TARGET_INJURED_THRESHOLD.get().floatValue();
        INSTANCE.target.attackedByValidTicks = TARGET_ATTACKED_BY_VALID_TICKS.get();
        INSTANCE.target.targetingFrequency = TARGET_TARGETING_FREQUENCY.get();

        INSTANCE.client.enableWaitPoseOnMoving = CLIENT_ENABLE_WAIT_POSE_ON_MOVING.get();
    }

    public static class Spawn {
        public boolean canNaturalSpawn = true;
        public boolean canDespawn = false;
        public List<String> maidSpawnBiomeTags = List.of();
        public List<String> maidSpawnExcludeBiomeTags = List.of();
        public int spawnWeight = 5;
        public int minSpawnGroupSize = 1;
        public int maxSpawnGroupSize = 3;
        public boolean silentDefaultVoice = false;
        public String defaultSoundPackName = "";
    }

    public static class Health {
        public int healInterval = 2;
        public int healAmount = 1;
        public float healDelayThreshold = 0.75f;
        public boolean disableMaidDeath = false;
        public float generalMaidDamageFactor = 1.0f;
        public float battleModeMaidDamageFactor = 1.0f;
        public float nonBattleModeMaidDamageFactor = 1.0f;
        public float emergencyMaidHealthThreshold = 0.5f;
        public boolean enableWorkInEmergency = false;
        public boolean enableFriendlyFire = false;
        public boolean enableSafeMove = true;
        public boolean immortal = false;
        public boolean fallImmunity = false;
        public boolean nonMobDamageImmunity = false;
    }

    public static class Movement {
        public float freedomSpeed = 0.65f;
        public float freedomRange = 16.0f;
        public float tracerSpeed = 0.65f;
        public int tracerHorizonRange = 4;
        public int tracerVerticalRange = 2;
        public float followSpeed = 1.0f;
        public float followStartDistance = 6.0f;
        public float followEndDistance = 5.0f;
        public float sprintSpeed = 1.2f;
        public float sprintStartDistance = 8.0f;
        public float sprintEndDistance = 6.0f;
        public float teleportStartDistance = 16.0f;
        public float emergencyTeleportStartDistance = 6.0f;
        public int teleportWidth = 3;
        public int teleportHeight = 1;
        public boolean canTeleportOwnerForwards = false;
        public float ownerForwardRange = 4.0f;
        public int maxTryTeleportCount = 10;
        public float pickupItemSpeed = 1.0f;
        public float pickupItemRange = 8.0f;
        public int pickupItemFrequency = 40;
        public float escapeSpeed = 1.2f;
    }

    public static class Work {
        public int defaultWorkItemSlotSize = 9;
        public float maxTargetRange = 16f;
        public float fencerAttackDistanceFactor = 1.0f;
        public float fencerAttackRateFactor = 0.75f;
        public float archerShootDistanceFactor = 1.0f;
        public float archerShootRateFactor = 1.0f;
        public float archerShootVelocityFactor = 1.0f;
        public int torcherLightLevelThreshold = 7;
        public float searchContainerRange = 8.0f;
    }

    public static class Contract {
        public int consumeSalaryInterval = 24000;
        public int unpaidDaysLimit = 7;
        public int maxAutoSalaryReceiptSlotSize = 3;
        public int startAutoSalaryReceiptSlotThreshold = 1;
        public int maxMemorySalaryBoxPos = 4;
        public float memorySalaryBoxDistance = 8.0f;
        public int memorySalaryBoxInterval = 20;
        public float searchSalaryBoxDistance = 16.0f;
        public int startIntervalOfAutoSalaryReceipt = 60;
        public int findPathIntervalOfAutoSalaryReceipt = 10;
        public int maxMoveTimeOnAutoSalaryReceipt = 200;
        public int maxMoveTimeAfterAutoSalaryReceipt = 400;
    }

    public static class Misc {
        public boolean canPickupItem = true;
        public boolean canPickupExperienceOrb = true;
        public boolean canPickupItemByNoOwner = false;
        public boolean canMilking = false;
        public int playSoundInterval = 5;
        public float followAtHeldSalaryRange = 1.5f;
        public float followAtHeldEmployItemRange = 1.5f;
        public float stareAtSalaryRange = 4.0f;
        public float stareAtEmployItemRange = 4.0f;
        public int maxAccelerationStack = 8;
        public int accelerationTicksPerStack = 80;
        public int accelerationMultiple = 2;
    }

    public static class Target {
        public int alertRange = 16;
        public int combatRange = 8;
        public int dangerousAvoidDistance = 8;
        public double distributionRatio = 0.5;
        public int maxAttackersPerTarget = 2;
        public float injuredThreshold = 0.5f;
        public int attackedByValidTicks = 200;
        public int targetingFrequency = 10;
    }

    public static class Client {
        public boolean enableWaitPoseOnMoving = false;
    }
}
