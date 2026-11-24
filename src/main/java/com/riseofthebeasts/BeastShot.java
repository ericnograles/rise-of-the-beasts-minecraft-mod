package com.riseofthebeasts;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import static com.riseofthebeasts.BeastState.Move;

public class BeastShot extends ThrowableItemProjectile {
    BeastShot(EntityType<? extends BeastShot> type,Level level){ super(type,level); }
    @Override protected Item getDefaultItem(){return Items.POINTED_DRIPSTONE;}
    boolean stone(){return getItem().is(Items.STONE);}
    boolean fiery(){return getItem().is(Items.FIRE_CHARGE);}
    @Override protected float getGravity(){return stone()?.026f:.003f;}
    @Override protected boolean canHitEntity(Entity entity){return !(entity instanceof Giant) && super.canHitEntity(entity);}
    static void launch(ServerLevel level,Giant part,Player p,Move move){
        int count=move==Move.SPIKE?3:1;
        for(int i=0;i<count;i++) {
            BeastShot shot=new BeastShot(Beasts.SHOT.get(),level); shot.setOwner(part);
            shot.setItem(new ItemStack(move==Move.FIRE?Items.FIRE_CHARGE:move==Move.STONE?Items.STONE:Items.POINTED_DRIPSTONE));
            Vec3 start=part.position().add(0,part.entityDataWidth()*.65+1,move==Move.SPIKE?-6:0);
            shot.setPos(start);
            Vec3 dir=p.getEyePosition().subtract(start).normalize().yRot((i-(count-1)*.5f)*.09f);
            shot.shoot(dir.x,dir.y+(shot.stone()?.13:0),dir.z,shot.stone()?1.1f:1.5f,0);
            level.addFreshEntity(shot);
        }
    }
    @Override public EntityDimensions getDimensions(Pose pose) { return stone()?EntityDimensions.fixed(4,4):super.getDimensions(pose); }
    @Override public void tick(){
        super.tick();
        if(stone())setBoundingBox(new AABB(getX()-2,getY()-2,getZ()-2,getX()+2,getY()+2,getZ()+2));
        if(isRemoved())return;
        if(level() instanceof ServerLevel server) {
            if(stone()) {
                AABB swept=getBoundingBox().expandTowards(getDeltaMovement().reverse());
                for(Player p:level().getEntitiesOfClass(Player.class,swept,this::canHitEntity)){
                    onHit(new EntityHitResult(p));break;
                }
                if(level().getBlockCollisions(this,getBoundingBox()).iterator().hasNext())discard();
            }
            if(fiery())server.sendParticles(ParticleTypes.FLAME,getX(),getY(),getZ(),4,.18,.18,.18,.01);
            if(tickCount>100)discard();
        }
    }
    @Override protected void onHit(HitResult result) {
        if(level().isClientSide)return;
        if(result instanceof EntityHitResult hit && canHitEntity(hit.getEntity()))
            hit.getEntity().hurt(damageSources().thrown(this,getOwner()),fiery()?9:stone()?5:4);
        discard();
    }
}
