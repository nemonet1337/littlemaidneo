package work.nemonet.littlemaidneo.entity.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.entity.MaidSoulEntity;
import work.nemonet.littlemaidneo.entity.soul.MaidSoulData;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaidManager {
    void registerMaid(LittleMaidEntity maid);

    void registerMaid(MaidSoulEntity soul);

    void registerMaid(MaidSoulData soul);

    List<LMInfo> getMaidList();

    boolean setGroup(UUID id, String group);

    void writeMaidManager(ValueOutput output);

    void readMaidManager(ValueInput input);

    List<MaidSoulData> getMaidSouls();

    void clearMaidSouls();

    void checkMaidUnload();

    abstract sealed class LMInfo permits MaidLMInfo, SoulEntityLMInfo, SoulLMInfo {
        protected final UUID id;
        protected final String name;
        protected final Status status;
        protected final BlockPos lastPos;
        protected final String worldId;
        protected String group = "";

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

        public String group() {
            return group == null ? "" : group;
        }

        public void setGroup(String group) {
            this.group = sanitizeGroup(group);
        }

        public void write(ValueOutput output) {
            output.putString("name", name);
            output.putString("status", status.name());
            output.store("id", UUIDUtil.CODEC, id);
            output.putIntArray("lastPos", new int[]{lastPos.getX(), lastPos.getY(), lastPos.getZ()});
            output.putString("worldId", worldId);
            if (!group().isEmpty()) {
                output.putString("group", group());
            }
            var entityId = getEntityId();
            if (entityId != -1) {
                output.putInt("entityId", entityId);
            }
        }

        public static LMInfo read(ValueInput input) {
            String name = input.getStringOr("name", "");
            Status status;
            try {
                status = Status.valueOf(input.getStringOr("status", Status.ALIVE.name()));
            } catch (IllegalArgumentException e) {
                status = Status.ALIVE;
            }
            UUID id = input.read("id", UUIDUtil.CODEC).orElse(null);
            if (id == null) return null;
            BlockPos lastPos = input.getIntArray("lastPos")
                    .filter(a -> a.length >= 3)
                    .map(a -> new BlockPos(a[0], a[1], a[2]))
                    .orElse(BlockPos.ZERO);
            String worldId = input.getStringOr("worldId", "");
            int entityId = input.getIntOr("entityId", -1);
            String group = input.getStringOr("group", "");

            LMInfo info;
            if (status == Status.ALIVE) {
                info = new MaidLMInfo(id, name, lastPos, worldId, null, entityId);
            } else if (status == Status.SOUL_ENTITY) {
                var soulNbt = input.read("soul", CompoundTag.CODEC).orElse(null);
                var soul = soulNbt != null ? MaidSoulData.fromNbt(soulNbt) : null;
                info = new SoulEntityLMInfo(id, name, lastPos, worldId, null, soul, entityId);
            } else {
                var soulNbt = input.read("soul", CompoundTag.CODEC).orElse(null);
                var soul = soulNbt != null ? MaidSoulData.fromNbt(soulNbt) : null;
                info = new SoulLMInfo(id, name, soul);
            }
            info.setGroup(group);
            return info;
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
                    maid.level().dimension().identifier().toString(),
                    loaded ? maid : null, loaded ? maid.getId() : -1);
        }

        /** 参照が失われた／アンロード時用。entityId を落として再ログイン後の stale ID を防ぐ。 */
        public static MaidLMInfo unloaded(MaidLMInfo info) {
            BlockPos pos = info.maid != null ? info.maid.blockPosition() : info.lastPos;
            return new MaidLMInfo(info.id, info.name, pos, info.worldId, null, -1);
        }

        @Override
        public Optional<Entity> getEntity() {
            if (maid != null && maid.isAlive()) {
                return Optional.of(maid);
            }
            return Optional.empty();
        }

        @Override
        public boolean isLoaded() {
            // entityId だけではセッション跨ぎで stale になるため、実体参照があるときだけ loaded
            return this.maid != null && this.maid.isAlive();
        }

        @Override
        public int getEntityId() {
            return this.entityId;
        }
    }

    final class SoulEntityLMInfo extends LMInfo {
        private final @Nullable MaidSoulEntity soulEntity;
        private final MaidSoulData soul;
        private final int entityId;

        private SoulEntityLMInfo(UUID id, String name, BlockPos lastPos, String worldId,
                                 @Nullable MaidSoulEntity soulEntity, MaidSoulData soul, int entityId) {
            super(id, name, Status.SOUL_ENTITY, lastPos, worldId);
            this.soulEntity = soulEntity;
            this.soul = soul;
            this.entityId = entityId;
        }

        public @Nullable MaidSoulEntity soulEntity() {
            return soulEntity;
        }

        public MaidSoulData getSoul() {
            return soul;
        }

        public static SoulEntityLMInfo create(MaidSoulEntity soul, boolean loaded) {
            return new SoulEntityLMInfo(soul.getSoul().getUuid(), soul.getSoul().getName(), soul.blockPosition(),
                    soul.level().dimension().identifier().toString(),
                    loaded ? soul : null, soul.getSoul(), loaded ? soul.getId() : -1);
        }

        public static SoulEntityLMInfo unloaded(SoulEntityLMInfo info) {
            BlockPos pos = info.soulEntity != null ? info.soulEntity.blockPosition() : info.lastPos;
            return new SoulEntityLMInfo(info.id, info.name, pos, info.worldId, null, info.soul, -1);
        }

        @Override
        public void write(ValueOutput output) {
            super.write(output);
            output.store("soul", CompoundTag.CODEC, soul.getNbt());
        }

        @Override
        public Optional<Entity> getEntity() {
            if (soulEntity != null && soulEntity.isAlive()) {
                return Optional.of(soulEntity);
            }
            return Optional.empty();
        }

        @Override
        public boolean isLoaded() {
            return this.soulEntity != null && this.soulEntity.isAlive();
        }

        @Override
        public int getEntityId() {
            return this.entityId;
        }
    }

    final class SoulLMInfo extends LMInfo {
        private final MaidSoulData soul;

        private SoulLMInfo(UUID id, String name, MaidSoulData soul) {
            super(id, name, Status.SOUL_WITHIN, BlockPos.ZERO, "");
            this.soul = soul;
        }

        public static SoulLMInfo create(MaidSoulData soul) {
            return new SoulLMInfo(soul.getUuid(), soul.getName(), soul);
        }

        public MaidSoulData soul() {
            return soul;
        }

        @Override
        public void write(ValueOutput output) {
            super.write(output);
            if (this.soul != null) {
                output.store("soul", CompoundTag.CODEC, soul.getNbt());
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

    static String sanitizeGroup(String group) {
        if (group == null) {
            return "";
        }
        String trimmed = group.trim();
        if (trimmed.isEmpty()
                || trimmed.equals("-")
                || trimmed.equalsIgnoreCase("none")
                || trimmed.equalsIgnoreCase("clear")) {
            return "";
        }
        if (trimmed.length() > 32) {
            trimmed = trimmed.substring(0, 32);
        }
        return trimmed.replace('\n', ' ').replace('\r', ' ');
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
