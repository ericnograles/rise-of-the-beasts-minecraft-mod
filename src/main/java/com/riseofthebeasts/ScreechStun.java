package com.riseofthebeasts;

import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;

final class ScreechStun extends MobEffect {
    ScreechStun(){
        super(MobEffectCategory.HARMFUL,0xff963e);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,"5337e9a8-7d36-4cce-9bbe-ea3d13369ac0",-1,AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
    static boolean stunned(LivingEntity entity){return entity.hasEffect(Beasts.STUN.get());}
    @SubscribeEvent public static void attack(AttackEntityEvent event){
        if(stunned(event.getEntity()))event.setCanceled(true);
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent event){
        if(event.isCancelable() && stunned(event.getEntity()))event.setCanceled(true);
    }
    @SubscribeEvent public static void mine(PlayerEvent.BreakSpeed event){
        if(stunned(event.getEntity()))event.setCanceled(true);
    }
    @SubscribeEvent public static void jump(LivingEvent.LivingJumpEvent event){
        if(event.getEntity() instanceof Player p && stunned(p))p.setDeltaMovement(p.getDeltaMovement().multiply(1,0,1));
    }
}
