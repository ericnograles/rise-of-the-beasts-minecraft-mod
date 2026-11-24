package com.riseofthebeasts;

import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;

final class Venom extends MobEffect {
    Venom(){ super(MobEffectCategory.HARMFUL,0x8fab3c); }
    @Override public boolean isDurationEffectTick(int duration,int amplifier){return duration%25==1;}
    @Override public void applyEffectTick(LivingEntity target,int amplifier){
        if(!target.level().isClientSide)target.hurt(target.damageSources().magic(),1);
    }
}
