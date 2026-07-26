package work.nemonet.littlemaidneo.entity.targeting;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * ターゲット識別子。
 * この識別子が示すターゲットのEntityTypeが必ず存在することを保証する。
 * ゲームの起動中に作成されるとEntityTypeが存在しない判定になるかもしれない。
 */
public class TargetIdentifier {
    private final String id;

    public TargetIdentifier(Entity entity) {
        this(entity.getType());
    }

    public TargetIdentifier(EntityType<?> entityType) {
        this.id = EntityType.getKey(entityType).toString();
    }

    /**
     * 文字列からTargetIdentifierをパースする。
     * EntityTypeが実在するものだけインスタンス化できる。
     */
    public static Optional<TargetIdentifier> tryParse(String id) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(id))
                .map(TargetIdentifier::new);
    }

    public EntityType<?> getEntityType() {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(this.id)).orElseThrow();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TargetIdentifier that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return this.id;
    }
}
