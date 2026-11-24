package com.riseofthebeasts;

import java.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import static com.riseofthebeasts.BeastState.*;

@Mod.EventBusSubscriber(modid=Beasts.ID,value=Dist.CLIENT)
final class KrakenDisplay {
    record Timed(KrakenView view,long received){
        double elapsed(ClientLevel level,float partial){return Math.max(0,Math.min(8,level.getGameTime()-received+partial));}
        double progress(ClientLevel level,float partial){
            int duration=view.move()==Move.DEATH?Animal.DEATH_TICKS:Animal.INTRO_TICKS;
            return Math.min(1,Math.max(0,1-(view.remaining()-elapsed(level,partial))/duration));
        }
        Vec3 focus(ClientLevel level,float partial){
            double t=progress(level,partial),sink=KrakenBody.submerge(view.move(),t);
            if(view.move()==Move.DEATH)sink*=1-Animal.ease((t-.22)/.53);
            return view.origin().add(0,42+sink,0);
        }
    }
    private static final RenderType LAYER=RenderType.entityCutoutNoCull(Beasts.id("textures/entity/kraken.png"));
    private static final Map<UUID,Timed> poses=new HashMap<>();
    private static ClientLevel world;
    private static void updateWorld(ClientLevel level){if(world!=level){poses.clear();world=level;}}
    static void receive(KrakenView view){
        var level=Minecraft.getInstance().level;updateWorld(level);
        if(level==null || view.removed())poses.remove(view.id());
        else poses.put(view.id(),new Timed(view,level.getGameTime()));
    }
    static Timed closestScene(ClientLevel level,Vec3 eye){
        updateWorld(level);
        Timed nearest=null;double range=Double.MAX_VALUE;
        for(var pose:poses.values()){
            var v=pose.view();
            if(v.move()!=Move.INTRO && v.move()!=Move.DEATH)continue;
            double d=v.origin().distanceToSqr(eye);
            if(d<range && d<330*330){nearest=pose;range=d;}
        }
        return nearest;
    }
    @SubscribeEvent static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
        var mc=Minecraft.getInstance();var level=mc.level;updateWorld(level);
        if(level==null || poses.isEmpty())return;
        var camera=event.getCamera().getPosition();var pose=event.getPoseStack();
        var buffers=mc.renderBuffers().bufferSource();
        int light=LevelRenderer.getLightColor(level,BlockPos.containing(camera));
        for(var it=poses.values().iterator();it.hasNext();){
            var timed=it.next();var v=timed.view();var p=v.origin();
            if(level.getGameTime()-timed.received()>50){it.remove();continue;}
            if(!event.getFrustum().isVisible(new AABB(p.x-225,p.y-250,p.z-KrakenBody.MANTLE,p.x+225,p.y+150,p.z+KrakenBody.REACH)))continue;
            double elapsed=timed.elapsed(level,event.getPartialTick()),t=timed.progress(level,event.getPartialTick());
            Vec3 socket=p.add(0,36+KrakenBody.submerge(v.move(),t),0);
            pose.pushPose();pose.translate(socket.x-camera.x,socket.y-camera.y,socket.z-camera.z);
            new KrakenArt(pose,buffers.getBuffer(LAYER),light).draw(v,v.age()+elapsed,t,v.remaining()-elapsed,camera.subtract(socket));
            pose.popPose();
        }
        // Preserve the long silhouette through ordinary distance haze. Underwater
        // and status-effect fog keep the same range as the rest of the scene.
        float near=RenderSystem.getShaderFogStart();
        float far=RenderSystem.getShaderFogEnd();
        boolean openAir=event.getCamera().getFluidInCamera()==net.minecraft.world.level.material.FogType.NONE &&
            mc.player!=null && !mc.player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS) &&
            !mc.player.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS);
        if(openAir){
            float horizon=Math.min(1100,mc.options.getEffectiveRenderDistance()*60);
            RenderSystem.setShaderFogStart(Math.max(near,horizon*.34f));
            RenderSystem.setShaderFogEnd(Math.max(far,horizon));
        }
        try {buffers.endBatch(LAYER);}
        finally {
            if(openAir){
                RenderSystem.setShaderFogStart(near);
                RenderSystem.setShaderFogEnd(far);
            }
        }
    }
}
