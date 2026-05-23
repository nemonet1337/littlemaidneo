package work.nemonet.littlemaidneo.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import work.nemonet.littlemaidneo.tags.LMTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CandleCakeBlock.class)
public abstract class MixinCandleCakeBlock {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onUseInjection(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player,
            InteractionHand hand,
            BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack itemStack = player.getItemInHand(hand);
        // 着火するときを取得できなさそうだったので、手動で判定
        // クライアントでは動かない
        if ((itemStack.getItem() instanceof FlintAndSteelItem
                || itemStack.getItem() instanceof FireChargeItem
                || itemStack.is(ItemTags.CREEPER_IGNITERS))
                && CandleCakeBlock.canLight(state)
                && LMRB$getAroundAlterComponentBlocks(world, pos) >= 4
                && world instanceof ServerLevel serverWorld) {
            if (LittleMaidEntity.resurrectionMaid(serverWorld, pos, player)) {
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
        }
    }

    @Unique
    private static int LMRB$getAroundAlterComponentBlocks(Level world, BlockPos center) {
        int num = 0;
        for (int i = 0; i < 9; i++) {
            if (i == 4) {
                continue;
            }
            var blockState = world.getBlockState(center.offset((i % 3) - 1, 0, (i / 3) - 1));
            if (blockState.is(LMTags.Blocks.MAID_ALTER_COMPONENT_BLOCKS)) {
                num++;
            }
        }
        return num;
    }
}
