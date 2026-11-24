package com.riseofthebeasts;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public class SoundingShell extends Item {
    SoundingShell(Properties properties){super(properties);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        if(level instanceof ServerLevel server && level.dimension()==Level.OVERWORLD) {
            BlockPos center=Cavern.centerFor(player.getBlockX(),player.getBlockZ()),best=null;
            double closest=Double.MAX_VALUE;
            for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++){
                BlockPos p=center.offset(x*Cavern.GRID,0,z*Cavern.GRID);
                double dist=p.distToCenterSqr(player.position());
                if(dist<closest && Cavern.valid(server,p)){closest=dist;best=p;}
            }
            Encounters.announce(player,best==null?"Silent water. Try again farther out.":
                "The shell echoes from a deep-ocean vent at X "+best.getX()+", Z "+best.getZ()+". Dive between the lights.");
            player.getCooldowns().addCooldown(this,80);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
}
