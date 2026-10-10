package work.nemonet.littlemaidneo.advancement.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import work.nemonet.littlemaidneo.LittleMaidNeo;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;

import java.util.Optional;

public class ContractMaidCriterion extends SimpleCriterionTrigger<ContractMaidCriterion.TriggerInstance> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(LittleMaidNeo.MODID, "contract_maid");

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, LittleMaidEntity entity) {
        this.trigger(player, instance -> instance.matches(player, entity));
    }

    public record TriggerInstance(Optional<Holder<LootItemCondition>> player, Optional<Holder<LootItemCondition>> entity)
            implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                        LootItemCondition.CODEC.optionalFieldOf("entity").forGetter(TriggerInstance::entity))
                        .apply(instance, TriggerInstance::new));

        public boolean matches(ServerPlayer player, LittleMaidEntity entity) {
            return this.entity.isEmpty() || this.entity.get().value().test(EntityPredicate.createContext(player, entity));
        }
    }
}
