package com.riseofthebeasts;

import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import static com.riseofthebeasts.BeastState.*;

public class Giant extends PathfinderMob {
    static final EntityDataAccessor<Integer> INDEX = SynchedEntityData.defineId(Giant.class,EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(Giant.class,EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> MOVE = SynchedEntityData.defineId(Giant.class,EntityDataSerializers.INT);
    static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(Giant.class,EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Integer> SPAN = SynchedEntityData.defineId(Giant.class,EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> TANGENT = SynchedEntityData.defineId(Giant.class,EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> REMAINING=SynchedEntityData.defineId(Giant.class,EntityDataSerializers.INT);
    Animal animal;
    Section section=new Section(0,0);
    Giant(EntityType<? extends PathfinderMob> type,Level level) {
        super(type,level); setNoGravity(true); setNoAi(true); noPhysics=true; setPersistenceRequired();
    }
    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(REMAINING,0); entityData.define(TANGENT,0); entityData.define(INDEX,0); entityData.define(PHASE,1); entityData.define(MOVE,0);
        entityData.define(WIDTH,10f); entityData.define(SPAN,16);
    }
    boolean levi() { return getType()==Beasts.LEVI.get(); }
    float entityDataWidth() { return entityData.get(WIDTH); }
    boolean head() { return entityData.get(INDEX)==0; }
    @Override public boolean shouldBeSaved() { return animal==null && super.shouldBeSaved(); }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override public boolean canBeCollidedWith() { return isAlive(); }
    @Override public boolean isPushable() { return false; }
    @Override public void push(Entity e) {}
    @Override public void travel(Vec3 input) {}
    @Override public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level && animal==null && !isRemoved()) {
            bind(BeastState.get(level).create(levi(),position()),section);
        }
        setBoundingBox(box());
    }
    void bind(Animal a,Section key) {
        animal=a; section=key; a.active.put(key,this);
        entityData.set(INDEX,key.index()); sync();
    }
    void sync() {
        var a=animal;
        entityData.set(WIDTH,(float)a.width(section)); entityData.set(SPAN,a.span(section.index()));
        entityData.set(PHASE,a.phase);
        Move shown=a.cinematic() || section.equals(a.striker) || a.move==Move.TRANSFORM || a.move==Move.BURST ? a.move:Move.REST;
        entityData.set(MOVE,shown.ordinal());
        entityData.set(REMAINING,shown==Move.REST?0:a.remaining);
        Vec3 center=a.position(section);
        if(!head()) {
            double start=section.index()*16-8;
            Vec3 front=a.point(start,section.lane()),back=a.point(start+a.span(section.index()),section.lane());
            Vec3 axis=back.subtract(front);
            int yaw=(int)Math.round(Math.toDegrees(Math.atan2(axis.x,axis.z)));
            int pitch=(int)Math.round(-Math.toDegrees(Math.atan2(axis.y,axis.horizontalDistance())));
            entityData.set(TANGENT,(yaw & 255) | ((pitch & 255)<<8));
            center=front.add(back).scale(.5);
        }
        setPos(center); setBoundingBox(box());
    }
    private AABB box() {
        double w=entityDataWidth();
        if(head()) {
            return new AABB(getX()-w*.53,getY()-(levi()?5:0),getZ()-(levi()?31:26),
                            getX()+w*.53,getY()+(levi()?33:Animal.CROWN),getZ()+(levi()?23:25));
        }
        double dz=entityData.get(SPAN)*.5;
        double dx=dz*Math.abs(Math.tan(Math.toRadians(yawDegrees())));
        double dy=Math.hypot(dx,dz)*Math.abs(Math.tan(Math.toRadians(pitchDegrees())));
        return new AABB(getX()-dx-w*.52,getY()-dy-w*.1,getZ()-dz-1,
                        getX()+dx+w*.52,getY()+dy+w*(levi()?.9:1.2),getZ()+dz+1);
    }
    @Override public boolean shouldRenderAtSqrDistance(double distance) {return distance<320*320;}
    int yawDegrees(){return (byte)(int)entityData.get(TANGENT);}
    int pitchDegrees(){return (byte)(entityData.get(TANGENT)>>8);}
    @Override public AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(3); }
    @Override public boolean hurt(DamageSource source,float amount) {
        if (!(level() instanceof ServerLevel level) || animal==null || amount<=0 || animal.health<=0 ||
            isInvulnerableTo(source) || (source.getEntity()==null && !source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL))) return false;
        if (animal.cinematic() || animal.move==Move.TRANSFORM)return false;
        if (animal.guard>0 && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_COOLDOWN)) return false;
        animal.guard=5; Encounters.damage(level,this,amount); return true;
    }
    @Override public void kill() {
        if(level() instanceof ServerLevel level && animal!=null) Encounters.damage(level,this,animal.health);
        else super.kill();
    }
}
