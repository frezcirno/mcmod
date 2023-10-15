package io.frezcirno.mymcmod;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class SuperSnowballEntity extends ThrowableItemProjectile {
    private SuperSnowballItem item;

    public SuperSnowballEntity(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public SuperSnowballEntity(Level level, LivingEntity entity, SuperSnowballItem item) {
        super(ModMain.SUPER_SNOWBALL_ENTITY_TYPE.get(), entity, level);
        this.item = item;
    }

    public SuperSnowballEntity(Level level, double yaw, double pitch, double speed) {
        super(ModMain.SUPER_SNOWBALL_ENTITY_TYPE.get(), yaw, pitch, speed, level);
    }

    @Override
    protected Item getDefaultItem() {
        return this.item;
    }

    private ParticleOptions getParticle() {
        ItemStack itemstack = this.getItemRaw();
        return itemstack.isEmpty() ? ParticleTypes.EFFECT : new ItemParticleOption(ParticleTypes.ITEM, itemstack);
    }

    public void handleEntityEvent(byte p_37402_) {
        if (p_37402_ == 3) {
            ParticleOptions particleoptions = this.getParticle();

            for(int i = 0; i < 8; ++i) {
                this.level.addParticle(particleoptions, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            }
        }

    }

    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        Entity entity = hitResult.getEntity();
        entity.hurt(DamageSource.thrown(this, this.getOwner()), 3.0F);
    }

    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level.isClientSide) {
            this.level.broadcastEntityEvent(this, (byte)3);
            boolean flag = net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.level, this.getOwner());
            this.level.explode(null, this.getX(), this.getY(), this.getZ(), (float)this.item.explosionPower, flag, flag ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.NONE);
            this.discard();
        }
    }
}
