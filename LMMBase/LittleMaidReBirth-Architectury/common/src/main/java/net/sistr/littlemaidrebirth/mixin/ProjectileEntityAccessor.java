package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Projectile.class)
public interface ProjectileEntityAccessor {

    @Invoker("ownedBy")
    boolean invokeIsOwner(Entity entity);

}
