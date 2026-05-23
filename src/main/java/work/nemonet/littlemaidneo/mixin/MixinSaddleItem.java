package work.nemonet.littlemaidneo.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SaddleItem;
import net.minecraft.world.level.Level;
import work.nemonet.littlemaidneo.entity.LittleMaidEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SaddleItem.class)
public class MixinSaddleItem extends Item {

    public MixinSaddleItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        if (user.getPassengers().stream().anyMatch(e -> e instanceof LittleMaidEntity)) {
            user.ejectPassengers();
        }
        return super.use(world, user, hand);
    }
}
