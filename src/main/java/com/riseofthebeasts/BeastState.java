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

    enum Move { REST, BITE, GRAB, CHARGE, BLAST, SPIKE, SLASH, STONE, FIRE, POISON, BURST, MYSTERY, TRANSFORM }
    record Section(int index, int lane) {}

    static final class Animal {
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
            bar=new ServerBossEvent(Component.literal(levi?"Leviathan":"Jormungandr"),levi?BossEvent.BossBarColor.BLUE:BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
        }
        float maxHealth() { return levi?600:40800; }
        int length() { return levi?1200:phase==1?1800:2560; }
        int laneLength(int lane) { return length() - Math.abs(lane)*160; }
        int span(int index) { return Math.min(16,length()-index*16); }
        double width(Section s) {
            if (s.index==0) return levi?12:11;
            double t=(s.index*16.0)/laneLength(s.lane);
            return levi?Math.max(.8,3.6*(1-t)):Math.max(1.4,10*(1-t*t));
        }
        Vec3 position(Section s) {
            double along=s.index*16 + span(s.index)*.5-8;
            return point(along,s.lane);
        }
        Vec3 point(double along,int lane) {
            double t=clock*.026 + along*.035;
            double spread=Math.min(1,along/110.);
            double x=Math.sin(t)*2.8 + (levi?lane*spread*(14+4*Math.sin(t*.5)):0);
            double y=levi?Math.sin(t*.8)*.65 + Math.abs(lane)*spread*.8:Math.sin(t*.76)*.8;
            double glide=Math.sin(clock*.018)*3.5;
            if ((move==Move.BITE || move==Move.CHARGE) && remaining<10 && along<64)
                glide-=Math.sin((10-remaining)*Math.PI/10)*(4-Math.max(0,along)/16)*(move==Move.CHARGE?2.4:1.2);
            return origin.add(x,y,along+glide);
        }
        CompoundTag save() {
            var n=new CompoundTag(); n.putUUID("Id",id); n.putBoolean("Aquatic",levi);
            n.putDouble("X",origin.x); n.putDouble("Y",origin.y); n.putDouble("Z",origin.z);
            n.putFloat("Health",health); n.putInt("Phase",phase); n.putInt("Stranded",dryTicks);
            n.putInt("Clock",clock); n.putInt("Cooldown",cooldown); n.putInt("Burst",burst);
            n.putString("Move",move.name()); n.putInt("Remaining",remaining); n.putInt("Index",striker.index);
            n.putInt("Lane",striker.lane); if(target!=null)n.putUUID("Target",target);
            return n;
        }
        static Animal load(CompoundTag n) {
            Animal a=new Animal(n.getUUID("Id"),n.getBoolean("Aquatic"),new Vec3(n.getDouble("X"),n.getDouble("Y"),n.getDouble("Z")));
            a.health=n.getFloat("Health"); a.phase=n.getInt("Phase");
            if(a.phase==2)a.bar.setColor(BossEvent.BossBarColor.RED); a.dryTicks=n.getInt("Stranded");
            a.clock=n.getInt("Clock"); a.cooldown=n.getInt("Cooldown"); a.burst=n.getInt("Burst");
            a.move=Move.valueOf(n.getString("Move")); a.remaining=n.getInt("Remaining");
            a.striker=new Section(n.getInt("Index"),n.getInt("Lane")); if(n.hasUUID("Target"))a.target=n.getUUID("Target");
            return a;
        }
    }
}
