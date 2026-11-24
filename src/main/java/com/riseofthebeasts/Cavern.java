package com.riseofthebeasts;

import com.mojang.serialization.Codec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Generates only the current 16x16 slice. Candidate vents use the seed's deep-ocean biome map. */
public class Cavern extends Feature<NoneFeatureConfiguration> {
    static final int GRID=2048,RADIUS=930;
    Cavern(Codec<NoneFeatureConfiguration> codec){super(codec);}
    static BlockPos centerFor(int x,int z){return new BlockPos(Math.floorDiv(x,GRID)*GRID+GRID/2,32,Math.floorDiv(z,GRID)*GRID+GRID/2);}
    static boolean valid(ServerLevel level,BlockPos center){
        var cache=level.getChunkSource();
        return cache.getGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(center.getX()),QuartPos.fromBlock(42),
            QuartPos.fromBlock(center.getZ()),cache.randomState().sampler()).is(BiomeTags.IS_DEEP_OCEAN);
    }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        WorldGenLevel level=context.level();
        if(level.getLevel().dimension()!=Level.OVERWORLD)return false;
        int x0=context.origin().getX() & ~15,z0=context.origin().getZ() & ~15;
        BlockPos center=centerFor(x0,z0);
        if(Math.hypot(x0+8-center.getX(),z0+8-center.getZ())>RADIUS+12 || !valid(level.getLevel(),center))return false;
        var pos=new BlockPos.MutableBlockPos();
        for(int x=x0;x<x0+16;x++)for(int z=z0;z<z0+16;z++){
            double dx=x+.5-center.getX(),dz=z+.5-center.getZ();
            double r=(dx*dx+dz*dz)/(RADIUS*RADIUS);
            if(r>=1)continue;
            double dome=Math.sqrt(1-r);
            int bottom=-55+(int)((1-dome)*12);
            int roof=bottom+Math.max(1,(int)(50*dome));
            boolean vent=dx*dx+dz*dz<64;
            int top=vent?level.getLevel().getSeaLevel()-1:roof;
            for(int y=bottom-1;y<=top+(vent?0:1);y++){
                Block block=Blocks.WATER;
                if(y==bottom-1 || (!vent && y==roof+1))block=Blocks.DEEPSLATE;
                if(y==bottom-1 && ((x*31+z*19)&127)==0)block=Blocks.SEA_LANTERN;
                if(vent && dx*dx+dz*dz>42 && (y+64)%9==0)block=Blocks.SEA_LANTERN;
                level.setBlock(pos.set(x,y,z),block.defaultBlockState(),2);
            }
        }
        return true;
    }
}
