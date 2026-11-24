package com.riseofthebeasts;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import static com.riseofthebeasts.BeastState.Move;

@Mod.EventBusSubscriber(modid=Beasts.ID,value=Dist.CLIENT)
public final class EncounterHud {
    static final ResourceLocation BAR=Beasts.id("textures/gui/world_serpent.png");
    static final ResourceLocation KRAKEN_BAR=Beasts.id("textures/gui/kraken.png");
    private static Giant subject;
    private static KrakenDisplay.Timed krakenScene;
    private static float krakenVisibility;
    @SubscribeEvent public static void locate(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();subject=null;krakenScene=null;krakenVisibility=0;
        if(mc.level==null || mc.player==null)return;
        krakenScene=KrakenDisplay.closestScene(mc.level,mc.player.getEyePosition());
        double nearest=Double.MAX_VALUE;
        for(var entity:mc.level.entitiesForRendering())if(entity instanceof Giant g){

            if(g.levi() || g.kraken()){
                double distance=g.getBoundingBox().distanceToSqr(mc.player.getEyePosition());
                krakenVisibility=Math.max(krakenVisibility,(float)BeastState.Animal.ease((108-Math.sqrt(distance))/36));
            }
            if(g.kraken() && krakenScene!=null)continue;
            int move=g.getEntityData().get(Giant.MOVE);
            if(move!=Move.INTRO.ordinal() && move!=Move.DEATH.ordinal())continue;
            // Prefer the face when it is tracked; distant tentacles get their own close-up.
            double d=g.distanceToSqr(mc.player)+(g.head()?0:100000);
            if(d<nearest){subject=g;nearest=d;}
        }
    }
    private static double progress(double partial){
        if(krakenScene!=null)return krakenScene.progress(Minecraft.getInstance().level,(float)partial);
        int duration=subject.getEntityData().get(Giant.MOVE)==Move.INTRO.ordinal()?BeastState.Animal.INTRO_TICKS:BeastState.Animal.DEATH_TICKS;
        return Mth.clamp(1-(subject.getEntityData().get(Giant.REMAINING)-partial)/duration,0,1);
    }
    @SubscribeEvent public static void underwater(ViewportEvent.RenderFog e){
        if(krakenVisibility==0 || e.getType()!=net.minecraft.world.level.material.FogType.WATER)return;
        var p=Minecraft.getInstance().player;
        if(p==null || p.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS) || p.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS))return;
        e.setNearPlaneDistance(Mth.lerp(krakenVisibility,e.getNearPlaneDistance(),14));
        e.setFarPlaneDistance(Mth.lerp(krakenVisibility,e.getFarPlaneDistance(),Math.max(124,e.getFarPlaneDistance())));
        e.setCanceled(true);
    }
    @SubscribeEvent public static void waterColor(ViewportEvent.ComputeFogColor e){
        if(krakenVisibility==0 || e.getCamera().getFluidInCamera()!=net.minecraft.world.level.material.FogType.WATER)return;
        e.setGreen(Mth.lerp(krakenVisibility*.55f,e.getGreen(),.16f));
        e.setBlue(Mth.lerp(krakenVisibility*.6f,e.getBlue(),.27f));
    }
    @SubscribeEvent public static void frame(ViewportEvent.ComputeCameraAngles e){
        if((subject==null && krakenScene==null) || Minecraft.getInstance().options.getCameraType().isMirrored())return;
        double partial=e.getPartialTick(),t=progress(partial);
        var focus=krakenScene!=null?krakenScene.focus(Minecraft.getInstance().level,(float)partial):subject.getPosition((float)partial).add(0,subject.head()?(subject.levi()?15:17):subject.entityDataWidth()*.4,0);
        if(krakenScene==null && subject.kraken() && subject.head() && subject.getEntityData().get(Giant.MOVE)==Move.DEATH.ordinal())
            focus=focus.add(0,-KrakenBody.submerge(Move.DEATH,t)*BeastState.Animal.ease((t-.22)/.53),0);
        var direction=focus.subtract(e.getCamera().getPosition());
        if(direction.lengthSqr()<16)return;
        float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));
        float blend=(float)(BeastState.Animal.ease(t*5)*BeastState.Animal.ease((1-t)*4));
        e.setYaw(e.getYaw()+blend*Mth.wrapDegrees(yaw-e.getYaw()));
        e.setPitch(Mth.lerp(blend,e.getPitch(),pitch));
    }
    @SubscribeEvent public static void lens(ViewportEvent.ComputeFov e){
        if((subject!=null || krakenScene!=null) && e.usedConfiguredFov())e.setFOV(Math.min(130,e.getFOV()+9*Math.sin(Math.PI*progress(e.getPartialTick()))));
    }
    @SubscribeEvent public static void scene(RenderGuiEvent.Pre e){
        var mc=Minecraft.getInstance();
        if((subject==null && krakenScene==null) || mc.options.hideGui)return;
        var g=e.getGuiGraphics();
        int w=g.guiWidth(),h=g.guiHeight();double t=progress(e.getPartialTick());
        double fade=Math.min(BeastState.Animal.ease(t*7),BeastState.Animal.ease((1-t)*6));
        int border=(int)(fade*h*.073),opacity=(int)(fade*215)<<24;
        // The boss name starts four GUI pixels below the top edge.
        g.fill(0,0,w,Math.min(3,border),opacity);
        g.fill(0,h-border,w,h,opacity);
        int alpha=(int)(fade*255);
        if(alpha<8)return;
        boolean death=krakenScene!=null?krakenScene.view().move()==Move.DEATH:subject.getEntityData().get(Giant.MOVE)==Move.DEATH.ordinal();
        var title=Component.translatable("scene.riseofthebeasts."+(krakenScene!=null || subject.kraken()?"kraken":subject.levi()?"leviathan":"serpent")+(death?".death":".intro"));
        int textY=h-48,half=mc.font.width(title)/2;
        g.fill(w/2-half-7,textY-4,w/2+half+7,textY+12,(int)(fade*155)<<24);
        g.drawCenteredString(mc.font,title,w/2,textY,(alpha<<24)|0xeed7ac);
    }
    @SubscribeEvent public static void serpentBar(CustomizeGuiOverlayEvent.BossEventProgress e){
        var boss=e.getBossEvent();
        if(!(boss.getName().getContents() instanceof TranslatableContents label))return;
        if(label.getKey().equals("boss.riseofthebeasts.kraken")){
            e.setCanceled(true);e.setIncrement(39);
            var graphics=e.getGuiGraphics();int x=graphics.guiWidth()/2-112,y=e.getY();
            graphics.drawCenteredString(Minecraft.getInstance().font,boss.getName(),x+112,y-9,0xfff7dbc0);
            graphics.blit(KRAKEN_BAR,x,y,0,0,224,27,224,54);
            int width=Math.round(196*Mth.clamp(boss.getProgress(),0,1));
            if(width>0)graphics.blit(KRAKEN_BAR,x+14,y+16,14,43,width,6,224,54);
            return;
        }
        if(!label.getKey().equals("boss.riseofthebeasts.jormungandr"))return;
        e.setCanceled(true);e.setIncrement(39);
        var g=e.getGuiGraphics();var font=Minecraft.getInstance().font;
        int mid=g.guiWidth()/2,y=e.getY()-9,x=mid-100;
        g.drawCenteredString(font,boss.getName(),mid,y,0xfff9e6c8);
        g.drawCenteredString(font,Component.translatable("subtitle.riseofthebeasts.world_serpent"),mid,y+10,0xffb8c4b2);
        g.blit(BAR,x,y+19,0,0,200,14,200,42);
        int filled=Math.round(180*Mth.clamp(boss.getProgress(),0,1));
        if(filled>0)g.blit(BAR,x+10,y+19,10,boss.getColor()==BossEvent.BossBarColor.RED?28:14,filled,14,200,42);
    }
    @SubscribeEvent public static void immobilize(MovementInputUpdateEvent e){
        if(!ScreechStun.stunned(e.getEntity()))return;
        var input=e.getInput();
        input.forwardImpulse=input.leftImpulse=0;
        input.up=input.down=input.left=input.right=input.jumping=input.shiftKeyDown=false;
    }
}
