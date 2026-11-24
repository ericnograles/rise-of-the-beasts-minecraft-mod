package com.riseofthebeasts;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import static com.riseofthebeasts.BeastState.*;

@GameTestHolder(Beasts.ID)
@PrefixGameTestTemplate(false)
public final class EncounterTests {
    @GameTest(template="empty")
    public static void movingAndStill(GameTestHelper h) {
        var level=h.getLevel(); Vec3 at=h.absolutePos(new BlockPos(3,6,3)).getCenter();
        var w=Beasts.WIND.get().create(level);w.setPos(at);
        var runner=h.makeMockSurvivalPlayer();var friend=h.makeMockSurvivalPlayer();friend.setUUID(java.util.UUID.randomUUID());
        runner.setPos(at.add(1,0,0));friend.setPos(at.add(0,0,1));
        w.observe(runner);w.observe(friend);runner.setPos(at.add(1.3,0,0));w.observe(runner);w.observe(friend);
        h.assertTrue(!w.scratch(friend) && friend.getHealth()==20,"Stationary friend took the runner's scratch");
        w.observe(runner);
        h.assertTrue(!w.scratch(runner),"Stopping before contact failed");
        runner.setPos(at.add(1.5,0,0));w.observe(runner);
        h.assertTrue(w.scratch(runner) && !runner.isAlive(),"Moving runner survived an unblocked scratch");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void shallowWaterResetsLandTimer(GameTestHelper h){
        var level=h.getLevel();BlockPos pos=h.absolutePos(new BlockPos(3,16,3));
        Giant g=Beasts.LEVI.get().create(level);g.setPos(pos.getCenter());
        Animal a=BeastState.get(level).create(true,g.position());
        g.bind(a,new Section(0,0));level.addFreshEntity(g);
        for(int i=0;i<1199;i++)h.assertTrue(!Encounters.stranded(a,false),"Died early");
        level.setBlockAndUpdate(pos,Blocks.WATER.defaultBlockState());
        h.assertTrue(Encounters.waterAtHead(level,g),"Shallow water missed the jaw");
        Encounters.tickStranding(level,a);h.assertTrue(a.dryTicks==0,"Water failed to reset continuous timer");
        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        h.assertTrue(!Encounters.waterAtHead(level,g),"Air counted as water");
        for(int i=0;i<1199;i++)h.assertTrue(!Encounters.stranded(a,false),"Second timer expired early");
        h.assertTrue(Encounters.tickStranding(level,a) && g.isRemoved() && !BeastState.get(level).animals.containsKey(a.id),"Survived 1200 dry ticks");h.succeed();
    }
    @GameTest(template="empty")
    public static void projectileShapesAndVenom(GameTestHelper h) {
        var level=h.getLevel();
        Vec3 at=h.absolutePos(new BlockPos(4,15,4)).getCenter();
        var cow=net.minecraft.world.entity.EntityType.COW.create(level);
        var venom=Beasts.VENOM.get();int pulses=0;
        for(int t=400;t>0;t--)if(venom.isDurationEffectTick(t,0))pulses++;
        h.assertTrue(pulses==16,"Venom needs 16 pulses in 20 seconds");
        for(Move move:new Move[]{Move.STONE,Move.FIRE,Move.SPIKE}){
            var shot=Beasts.SHOT.get().create(level);
            shot.setItem(new net.minecraft.world.item.ItemStack(move==Move.STONE?Items.STONE:move==Move.FIRE?Items.FIRE_CHARGE:Items.POINTED_DRIPSTONE));
            shot.setPos(at);shot.tick();
            if(move==Move.STONE)h.assertTrue(shot.getBoundingBox().getXsize()==4 && shot.getBoundingBox().getYsize()==4,"Small stone hitbox");
            cow.setHealth(10);cow.invulnerableTime=0;
            shot.onHit(new net.minecraft.world.phys.EntityHitResult(cow));
            float damage=move==Move.STONE?5:move==Move.FIRE?9:4;
            h.assertTrue(cow.getHealth()==10-damage,"Wrong projectile damage for "+move);
        }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void stomachAndBothDefeats(GameTestHelper h){
        var level=h.getLevel();
        var p=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"BeastTester"));
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel=new io.netty.channel.embedded.EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection,p);
        Vec3 at=h.absolutePos(new BlockPos(3,8,3)).getCenter();p.teleportTo(level,at.x,at.y,at.z,0,0);
        var state=BeastState.get(level);var a=state.create(false,at);
        var g=Beasts.SERPENT.get().create(level);g.bind(a,new Section(0,0));level.addFreshEntity(g);
        a.move=Move.BITE;
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        // Vanilla grants a brief join shield. Let the fixture reach an ordinary combat state.
        for(int i=0;i<65;i++)p.tick();
        var difficulty=level.getDifficulty();
        try {
            level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
            Encounters.strike(level,a,g,p);
        } finally {
            level.getServer().setDifficulty(difficulty,true);
        }
        h.assertTrue(p.getHealth()==12,"Bite must deal eight HP");
        h.assertTrue(Stomach.inside(p) && p.level().dimension()==Stomach.DIM,"Swallow did not enter stomach");
        BlockPos valve=BlockPos.of(p.getPersistentData().getCompound(Stomach.RETURN).getLong("Valve"));
        h.assertTrue(p.level().getBlockState(valve).is(Blocks.SHROOMLIGHT),"Missing escape valve");
        h.assertTrue(!Stomach.escape(p,valve.above()) && Stomach.escape(p,valve),"Valve escape failed");
        h.assertTrue(p.level()==level && !Stomach.inside(p),"Escape lost the return position");
        Stomach.swallow(p,a.id);
        Stomach.login(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(p));
        h.assertTrue(!Stomach.inside(p) && p.level()==level,"Reconnect failed to release player");
        Stomach.swallow(p,a.id);g.hurt(p.damageSources().playerAttack(p),40800);
        h.assertTrue(p.level()==level && a.phase==2 && a.health==40800 && a.length()==2560,"Transformation or phase release failed");
        h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,g.getBoundingBox().inflate(8),it->it.getItem().is(Items.NETHER_STAR)).isEmpty(),"Early victory drop");
        Encounters.damage(level,g,40800);h.assertTrue(a.health==40800,"Transition repeated during transformation");
        Animal restored=Animal.load(a.save());h.assertTrue(restored.phase==2 && restored.health==a.health && restored.move==Move.TRANSFORM,"Encounter save lost phase state");
        a.move=Move.REST;Stomach.swallow(p,a.id);Encounters.damage(level,g,40800);
        h.assertTrue(!state.animals.containsKey(a.id) && !Stomach.inside(p) && p.level()==level,"Final defeat did not clean up");
        h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,g.getBoundingBox().inflate(8),it->it.getItem().is(Items.NETHER_STAR)).size()==1,"Expected one victory reward");
        level.getServer().getPlayerList().remove(p);channel.finishAndReleaseAll();h.succeed();
    }
}
