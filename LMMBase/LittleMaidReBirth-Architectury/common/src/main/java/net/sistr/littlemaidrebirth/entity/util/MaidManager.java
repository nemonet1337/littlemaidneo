package net.sistr.littlemaidrebirth.entity.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity.MaidSoul;
import net.sistr.littlemaidrebirth.entity.MaidSoulEntity;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaidManager {
    void registerMaid(LittleMaidEntity maid);

    void registerMaid(MaidSoulEntity soul);

    void registerMaid(LittleMaidEntity.MaidSoul soul);

    List<LMInfo> getMaidList();

    void writeMaidManager(CompoundTag nbt);

    void readMaidManager(CompoundTag nbt);

    List<LittleMaidEntity.MaidSoul> getMaidSouls();

    void clearMaidSouls();

    void checkMaidUnload();

    abstract sealed class LMInfo permits MaidLMInfo, SoulEntityLMInfo, SoulLMInfo {
        protected final UUID id;
        protected final String name;
        protected final Status status;
        protected final BlockPos lastPos;
        protected final String worldId;

        protected LMInfo(UUID id, String name, Status status, BlockPos lastPos, String worldId) {
            this.id = id;
            this.name = name;
            this.status = status;
            this.lastPos = lastPos;
            this.worldId = worldId;
        }

        public UUID id() {
            return id;
        }

        public String name() {
            return name;
        }

        public Status status() {
            return status;
        }

        public void write(CompoundTag infoNbt) {
            infoNbt.putString("name", name);
            infoNbt.putString("status", status.name());
            infoNbt.putUUID("id", id);
            infoNbt.putIntArray("lastPos", new int[]{lastPos.getX(), lastPos.getY(), lastPos.getZ()});
            infoNbt.putString("worldId", worldId);
            var entityId = getEntityId();
            if (entityId != -1) {
                infoNbt.putInt("entityId", entityId);
            }
        }

        public static LMInfo read(CompoundTag infoNbt) {
            String name = infoNbt.getString("name");
            Status status = Status.valueOf(infoNbt.getString("status"));
            UUID id = infoNbt.getUUID("id");
            BlockPos lastPos = BlockPos.ZERO;
            if (infoNbt.contains("lastPos")) {
                int[] lastPosArray = infoNbt.getIntArray("lastPos");
                lastPos = new BlockPos(lastPosArray[0], lastPosArray[1], lastPosArray[2]);
            }
            String worldId = infoNbt.getString("worldId");
            int entityId = -1;
            if (infoNbt.contains("entityId")) {
                entityId = infoNbt.getInt("entityId");
            }

            if (status == Status.ALIVE) {
                return new MaidLMInfo(id, name, lastPos, worldId, null, entityId);
            } else if (status == Status.SOUL_ENTITY) {
                var soul = LittleMaidEntity.MaidSoul.fromNbt(infoNbt.getCompound("soul"));
                return new SoulEntityLMInfo(id, name, lastPos, worldId, null, soul, entityId);
            } else {
                var soul = LittleMaidEntity.MaidSoul.fromNbt(infoNbt.getCompound("soul"));
                return new SoulLMInfo(id, name, soul);
            }
        }

        public Optional<Entity> getEntityClient(Level world) {
            var entityId = getEntityId();
            if (entityId == -1) {
                return Optional.empty();
            }
            return Optional.ofNullable(world.getEntity(entityId));
        }

        public abstract Optional<Entity> getEntity();

        public abstract boolean isLoaded();

        public abstract int getEntityId();

        public BlockPos getLastPos() {
            return lastPos;
        }

        public String getWorldId() {
            return worldId;
        }
    }

    final class MaidLMInfo extends LMInfo {
        private final @Nullable LittleMaidEntity maid;
        private final int entityId;

        private MaidLMInfo(UUID id, String name, BlockPos lastPos, String worldId,
                           @Nullable LittleMaidEntity maid, int entityId) {
            super(id, name, Status.ALIVE, lastPos, worldId);
            this.maid = maid;
            this.entityId = entityId;
        }

        public @Nullable LittleMaidEntity maid() {
            return maid;
        }

        public static MaidLMInfo create(LittleMaidEntity maid, boolean loaded) {
            return new MaidLMInfo(maid.getUUID(), maid.getName().getString(), maid.blockPosition(),
                    maid.level().dimension().location().toString(),
                    loaded ? maid : null, loaded ? maid.getId() : -1);
        }

        @Override
        public Optional<Entity> getEntity() {
            return Optional.ofNullable(maid);
        }

        @Override
        public boolean isLoaded() {
            return this.maid != null || this.entityId != -1;
        }

        @Override
        public int getEntityId() {
            return this.entityId;
        }
    }

    final class SoulEntityLMInfo extends LMInfo {
        private final @Nullable MaidSoulEntity soulEntity;
        private final LittleMaidEntity.MaidSoul soul;
        private final int entityId;

        private SoulEntityLMInfo(UUID id, String name, BlockPos lastPos, String worldId,
                                 @Nullable MaidSoulEntity soulEntity, LittleMaidEntity.MaidSoul soul, int entityId) {
            super(id, name, Status.SOUL_ENTITY, lastPos, worldId);
            this.soulEntity = soulEntity;
            this.soul = soul;
            this.entityId = entityId;
        }

        public @Nullable MaidSoulEntity soulEntity() {
            return soulEntity;
        }

        public LittleMaidEntity.MaidSoul getSoul() {
            return soul;
        }

        public static SoulEntityLMInfo create(MaidSoulEntity soul, boolean loaded) {
            return new SoulEntityLMInfo(soul.getSoul().getUuid(), soul.getSoul().getName(), soul.blockPosition(),
                    soul.level().dimension().location().toString(),
                    loaded ? soul : null, soul.getSoul(), loaded ? soul.getId() : -1);
        }

        @Override
        public void write(CompoundTag infoNbt) {
            super.write(infoNbt);
            infoNbt.put("soul", soul.getNbt());
        }

        @Override
        public Optional<Entity> getEntity() {
            return Optional.ofNullable(soulEntity);
        }

        @Override
        public boolean isLoaded() {
            return this.soulEntity != null || this.entityId != -1;
        }

        @Override
        public int getEntityId() {
            return this.entityId;
        }
    }

    final class SoulLMInfo extends LMInfo {
        private final LittleMaidEntity.MaidSoul soul;

        private SoulLMInfo(UUID id, String name, LittleMaidEntity.MaidSoul soul) {
            super(id, name, Status.SOUL_WITHIN, BlockPos.ZERO, "");
            this.soul = soul;
        }

        public static SoulLMInfo create(LittleMaidEntity.MaidSoul soul) {
            return new SoulLMInfo(soul.getUuid(), soul.getName(), soul);
        }

        public LittleMaidEntity.MaidSoul soul() {
            return soul;
        }

        @Override
        public void write(CompoundTag infoNbt) {
            super.write(infoNbt);
            if (this.soul != null) {
                infoNbt.put("soul", soul.getNbt());
            }
        }

        @Override
        public Optional<Entity> getEntity() {
            return Optional.empty();
        }

        @Override
        public boolean isLoaded() {
            return false;
        }

        @Override
        public int getEntityId() {
            return -1;
        }

    }

    enum Status {
        ALIVE(Component.literal("Alive").withStyle(ChatFormatting.WHITE)), // 生きてる
        SOUL_ENTITY(Component.literal("Soul").withStyle(ChatFormatting.DARK_AQUA)), // ソウルエンティティになってる
        SOUL_WITHIN(Component.literal("Soul Within").withStyle(ChatFormatting.AQUA)); // ソウルになってプレイヤーと共にいる

        private final Component text;

        Status(Component text) {
            this.text = text;
        }

        public Component getText() {
            return text;
        }
    }
}
