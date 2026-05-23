package work.nemonet.littlemaidneo.entity;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.resource.util.LMSounds;
import work.nemonet.littlemaidneo.LMRBMod;
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
    protected void postReceive() {
        super.postReceive();
        var maid = this.mob;
        maid.swing(InteractionHand.MAIN_HAND);
        maid.playSound(SoundEvents.ITEM_PICKUP,
                1.0F, maid.getRandom().nextFloat() * 0.1F + 1.0F);
        maid.play(LMSounds.EAT_SUGAR);
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
                if (this.mob.getCommandSenderWorld() instanceof ServerLevel serverWorld) {
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
    public void readContractable(CompoundTag nbt) {
        super.readContractable(nbt);

        salaryBoxPosSet.clear();
        if (nbt.contains("salaryBoxPosList")) {
            var boxPosList = nbt.getList("salaryBoxPosList", Tag.TAG_COMPOUND);
            boxPosList.forEach(posTag -> {
                if (posTag instanceof CompoundTag posTagCompound) {
                    NbtUtils.readBlockPos(posTagCompound, "pos").ifPresent(salaryBoxPosSet::add);
                }
            });
            checkAndFixSalaryBoxPosSize();
        }
    }

    @Override
    public void writeContractable(CompoundTag nbt) {
        super.writeContractable(nbt);

        if (!salaryBoxPosSet.isEmpty()) {
            checkAndFixSalaryBoxPosSize();
            ListTag salaryBoxPosList = new ListTag();
            for (BlockPos pos : salaryBoxPosSet) {
                var posTag = NbtUtils.writeBlockPos(pos);
                salaryBoxPosList.add(posTag);
            }
            nbt.put("salaryBoxPosList", salaryBoxPosList);
        }
    }

    protected int getMaxMemorySalaryBoxPos() {
        return LMRBMod.getConfig().contract.maxMemorySalaryBoxPos;
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
