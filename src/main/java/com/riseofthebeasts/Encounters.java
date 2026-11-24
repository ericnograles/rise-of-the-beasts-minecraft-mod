package com.riseofthebeasts;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import static com.riseofthebeasts.BeastState.*;

public final class Encounters {
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        BeastState data=BeastState.get(level);
        if(level.dimension()==Level.OVERWORLD && level.getGameTime()%40==0) discoverCavern(level,data);
        for(Animal a:List.copyOf(data.animals.values())) {
            if(level.getGameTime()%8==0) sections(level,a);
            if(a.active.isEmpty() && a.move!=Move.DEATH) continue;
            if(a.cinematic()) {
                if(a.remaining==(a.move==Move.INTRO?Animal.INTRO_TICKS:Animal.DEATH_TICKS))sceneSound(a);
                if(a.move==Move.DEATH && a.remaining==1) {
                    a.bar.removeAllPlayers(); a.active.values().forEach(Giant::discard);
                    a.active.clear(); data.animals.remove(a.id); data.setDirty(); continue;
                }
                if(--a.remaining==0){a.move=Move.REST;a.cooldown=30;}
            }
            a.clock+=a.burst>0?4:1;
            if(a.burst>0)a.burst--;
            if(a.guard>0)a.guard--;
            a.recoil=a.recoil>.04?a.recoil*.8:0;
            if(a.move!=Move.DEATH && tickStranding(level,a))continue;
            a.active.values().removeIf(Giant::isRemoved);
            for(Giant g:a.active.values())g.sync();
            if(a.cinematic() && a.remaining%8==0) {
                for(Giant g:a.active.values()) {
                    if(!g.head() && g.section.index()%5!=0)continue;
                    var effect=a.levi?ParticleTypes.BUBBLE_COLUMN_UP:a.move==Move.DEATH?ParticleTypes.ASH:ParticleTypes.SPLASH;
                    level.sendParticles(effect,g.getX(),g.getY()+2,g.getZ(),12,3,2,4,.07);
                }
            } else if(!a.cinematic())fight(level,a);
            data.setDirty();
        }
    }
    static boolean tickStranding(ServerLevel level,Animal a) {
        Giant head=a.active.get(new Section(0,0));
        if(a.move==Move.DEATH || !a.levi || head==null || head.isRemoved() || !stranded(a,waterAtHead(level,head)))return false;
        finish(level,head,false);
        return true;
    }
    static boolean waterAtHead(ServerLevel level,Giant g) {
        // Sample the jaw footprint at the waterline, including a one-block-deep pool.
        BlockPos base=g.animal==null?g.blockPosition():BlockPos.containing(g.getX(),g.animal.origin.y,g.getZ());
        for(int dx=-3;dx<=3;dx+=3)for(int dz=-4;dz<=4;dz+=4)
            for(int dy=-1;dy<=1;dy++)
                if(level.hasChunkAt(base.offset(dx,dy,dz)) && level.getFluidState(base.offset(dx,dy,dz)).is(FluidTags.WATER))return true;
        return false;
    }
    static boolean stranded(Animal a,boolean wet) {
        a.dryTicks=wet?0:a.dryTicks+1;
        return a.dryTicks>=1200;
    }
    private static void discoverCavern(ServerLevel level,BeastState data) {
        for(ServerPlayer p:level.players()) {
            if(p.isSpectator() || p.getY()>40)continue;
            BlockPos center=Cavern.centerFor(p.getBlockX(),p.getBlockZ());
            if(Math.abs(p.getX()-center.getX())>48 || Math.abs(p.getZ()-center.getZ())>48 ||
                data.discovered.contains(center.asLong()) || !level.hasChunkAt(center) ||
                !level.getBlockState(center.offset(7,-6,0)).is(net.minecraft.world.level.block.Blocks.SEA_LANTERN) || !Cavern.valid(level,center))continue;
            data.discovered.add(center.asLong());
            data.create(true,new Vec3(center.getX(),-46,center.getZ()-360));
            announce(p,"A row of orange suckers stirs below you. The jaw lies to the north.");
        }
    }
    @SubscribeEvent public static void summon(PlayerInteractEvent.RightClickItem event) {
        if(!(event.getEntity() instanceof ServerPlayer p) || !p.isShiftKeyDown() ||
            !event.getItemStack().is(Items.ECHO_SHARD) || p.level().dimension()!=Level.OVERWORLD ||
            !p.level().getBiome(p.blockPosition()).is(BiomeTags.IS_OCEAN) || Math.abs(p.getY()-p.level().getSeaLevel())>6) return;
        ServerLevel level=p.serverLevel();
        boolean nearby=BeastState.get(level).animals.values().stream().anyMatch(a->!a.levi &&
            Math.abs(p.getX()-a.origin.x)<100 && p.getZ()>=a.origin.z-100 && p.getZ()<=a.origin.z+a.length()+100);
        if(nearby) { announce(p,"The serpent is already listening."); return; }
        BeastState.get(level).create(false,new Vec3(p.getX(),level.getSeaLevel(),p.getZ()+52));
        if(!p.getAbilities().instabuild)event.getItemStack().shrink(1);
        announce(p,"Jormungandr: Who called my name?");
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS); event.setCanceled(true);
    }
    static void sections(ServerLevel level,Animal a) {
        Set<Section> wanted=new HashSet<>();
        List<ServerPlayer> observers=new ArrayList<>();
        for(ServerPlayer player:level.players()) {
            Vec3 p=player.position();
            int sight=a.levi?112:246;
            if(Math.abs(p.x-a.origin.x)>sight+(a.levi?134:24) || p.y<a.origin.y-sight || p.y>a.origin.y+sight+200)continue;
            int around=(int)Math.floor((p.z-a.origin.z)/16);
            boolean visible=false;
            int steps=(sight+40)/16+1;
            for(int i=Math.max(0,around-steps);i<=Math.min((a.length()-1)/16,around+steps);i++) {
                int lanes=a.levi && i>0?4:0;
                for(int lane=-lanes;lane<=(lanes==0?0:lanes-1);lane++) {
                    if(i*16>=a.laneLength(lane))continue;
                    var key=new Section(i,lane); Vec3 pos=a.position(key);
                    if(p.distanceToSqr(pos)>sight*sight || !level.isPositionEntityTicking(BlockPos.containing(pos)))continue;
                    wanted.add(key); visible=true;
                }
            }
            if(visible)observers.add(player);
        }
        for(var it=a.active.entrySet().iterator();it.hasNext();) {
            var entry=it.next();
            if(!wanted.contains(entry.getKey()) || entry.getValue().isRemoved()) { entry.getValue().discard(); it.remove(); }
        }
        for(Section key:wanted) if(!a.active.containsKey(key)) {
            Giant g=(a.levi?Beasts.LEVI:Beasts.SERPENT).get().create(level);
            g.bind(a,key); if(!level.addFreshEntity(g))a.active.remove(key);
        }
        for(var p:List.copyOf(a.bar.getPlayers())) if(!observers.contains(p))a.bar.removePlayer(p);
        for(var p:observers)a.bar.addPlayer(p);
        a.bar.setProgress(a.health/a.maxHealth());
    }
    static void damage(ServerLevel level,Giant part,float amount) {
        Animal a=part.animal;
        if(a.health<=0 || a.cinematic() || a.move==Move.TRANSFORM)return;
        a.health=Math.max(0,a.health-amount);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,part.getX(),part.getY()+2,part.getZ(),5,.6,.3,.6,.1);
        if(a.health==0) {
            if(!a.levi && a.phase==1) {
                Stomach.releaseAll(level,a.id);
                a.phase=2; a.health=a.maxHealth(); a.remaining=50; a.move=Move.TRANSFORM; a.target=null;
                a.bar.setColor(net.minecraft.world.BossEvent.BossBarColor.RED);
                voice(a,"The shell breaks. The fire remembers.");
                for(Giant g:a.active.values()) {
                    g.sync(); level.sendParticles(ParticleTypes.FLAME,g.getX(),g.getY()+4,g.getZ(),32,2,2,3,.1);
                }
                part.playSound(SoundEvents.END_PORTAL_SPAWN,1,.7f);
            } else finish(level,part,!a.levi);
        }
        a.bar.setProgress(a.health/a.maxHealth());
        BeastState.get(level).setDirty();
    }
    private static void finish(ServerLevel level,Giant g,boolean reward) {
        Animal a=g.animal; a.health=0;
        Stomach.releaseAll(level,a.id);
        if(reward) {
            voice(a,"At last, the tide is quiet.");
            g.spawnAtLocation(new ItemStack(Items.NETHER_STAR).setHoverName(Component.literal("Star of the World Serpent")));
        }
        a.deathLift=a.lift(); a.move=Move.DEATH; a.remaining=Animal.DEATH_TICKS; a.target=null;
        a.bar.setProgress(0); a.active.values().forEach(Giant::sync);
        BeastState.get(level).setDirty();
    }
    private static void sceneSound(Animal a) {
        var sound=a.move==Move.DEATH?(a.levi?SoundEvents.GUARDIAN_DEATH:SoundEvents.ENDER_DRAGON_DEATH)
            :(a.levi?SoundEvents.ELDER_GUARDIAN_AMBIENT:SoundEvents.WARDEN_EMERGE);
        for(ServerPlayer p:a.bar.getPlayers())
            p.playNotifySound(sound,SoundSource.HOSTILE,.85f,a.levi?.62f:.78f);
    }
    private static void fight(ServerLevel level,Animal a) {
        if(a.remaining>0) {
            if(--a.remaining==0) {
                Giant g=a.active.get(a.striker);
                Player p=a.target==null?null:level.getPlayerByUUID(a.target);
                if(g!=null && a.move==Move.SCREECH)screech(level,g);
                else if(g!=null && p!=null && valid(p))strike(level,a,g,p);
                if(a.levi)a.recoil=a.lungeReach();
                a.move=Move.REST; a.cooldown=30; a.target=null;
            }
            return;
        }
        if(a.cooldown-->0)return;
        // Pick a local target first, then the nearest physical portion. Distant mouths cannot bite.
        List<ServerPlayer> players=a.bar.getPlayers().stream().filter(Encounters::valid).toList();
        if(players.isEmpty()){a.cooldown=12;return;}
        Player p=players.get(level.random.nextInt(players.size()));
        Giant g=a.active.values().stream().min(Comparator.comparingDouble(part->part.getBoundingBox().distanceToSqr(p.getEyePosition()))).orElse(null);
        if(g==null || g.getBoundingBox().distanceToSqr(p.getEyePosition())>40*40){a.cooldown=10;return;}
        List<Move> choices=new ArrayList<>();
        if(a.levi) {
            if(g.head())Collections.addAll(choices,Move.BITE,Move.CHARGE,Move.SCREECH);
            else choices.add(Move.GRAB);
        } else {
            Collections.addAll(choices,Move.BLAST,Move.SLASH);
            if(g.head())Collections.addAll(choices,Move.BITE,Move.SPIKE);
            if(g.section.index()*16>a.length()*.7)choices.add(Move.STONE);
            if(a.phase==2)Collections.addAll(choices,Move.FIRE,Move.POISON,Move.BURST,Move.MYSTERY);
        }
        Move choice=choices.get(level.random.nextInt(choices.size()));
        if(choice==Move.MYSTERY) {
            voice(a,"Guess my next breath."); choices.remove(Move.MYSTERY); choice=choices.get(level.random.nextInt(choices.size()));
        }
        a.move=choice; a.striker=g.section; a.target=p.getUUID(); a.remaining=choice==Move.SCREECH?34:a.levi?24:20;
        if(!a.levi)voice(a,switch(choice) {
            case BLAST -> "Stand against my breath."; case BITE -> "Into the dark."; case SPIKE -> "Teeth of the deep.";
            case STONE -> "Catch the mountain."; case FIRE -> "Burn."; case POISON -> "Breathe deeply.";
            case BURST -> "Follow, if you can."; default -> "Mind my scales.";
        });
        g.playSound(a.levi?SoundEvents.RAVAGER_ROAR:SoundEvents.ENDER_DRAGON_GROWL,1,.65f);
    }
    private static boolean valid(Player p) { return p.isAlive() && !p.isSpectator() && !p.isCreative() && !Stomach.inside(p); }
    static void strike(ServerLevel level,Animal a,Giant g,Player p) {
        double distance=g.getBoundingBox().distanceToSqr(p.getEyePosition());
        Vec3 from=g.position().add(0,g.head()?4:2,0);
        Vec3 dir=p.position().subtract(from).multiply(1,0,1).normalize();
        switch(a.move) {
            case BITE -> {
                if(distance<=64 && p.hurt(g.damageSources().mobAttack(g),a.levi?6:8) && !a.levi && p instanceof ServerPlayer sp && p.isAlive())Stomach.swallow(sp,a.id);
            }
            case SCREECH -> screech(level,g);
            case GRAB -> { if(distance<=100) { p.hurt(g.damageSources().mobAttack(g),4); p.push(-dir.x*1.1,.1,-dir.z*1.1); p.hurtMarked=true;
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,30,3)); } }
            case CHARGE, SLASH -> { if(distance<=100) { p.hurt(g.damageSources().mobAttack(g),a.levi?6:8); p.push(dir.x,.45,dir.z);p.hurtMarked=true; } }
            case BLAST -> { if(distance<24*24) { p.setDeltaMovement(dir.scale(2.35).add(0,.65,0));p.hurtMarked=true; } }
            case BURST -> a.burst=100;
            case POISON -> { if(distance<24*24)p.addEffect(new MobEffectInstance(Beasts.VENOM.get(),400)); }
            case FIRE, SPIKE, STONE -> BeastShot.launch(level,g,p,a.move);
            default -> {}
        }
    }
    static void screech(ServerLevel level,Giant g) {
        g.playSound(Beasts.SCREECH.get(),3,1);
        level.sendParticles(ParticleTypes.SONIC_BOOM,g.getX(),g.getY()+13,g.getZ()-22,1,0,0,0,0);
        for(Player listener:level.getEntitiesOfClass(Player.class,g.getBoundingBox().inflate(28),Encounters::valid)) {
            if(g.getBoundingBox().distanceToSqr(listener.getEyePosition())>28*28)continue;
            listener.stopUsingItem();listener.setSprinting(false);
            listener.setDeltaMovement(Vec3.ZERO);listener.hurtMarked=true;
            listener.addEffect(new MobEffectInstance(Beasts.STUN.get(),40));
        }
    }
    static void announce(Player p,String s){ p.sendSystemMessage(Component.literal(s)); }
    private static void voice(Animal a,String s) { for(ServerPlayer p:a.bar.getPlayers())announce(p,"Jormungandr: "+s); }
}
