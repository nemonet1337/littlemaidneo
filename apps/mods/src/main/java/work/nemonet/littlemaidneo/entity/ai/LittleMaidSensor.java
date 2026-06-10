package work.nemonet.littlemaidneo.entity.ai;

import com.google.common.collect.ImmutableSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Unit;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.setup.ModRegistration;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;

import java.util.Optional;
import java.util.Set;

public class LittleMaidSensor extends Sensor<LittleMaidEntity> {
    public LittleMaidSensor() {
        super(20); // 1秒(20ticks)ごとにスキャン
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(
                ModRegistration.OWNER.get(),
                ModRegistration.IS_WAITING.get()
        );
    }

    @Override
    protected void doTick(ServerLevel level, LittleMaidEntity entity) {
        var brain = entity.getBrain();

        // 待機状態の書き込み
        if (TameableUtil.isWait(entity)) {
            brain.setMemory(ModRegistration.IS_WAITING.get(), Unit.INSTANCE);
        } else {
            brain.eraseMemory(ModRegistration.IS_WAITING.get());
        }

        // 主人プレイヤーの検知と書き込み
        Optional<Player> ownerOpt = TameableUtil.getTameOwner(entity)
                .filter(owner -> owner instanceof Player)
                .map(owner -> (Player) owner);
        
        if (ownerOpt.isPresent()) {
            brain.setMemory(ModRegistration.OWNER.get(), ownerOpt.get());
        } else {
            brain.eraseMemory(ModRegistration.OWNER.get());
        }
    }
}
