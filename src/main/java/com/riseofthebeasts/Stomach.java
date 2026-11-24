package com.riseofthebeasts;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;

final class Stomach {
    static final ResourceKey<Level> DIM=ResourceKey.create(Registries.DIMENSION,Beasts.id("stomach"));
    static final String RETURN="riseofthebeasts_return", CELL="riseofthebeasts_cell";
    private static final class Cells extends SavedData {
        int next;
        static Cells load(CompoundTag tag){Cells cells=new Cells();cells.next=tag.getInt("Next");return cells;}
        @Override public CompoundTag save(CompoundTag tag){tag.putInt("Next",next);return tag;}
    }
    static boolean inside(Player p){return p.getPersistentData().contains(RETURN);}
    static void swallow(ServerPlayer player,UUID boss) {
        if(inside(player) || player.level().dimension()==DIM)return;
        ServerLevel belly=player.server.getLevel(DIM);
        if(belly==null)return;
        CompoundTag data=player.getPersistentData(), home=new CompoundTag();
        if(!data.contains(CELL)){
            Cells cells=belly.getDataStorage().computeIfAbsent(Cells::load,Cells::new,"riseofthebeasts_chambers");
            data.putInt(CELL,cells.next++);cells.setDirty();
        }
        int cell=data.getInt(CELL); BlockPos center=new BlockPos((cell%2048)*32,64,(cell/2048)*32);
        buildChamber(belly,center);
        home.putUUID("Boss",boss); home.putString("Dimension",player.level().dimension().location().toString());
        home.putDouble("X",player.getX()); home.putDouble("Y",player.getY()); home.putDouble("Z",player.getZ());
        home.putLong("Valve",center.offset(0,2,-5).asLong()); data.put(RETURN,home);
        player.stopRiding(); player.teleportTo(belly,center.getX()+.5,65,center.getZ()+3.5,180,0);
        Encounters.announce(player,"Swallowed! Cross the stomach and use the glowing valve in the far wall to escape.");
    }
    private static void buildChamber(ServerLevel level,BlockPos center) {
        for(int z=-5;z<=5;z++)for(int x=-4;x<=4;x++)for(int y=0;y<=5;y++){
            boolean wall=Math.abs(z)==5 || Math.abs(x)==4 || y==0 || y==5;
            Block block=wall?Blocks.RED_NETHER_BRICKS:Blocks.AIR;
            if(wall && (z%3==0 || x==0 && y==5))block=Blocks.CRIMSON_HYPHAE;
            if(y==1 && Math.abs(x)==3 && z%3==0)block=Blocks.BONE_BLOCK;
            if(x==0 && y==2 && z==-5)block=Blocks.SHROOMLIGHT;
            level.setBlockAndUpdate(center.offset(x,y,z),block.defaultBlockState());
        }
    }
    @SubscribeEvent public static void valve(PlayerInteractEvent.RightClickBlock event) {
        if(event.getEntity() instanceof ServerPlayer p && escape(p,event.getPos())){
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);event.setCanceled(true);
        }
    }
    static boolean escape(ServerPlayer p,BlockPos pos){
        if(!inside(p) || p.level().dimension()!=DIM || pos.asLong()!=p.getPersistentData().getCompound(RETURN).getLong("Valve"))return false;
        release(p);return true;
    }
    @SubscribeEvent public static void protect(net.minecraftforge.event.level.BlockEvent.BreakEvent event){
        if(event.getPlayer().level().dimension()==DIM && !event.getPlayer().isCreative())event.setCanceled(true);
    }
    static void releaseAll(ServerLevel level,UUID boss){
        for(ServerPlayer p:level.getServer().getPlayerList().getPlayers())
            if(inside(p) && boss.equals(p.getPersistentData().getCompound(RETURN).getUUID("Boss")))release(p);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer p && (inside(p)||p.level().dimension()==DIM))release(p);
    }
    @SubscribeEvent public static void recover(net.minecraftforge.event.TickEvent.PlayerTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || p.tickCount%20!=0 || !inside(p))return;
        CompoundTag home=p.getPersistentData().getCompound(RETURN);
        BlockPos valve=BlockPos.of(home.getLong("Valve"));
        if(p.level().dimension()!=DIM || p.blockPosition().distSqr(valve)>256)release(p);
    }
    static void release(ServerPlayer p){
        CompoundTag tag=p.getPersistentData().getCompound(RETURN);
        ResourceLocation name=ResourceLocation.tryParse(tag.getString("Dimension"));
        ServerLevel home=name==null?null:p.server.getLevel(ResourceKey.create(Registries.DIMENSION,name));
        Vec3 at=new Vec3(tag.getDouble("X"),tag.getDouble("Y"),tag.getDouble("Z"));
        if(home==null || home.dimension()==DIM){home=p.server.overworld();at=Vec3.atBottomCenterOf(home.getSharedSpawnPos().above());}
        home.getChunkAt(BlockPos.containing(at));
        // Step out of the jaw, with headroom and protection for surface, cave and ocean returns.
        Vec3 out=null;
        for(int r=0;r<=16 && out==null;r+=4)for(int dx=-1;dx<=1 && out==null;dx++)for(int dz=-1;dz<=1 && out==null;dz++){
            for(int dy=0;dy<=12;dy++){
                Vec3 spot=at.add(dx*r,dy,dz*r);
                if(safe(home,p,spot)){out=spot;break;}
            }
        }
        if(out==null)out=Vec3.atBottomCenterOf(home.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,BlockPos.containing(at)).above());
        p.teleportTo(home,out.x,out.y,out.z,p.getYRot(),0);
        p.getPersistentData().remove(RETURN);p.fallDistance=0;
        for(MobEffect effect:new MobEffect[]{MobEffects.WATER_BREATHING,MobEffects.SLOW_FALLING,MobEffects.DAMAGE_RESISTANCE,MobEffects.FIRE_RESISTANCE})
            p.addEffect(new MobEffectInstance(effect,200,effect==MobEffects.DAMAGE_RESISTANCE?4:0));
        Encounters.announce(p,"Cold air. You are free.");
    }
    private static boolean safe(ServerLevel level,ServerPlayer p,Vec3 pos){
        BlockPos feet=BlockPos.containing(pos);
        AABB box=p.getDimensions(p.getPose()).makeBoundingBox(pos);
        return !level.isOutsideBuildHeight(feet.above()) && level.getWorldBorder().isWithinBounds(feet) &&
            !level.getFluidState(feet).is(FluidTags.LAVA) && level.noCollision(p,box) && level.getEntitiesOfClass(Giant.class,box).isEmpty();
    }
}
