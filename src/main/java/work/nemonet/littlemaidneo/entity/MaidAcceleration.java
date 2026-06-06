package work.nemonet.littlemaidneo.entity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * メイドさんの加速状態（Trans-Am等による加速動作、accelerationTicks）を管理するコンポーネント。
 */
public final class MaidAcceleration {
    private final LittleMaidEntity maid;
    private int accelerationTicks;

    public MaidAcceleration(LittleMaidEntity maid) {
        this.maid = maid;
    }

    public int getTickMultiple() {
        return isAcceleration()
                ? LittleMaidEntity.getConfig().misc.accelerationMultiple
                : 1;
    }

    public void setAccelerationTicks(int ticks) {
        this.accelerationTicks = ticks;
        if (ticks > 0) {
            setAccelerateData(true);
        }
    }

    public void decAccelerationTicks() {
        if (this.accelerationTicks > 0) {
            this.accelerationTicks--;
        }
        if (this.accelerationTicks <= 0) {
            this.accelerationTicks = 0;
            setAccelerateData(false);
        }
    }

    public int getAccelerationTicks() {
        return this.accelerationTicks;
    }

    public boolean isAcceleration() {
        return this.maid.isAcceleration_LM();
    }

    private void setAccelerateData(boolean accelerate) {
        this.maid.setAccelerationData_LM(accelerate);
    }

    public void save(ValueOutput output) {
        output.putInt("accelerationTicks", accelerationTicks);
    }

    public void load(ValueInput input) {
        accelerationTicks = input.getIntOr("accelerationTicks", 0);
    }

    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.accelerationTicks);
    }

    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        this.accelerationTicks = buf.readVarInt();
    }
}
