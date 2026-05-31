package work.nemonet.littlemaidneo.entity.mode;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import work.nemonet.littlemaidneo.util.Tuple;
import work.nemonet.littlemaidneo.api.mode.ItemMatcher;
import work.nemonet.littlemaidneo.api.mode.Mode;
import work.nemonet.littlemaidneo.api.mode.ModeManager;
import work.nemonet.littlemaidneo.entity.util.HasInventory;

import java.util.*;
import java.util.function.Consumer;

/**
 * HasModeの移譲用クラス
 */
public class HasModeImpl implements HasMode {
    private final LivingEntity owner;
    private final HasInventory hasInventory;
    private final Set<Mode> modes = Sets.newHashSet();
    private final List<Tuple<ItemMatcher, Mode>> itemMatchers = new ObjectArrayList<>();
    private final Consumer<Mode> onModeChange;
    private Mode nowMode;

    public HasModeImpl(LivingEntity owner, HasInventory hasInventory, Set<Mode> modes, Consumer<Mode> onModeChange) {
        this.owner = owner;
        this.hasInventory = hasInventory;
        this.onModeChange = onModeChange;
        this.modes.addAll(modes);
        updateMatchList();
    }

    protected void updateMatchList() {
        this.itemMatchers.clear();
        this.modes.stream()
                .flatMap(mode -> mode.getModeType().getItemMatcherList().stream()
                        .map(tuple -> new Tuple<>(mode, tuple)))
                .sorted(Comparator.<Tuple<Mode, Tuple<ItemMatcher.Priority, ItemMatcher>>>
                                comparingInt(tuple -> tuple.b().a().get())
                        .reversed())
                .forEach(tuple -> this.itemMatchers.add(new Tuple<>(tuple.b().b(), tuple.a())));
    }

    public void addMode(Mode mode) {
        modes.add(mode);
        updateMatchList();
    }

    public void addAllMode(Collection<Mode> mode) {
        modes.addAll(mode);
        updateMatchList();
    }

    @Override
    public Optional<Mode> getMode() {
        return Optional.ofNullable(this.nowMode);
    }

    @Override
    public void writeModeData(ValueOutput output) {
        if (this.nowMode != null) {
            ModeManager.INSTANCE.getId(nowMode)
                    .ifPresent(identifier -> {
                        output.putString("ModeID", identifier.toString());
                        CompoundTag modeData = new CompoundTag();
                        nowMode.writeModeData(modeData);
                        output.store("ModeData", CompoundTag.CODEC, modeData);
                    });
        }
    }

    @Override
    public void readModeData(ValueInput input) {
        input.getString("ModeID").ifPresent(modeIDStr -> {
            var modeID = Identifier.tryParse(modeIDStr);
            if (modeID != null) {
                input.read("ModeData", CompoundTag.CODEC).ifPresent(modeData -> {
                    ModeManager.INSTANCE.getType(modeID)
                            .flatMap(modeType -> modes.stream()
                                    .filter(mode -> mode.getModeType() == modeType)
                                    .findFirst())
                            .ifPresent(mode -> {
                                mode.readModeData(modeData);
                                nowMode = mode;
                                onModeChange.accept(mode);
                            });
                });
            }
        });
    }

    public void tick() {
        // モード無しなら新たな
        if (nowMode == null) {
            getNewMode().ifPresent(this::changeNewMode);
            return;
        }
        if (!isModeContinue()) {
            // 手持ちアイテムに現在のモードで適用できるかチェック
            var index = getNowModeItemIndex();
            if (index == -1) {
                // モード続行不可
                nowMode.resetTask();
                nowMode.endModeTask();
                nowMode = null;
                onModeChange.accept(null);
                // 新たなモードに切り替え
                getNewMode().ifPresent(this::changeNewMode);
            } else {
                // モードアイテムがあるならメインハンドと入れ替え
                switchMainHandItem(index);
            }
        }
    }

    // 現在のモードのモードアイテムがインベントリにあるならTrue
    public int getNowModeItemIndex() {
        if (nowMode == null) return -1;

        var inv = hasInventory.getInventory();
        for (int index = 0; index < inv.getContainerSize(); index++) {
            var stack = inv.getItem(index);
            if (nowMode.getModeType().isModeItem(stack)) {
                return index;
            }
        }
        return -1;
    }

    // メインハンドとインベントリのアイテムを入れ替える
    public void switchMainHandItem(int index) {
        var inv = hasInventory.getInventory();
        ItemStack invStack = inv.getItem(index);
        var tmp = owner.getMainHandItem();

        owner.setItemInHand(InteractionHand.MAIN_HAND, invStack);
        inv.setItem(index, tmp);
    }

    // モードを継続するか
    // 所持アイテムが現在のモードを有効にするならTrue
    public boolean isModeContinue() {
        if (nowMode == null) return false;
        var stack = owner.getMainHandItem();
        return nowMode.getModeType().isModeItem(stack);
    }

    // モードを切り替える
    public void changeNewMode(Mode mode) {
        if (nowMode != null) {
            nowMode.resetTask();
            nowMode.endModeTask();
        }
        mode.startModeTask();
        nowMode = mode;
        onModeChange.accept(mode);
    }

    // 現在メインハンドにあるアイテムが有効にするモードを返す
    public Optional<Mode> getNewMode() {
        var mainHand = owner.getMainHandItem();
        if (mainHand.isEmpty()) {
            return Optional.empty();
        }
        for (Tuple<ItemMatcher, Mode> tuple : this.itemMatchers) {
            if (tuple.a().isMatch(mainHand)) {
                return Optional.of(tuple.b());
            }
        }
        return Optional.empty();
    }
}
