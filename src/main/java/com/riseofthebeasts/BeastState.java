package com.riseofthebeasts;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

/** One authoritative creature per record. Physical sections only exist around observers. */
final class BeastState extends SavedData {
    final Map<UUID, Animal> animals = new LinkedHashMap<>();
    final Set<Long> discovered = new HashSet<>();
    static BeastState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(BeastState::load,BeastState::new,"riseofthebeasts_encounters");
    }
    Animal create(boolean levi, Vec3 origin) {
        Animal animal = new Animal(UUID.randomUUID(),levi,origin);
        animal.move=Move.INTRO; animal.remaining=Animal.INTRO_TICKS;
        animals.put(animal.id,animal); setDirty(); return animal;
    }
    private static BeastState load(CompoundTag tag) {
        var data = new BeastState();
        for (var entry : tag.getList("Animals",Tag.TAG_COMPOUND)) {
            var a=Animal.load((CompoundTag)entry); data.animals.put(a.id,a);
        }
        for (long center : tag.getLongArray("Discovered")) data.discovered.add(center);
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag animalsTag=new ListTag();
        animals.values().forEach(a -> animalsTag.add(a.save())); tag.put("Animals",animalsTag);
        tag.putLongArray("Discovered",discovered.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }

    enum Move { REST, BITE, GRAB, CHARGE, BLAST, SPIKE, SLASH, STONE, FIRE, POISON, BURST, MYSTERY, TRANSFORM, SCREECH, INTRO, DEATH }
    record Section(int index, int lane) {}

    static final class Animal {
        static final int INTRO_TICKS=100, DEATH_TICKS=90;
        static final double REARED_HEIGHT=200, CROWN=36;
        final UUID id;
        final Vec3 origin;
        final boolean levi;
        final ServerBossEvent bar;
        final Map<Section,Giant> active=new HashMap<>();
        float health;
        int phase=1, dryTicks, clock, cooldown=40, remaining, burst, guard;
        Section striker=new Section(0,0);
        UUID target;
        Move move=Move.REST;
        Animal(UUID id, boolean levi, Vec3 origin) {
            this.id=id; this.levi=levi; this.origin=origin;
            health=maxHealth();
            bar=new ServerBossEvent(Component.translatable(levi?"entity.riseofthebeasts.leviathan":"boss.riseofthebeasts.jormungandr"),levi?BossEvent.BossBarColor.BLUE:BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
        }
        float maxHealth() { return levi?600:40800; }
        int length() { return levi?1200:phase==1?1800:2560; }
        int laneLength(int lane) { return levi ? length()-Math.abs(lane)*112 : length(); }
        int span(int index) { return Math.min(16,length()-index*16); }
        double width(Section s) {
            if (s.index==0) return levi?44:33;
            double fraction=s.index*16.0/laneLength(s.lane);
            return levi?Math.max(1.25,8.5*(1-fraction*fraction)):Math.max(2.4,29*(1-fraction*fraction));
        }
        boolean cinematic() { return move==Move.INTRO || move==Move.DEATH; }
        static double ease(double x) {
            x=Math.max(0,Math.min(1,x)); return x*x*(3-2*x);
        }
        double sceneProgress() {
            int duration=move==Move.DEATH?DEATH_TICKS:INTRO_TICKS;
            return 1-Math.min(1,(double)remaining/duration);
        }
        Vec3 position(Section s) {
            return point(s.index*16 + span(s.index)*.5-8,s.lane);
        }
        double lift() {
            if(move==Move.INTRO)return ease(sceneProgress()/.83);
            if(move==Move.DEATH)return deathLift*(1-ease(sceneProgress()));
            // The surfacing cycle has a high and a low plateau. Mouth attacks can
            // only be selected when an actual tracked jaw is within range.
            return ease((Math.cos((clock-INTRO_TICKS)*2*Math.PI/680)+.25)*1.4);
        }
        double deathLift;
        double recoil;
        double lungeReach() { return move==Move.CHARGE?18:move==Move.BITE?7:0; }
        Vec3 point(double along,int lane) {
            double wave=clock*.024+along*.023;
            double glide=Math.sin(clock*.018)*2.5;
            if(levi) {
                double t=sceneProgress();
                double fan=move==Move.INTRO?.25+.75*ease(t):move==Move.DEATH?1-.76*ease(t):1;
                double beat=1-remaining/24.0;
                double forward=lungeReach()*(ease((beat-.35)/.55)-.18*Math.sin(Math.PI*ease(beat/.35)))+recoil;
                double thrust=forward*Math.exp(-Math.max(0,along)/48);
                double sink=move==Move.DEATH?-9*ease(t):move==Move.INTRO?-6*(1-ease(t)):0;
                Vec3 head=origin.add(Math.sin(clock*.021)*1.4,sink,glide-thrust);
                if(along<8)return head.add(0,0,along);
                // Eight roots lie on an ellipse at the rear of the skull. Each
                // arm uses the same continuous curve on both sides of a seam.
                double angle=2*Math.PI*(lane+4)/8.0+.22;
                double arm=along-8,spread=1-Math.exp(-arm/75);
                double reach=(38+65*spread)*fan;
                double bend=spread*Math.sin(wave+angle)*10;
                if(move==Move.GRAB && lane==striker.lane) {
                    double local=Math.exp(-Math.pow((along-striker.index*16)/30,2));
                    bend+=local*13*Math.sin(Math.PI*ease((24-remaining)/24.0));
                }
                return head.add(Math.cos(angle)*(13+spread*reach)+bend,
                    12+Math.sin(angle)*(7+spread*5*fan)+spread*4*Math.sin(wave*.79+angle),along);
            }
            double arch=1-ease((along-8)/210);
            double height=(REARED_HEIGHT-CROWN)*lift()*arch;
            double ripple=move==Move.DEATH?ease(sceneProgress())*(30+6*Math.sin(along*.03+sceneProgress()*7)):0;
            return origin.add(Math.sin(wave*.55)*(2+5*(1-arch)),height-ripple,
                    along+glide);
        }
        CompoundTag save() {
            var n=new CompoundTag(); n.putUUID("Id",id); n.putBoolean("Aquatic",levi);
            n.putDouble("X",origin.x); n.putDouble("Y",origin.y); n.putDouble("Z",origin.z);
            n.putDouble("DeathLift",deathLift); n.putFloat("Health",health); n.putInt("Phase",phase); n.putInt("Stranded",dryTicks);
            n.putDouble("Recoil",recoil);
            n.putInt("Clock",clock); n.putInt("Cooldown",cooldown); n.putInt("Burst",burst);
            n.putString("Move",move.name()); n.putInt("Remaining",remaining); n.putInt("Index",striker.index);
            n.putInt("Lane",striker.lane); if(target!=null)n.putUUID("Target",target);
            return n;
        }
        static Animal load(CompoundTag n) {
            Animal a=new Animal(n.getUUID("Id"),n.getBoolean("Aquatic"),new Vec3(n.getDouble("X"),n.getDouble("Y"),n.getDouble("Z")));
            a.deathLift=n.getDouble("DeathLift"); a.health=n.getFloat("Health"); a.phase=n.getInt("Phase");
            a.recoil=n.getDouble("Recoil");
            if(a.phase==2)a.bar.setColor(BossEvent.BossBarColor.RED); a.dryTicks=n.getInt("Stranded");
            a.clock=n.getInt("Clock"); a.cooldown=n.getInt("Cooldown"); a.burst=n.getInt("Burst");
            a.move=Move.valueOf(n.getString("Move")); a.remaining=n.getInt("Remaining");
            a.striker=new Section(n.getInt("Index"),n.getInt("Lane")); if(n.hasUUID("Target"))a.target=n.getUUID("Target");
            return a;
        }
    }
}
