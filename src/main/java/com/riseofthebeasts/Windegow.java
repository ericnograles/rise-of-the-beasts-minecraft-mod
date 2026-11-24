package com.riseofthebeasts;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.Vec3;

public class Windegow extends Monster {
    static final EntityDataAccessor<Integer> WINDUP = SynchedEntityData.defineId(Windegow.class, EntityDataSerializers.INT);
    private final Map<UUID, Vec3> positions = new HashMap<>();
    private final Set<UUID> moving = new HashSet<>();
    private int cooldown;
    Windegow(EntityType<? extends Monster> type, Level level) { super(type, level); setMaxUpStep(1.3f); }
    static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,1000).add(Attributes.MOVEMENT_SPEED,.44)
            .add(Attributes.FOLLOW_RANGE,36).add(Attributes.ATTACK_DAMAGE,1000);
    }
    static boolean canSpawn(EntityType<Windegow> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getLevel().isNight() && level.getBiome(pos).is(BiomeTags.IS_FOREST) &&
            Monster.checkMonsterSpawnRules(type,level,reason,pos,random);
    }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(WINDUP,0); }
    void observe(Player player) {
        UUID id = player.getUUID();
        Vec3 old = positions.put(id,player.position());
        if (old != null && old.distanceToSqr(player.position()) > .0001) moving.add(id);
        else moving.remove(id);
    }
    boolean scratch(Player player) {
        if (!moving.contains(player.getUUID()) || !eligible(player) || distanceToSqr(player)>17 || !hasLineOfSight(player)) return false;
        return player.hurt(damageSources().mobAttack(this),1000);
    }
    private boolean eligible(Player p) { return p.isAlive() && !p.isSpectator() && !p.isCreative(); }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide || !isAlive()) return;
        if (level().isDay()) { kill(); return; }
        var players = level().getEntitiesOfClass(Player.class,getBoundingBox().inflate(36),this::eligible);
        Set<UUID> seen = new HashSet<>();
        for (var player : players) { observe(player); seen.add(player.getUUID()); }
        positions.keySet().retainAll(seen); moving.retainAll(seen);
        int windup = entityData.get(WINDUP);
        if (windup>0) {
            getNavigation().stop();
            if (!(getTarget() instanceof Player victim) || !moving.contains(victim.getUUID()) || !eligible(victim)) {
                entityData.set(WINDUP,0); cooldown=25; return;
            }
            entityData.set(WINDUP,windup-1);
            if (windup==1 && getTarget() instanceof Player p) { scratch(p); cooldown=25; }
            return;
        }
        if (cooldown>0) cooldown--;
        Player target = players.stream().filter(p -> moving.contains(p.getUUID()) && hasLineOfSight(p))
            .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        setTarget(target);
        if (target==null) { getNavigation().stop(); return; }
        getLookControl().setLookAt(target,35,35);
        double distance=distanceToSqr(target);
        if (distance<=16 && cooldown==0) {
            getNavigation().stop();
            entityData.set(WINDUP,18);
            playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK,1.2f,1.65f);
        } else if (tickCount%5==0) {
            getNavigation().moveTo(target,1.2);
            if (distance>16 && distance<144 && onGround() && tickCount%30==0) {
                var leap=target.position().subtract(position()).multiply(1,0,1).normalize().scale(1.1);
                setDeltaMovement(leap.x,.5,leap.z); hasImpulse=true;
            }
        }
    }
}
