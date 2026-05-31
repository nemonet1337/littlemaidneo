package work.nemonet.littlemaidneo.entity;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.resource.util.LMSounds;
import work.nemonet.littlemaidneo.config.LMRBConfig;
import work.nemonet.littlemaidneo.entity.util.MovingMode;
import work.nemonet.littlemaidneo.entity.util.SalaryBoxPosListener;
import work.nemonet.littlemaidneo.entity.util.TameableUtil;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class LMItemContractable<T extends LittleMaidEntity> extends ItemContractable<LittleMaidEntity>
        implements SalaryBoxPosListener {
    protected final ObjectArraySet<BlockPos> salaryBoxPosSet = new ObjectArraySet<>(getMaxMemorySalaryBoxPos());

    public LMItemContractable(T mob, Supplier<Integer> maxConsumeInterval, Supplier<Integer> maxUnpaidTimes,
            Predicate<ItemStack> salaryItems) {
        super(mob, maxConsumeInterval, maxUnpaidTimes, salaryItems);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.mob.level().isClientSide()) {
            this.mob.setContractTime(this.consumeInterval);
        }
    }

    @Override
    protected void postReceive() {
        super.postReceive();
        var maid = this.mob;
        maid.swing(InteractionHand.MAIN_HAND);
        maid.playSound(SoundEvents.ITEM_PICKUP,
                1.0F, maid.getRandom().nextFloat() * 0.1F + 1.0F);
        maid.play(LMSounds.EAT_SUGAR);
        if (!maid.level().isClientSide()) {
            maid.setContractTime(this.consumeInterval);
        }
    }

    @Override
    protected void onStrike() {
        super.onStrike();

        mob.setStrike(true);
        TameableUtil.setWait(mob, false);
        if (mob.getMovingMode() != MovingMode.FREEDOM) {
            mob.setMovingMode(MovingMode.FREEDOM);
            mob.setFreedomPos(mob.blockPosition());
        }
    }

    @Override
    public void listenSalaryBoxPos(BlockPos pos) {
        if (!TameableUtil.hasTameOwner(this.mob)
                || salaryBoxPosSet.contains(pos)) {
            return;
        }
        if (salaryBoxPosSet.size() < getMaxMemorySalaryBoxPos()) {
            salaryBoxPosSet.add(pos);
        } else {
            checkAndFixSalaryBoxPosSize();
            var farthestPos = salaryBoxPosSet.stream()
                    .max(this::compareDistance)
                    .orElseThrow();
            if (pos.distToCenterSqr(mob.position()) < farthestPos.distToCenterSqr(mob.position())) {
                salaryBoxPosSet.remove(farthestPos);
                salaryBoxPosSet.add(pos);
                if (this.mob.level() instanceof ServerLevel serverWorld) {
                    var particlePos = pos.getCenter();

                    serverWorld.sendParticles(ParticleTypes.FIREWORK,
                            particlePos.x, particlePos.y, particlePos.z,
                            10, 0, 0, 0, 0.2);
                }
            }
        }
    }

    public List<BlockPos> getSalaryBoxPositions() {
        return new ObjectArrayList<>(salaryBoxPosSet);
    }

    public boolean hasSalaryBoxPositions() {
        return !salaryBoxPosSet.isEmpty();
    }

    public void setSalaryBoxPositions(Collection<BlockPos> salaryBoxPosSet) {
        this.salaryBoxPosSet.clear();
        this.salaryBoxPosSet.addAll(salaryBoxPosSet);
        checkAndFixSalaryBoxPosSize();
    }

    @Override
    public void readContractable(ValueInput input) {
        super.readContractable(input);

        salaryBoxPosSet.clear();
        for (var entry : input.childrenListOrEmpty("salaryBoxPosList")) {
            entry.read("pos", BlockPos.CODEC).ifPresent(salaryBoxPosSet::add);
        }
        if (!salaryBoxPosSet.isEmpty()) {
            checkAndFixSalaryBoxPosSize();
        }
    }

    @Override
    public void writeContractable(ValueOutput output) {
        super.writeContractable(output);

        if (!salaryBoxPosSet.isEmpty()) {
            checkAndFixSalaryBoxPosSize();
            var list = output.childrenList("salaryBoxPosList");
            for (BlockPos pos : salaryBoxPosSet) {
                list.addChild().store("pos", BlockPos.CODEC, pos);
            }
        }
    }

    protected int getMaxMemorySalaryBoxPos() {
        return LMRBConfig.get().contract.maxMemorySalaryBoxPos;
    }

    protected void checkAndFixSalaryBoxPosSize() {
        if (salaryBoxPosSet.size() > getMaxMemorySalaryBoxPos()) {
            var tmp = salaryBoxPosSet.stream()
                    .sorted(this::compareDistance)
                    .limit(getMaxMemorySalaryBoxPos())
                    .toList();
            salaryBoxPosSet.clear();
            salaryBoxPosSet.addAll(tmp);
        }
    }

    protected int compareDistance(BlockPos pos1, BlockPos pos2) {
        return Double.compare(
                pos1.distToCenterSqr(mob.position()),
                pos2.distToCenterSqr(mob.position()));
    }
}
