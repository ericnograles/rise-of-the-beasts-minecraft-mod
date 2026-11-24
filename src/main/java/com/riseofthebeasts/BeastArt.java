package com.riseofthebeasts;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import static com.riseofthebeasts.BeastState.Move;

@Mod.EventBusSubscriber(modid=Beasts.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class BeastArt {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(Beasts.WIND.get(),Painter::new);e.registerEntityRenderer(Beasts.LEVI.get(),Painter::new);
        e.registerEntityRenderer(Beasts.SERPENT.get(),Painter::new);e.registerEntityRenderer(Beasts.SHOT.get(),ShotRenderer::new);
    }
    private static class ShotRenderer extends ThrownItemRenderer<BeastShot> {
        final net.minecraft.client.renderer.block.BlockRenderDispatcher blocks;
        ShotRenderer(EntityRendererProvider.Context ctx){super(ctx,1.35f,true);blocks=ctx.getBlockRenderDispatcher();}
        @Override public void render(BeastShot shot,float yaw,float partial,PoseStack p,MultiBufferSource buffers,int light){
            if(!shot.stone()){super.render(shot,yaw,partial,p,buffers,light);return;}
            p.pushPose(); p.translate(-2,-2,-2);p.scale(4,4,4);
            blocks.renderSingleBlock(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),p,buffers,light,OverlayTexture.NO_OVERLAY);
            p.popPose();
        }
    }
    private static class Painter<T extends Entity> extends EntityRenderer<T> {
        static final ResourceLocation WIND=Beasts.id("textures/entity/windegow.png"), LEVI=Beasts.id("textures/entity/leviathan.png"), SERPENT=Beasts.id("textures/entity/jormungandr.png");
        final ModelPart[] materials=new ModelPart[12];
        Painter(EntityRendererProvider.Context ctx) {
            super(ctx);
            MeshDefinition mesh=new MeshDefinition();
            for(int i=0;i<materials.length;i++)mesh.getRoot().addOrReplaceChild("m"+i,CubeListBuilder.create()
                .texOffs(i%4*32,i/4*32).addBox(-4,-4,-4,8,8,8),PartPose.ZERO);
            var root=LayerDefinition.create(mesh,128,128).bakeRoot();
            for(int i=0;i<materials.length;i++)materials[i]=root.getChild("m"+i);
        }
        @Override public ResourceLocation getTextureLocation(T e){return e instanceof Windegow?WIND:((Giant)e).levi()?LEVI:SERPENT;}
        @Override public void render(T e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            var brush=new Brush(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(e))),materials,light);
            pose.pushPose();
            if(e instanceof Windegow w){
                pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial,w.yBodyRotO,w.yBodyRot)+180));
                brush.wind(w,partial);
            }else brush.giant((Giant)e,partial);
            pose.popPose();super.render(e,yaw,partial,pose,buffers,light);
        }
    }
    /** Model coordinates are in blocks, Y up and forward along negative Z. */
    private record Brush(PoseStack p,VertexConsumer vertices,ModelPart[] parts,int light) {
        void box(int m,double x,double y,double z,double w,double h,double d,double pitch,double roll) {
            p.pushPose();p.translate(x,y,z);p.mulPose(Axis.ZP.rotation((float)roll));p.mulPose(Axis.XP.rotation((float)pitch));
            p.scale((float)w*2,(float)h*2,(float)d*2);
            parts[m].render(p,vertices,(m==8 || m==9)?Math.max(light,0xB000B0):light,OverlayTexture.NO_OVERLAY);p.popPose();
        }
        void box(int m,double x,double y,double z,double w,double h,double d){box(m,x,y,z,w,h,d,0,0);}
        void wind(Windegow w,float partial){
            double t=w.tickCount+partial;
            double stride=Math.sin(w.walkAnimation.position(partial)*.58)*Math.min(1,w.walkAnimation.speed(partial)*2.2);
            int windup=w.getEntityData().get(Windegow.WINDUP);
            double lift=windup==0?0:Math.sin(Math.min(1,(19-windup+partial)/8)*Math.PI*.5);
            p.translate(0,Math.abs(stride)*.08,0);
            box(0,0,2.02,.08,.64,.62,.48,-.08,0);
            box(0,0,2.65,0,.78,.98,.46,-.21,0);box(1,0,3.01,-.12,1.12,.32,.52);
            for(int side:new int[]{-1,1}){
                for(int i=0;i<3;i++)box(1,side*.23,2.52+i*.17,-.26-i*.03,.35,.075,.13,0,side*.12);
                p.pushPose();p.translate(side*.23,1.89,.10);p.mulPose(Axis.XP.rotation((float)(side*stride*.67)));
                box(0,0,-.43,0,.26,.95,.29,.14,side*.07);box(1,0,-.91,.04,.28,.26,.28);
                box(0,0,-1.30,.15,.18,.85,.19,-.2,0);box(4,0,-1.77,-.02,.25,.18,.42);
                p.popPose();
                p.pushPose();p.translate(side*.60,3,-.1);p.mulPose(Axis.XP.rotation((float)(-side*stride*.42+lift*1.75)));
                box(0,side*.09,-.44,0,.22,.98,.23,0,side*.2);
                box(1,side*.21,-.93,0,.24,.29,.25);
                box(0,side*.27,-1.32,-.13,.18,.84,.19,.27,side*.13);
                box(1,side*.31,-1.78,-.29,.29,.27,.18);
                for(int i=-1;i<=1;i++){
                    box(5,side*.31+i*.115,-1.99,-.34,.065,.31,.085,.20,i*.13);
                    box(3,side*.31+i*.13,-2.16,-.42,.055,.13,.07,.46,i*.16);
                }
                p.popPose();
            }
            p.pushPose();p.translate(0,3.47,-.32);p.mulPose(Axis.YP.rotationDegrees((-w.yHeadRot+w.yBodyRot)*.72f));
            box(2,0,0,0,.62,.62,.5,-.08,0);box(5,0,-.22,-.31,.37,.32,.37,.12,0);
            for(int s:new int[]{-1,1}){
                box(4,s*.18,.06,-.26,.19,.20,.055,0,s*.12);box(8,s*.18,.07,-.298,.045,.06,.03);
                box(4,s*.095,-.22,-.505,.065,.12,.03);
                box(5,s*.40,.39,.03,.19,.50,.19,.08,-s*.62);
                box(2,s*.60,.63,.04,.15,.43,.15,0,-s*.56);
                box(2,s*.71,.91,.02,.105,.37,.11,0,-s*.18);
                box(5,s*.43,.66,-.02,.095,.38,.1,-.14,s*.22);
                box(3,s*.75,1.11,.015,.075,.13,.08);
            }
            p.popPose();
        }
        void giant(Giant g,float partial) {
            double width=g.entityDataWidth(),span=g.getEntityData().get(Giant.SPAN);
            int index=g.getEntityData().get(Giant.INDEX);boolean hot=g.getEntityData().get(Giant.PHASE)==2;
            Move move=Move.values()[g.getEntityData().get(Giant.MOVE)];
            double t=g.tickCount+partial+index*2;
            int left=g.getEntityData().get(Giant.REMAINING);
            if(g.levi()) {
                new Kraken(p,vertices,light).draw(g,move,left-partial,t);
                return;
            }
            double action=move==Move.REST?0:Math.sin(t*.35);
            if(index==0){
                p.scale(3,3,3);width=11;
                // The crown tip is 12 model blocks above the jaw base (36 in world space).
                for(int side:new int[]{-1,1}){
                    box(hot?9:7,side*2,8.1,3.8,1.8,4.2,2.2,.24,-side*.2);
                    box(hot?9:7,side*2.6,10.7,4.4,1.15,2.0,1.2);
                    box(hot?8:5,side*2.65,11.8,4.4,.55,.4,.66);
                }
                double open=.13+Math.sin(t*.07)*.05;
                if(move==Move.INTRO)open+=.46*Math.sin(Math.PI*(1-left/100.));
                if(move==Move.DEATH)open+=.33*left/90.;
                if(move==Move.BITE || move==Move.FIRE || move==Move.SPIKE)open+=.26+.19*action;
                p.translate(0,width*.37,0);
                box(0,0,0,3,width*.88,width*.53,9);
                box(2,0,2.9,-.8,width*.79,1.35,7.2,-.12,0);
                box(7,0,3.8,.4,width*.42,.8,5.2,-.18,0);
                for(int i=0;i<4;i++)box(hot?8:7,0,4+i*.30,1.4+i*.93,1.6-i*.2,1.7,1.4,.55,0);
                for(int s:new int[]{-1,1}){
                    p.pushPose();p.translate(0,s*.5,1);p.mulPose(Axis.XP.rotation((float)(s*open)));
                    box(s==1?0:1,0,s*.48,-3.6,width*.84,1.8,9);
                    box(3,0,-s*.35,-3.6,width*.68,.52,8.6);
                    for(int side:new int[]{-1,1})for(int k=0;k<7;k++){
                        double z=-7.8+k*1.20;
                        box(4,side*width*.36,-s*.96,z,.5,1.0,.58,.08,side*.04);
                        box(5,side*width*.35,-s*1.58,z+.08,.23,.47,.27,.18,0);
                    }
                    p.popPose();
                    box(2,s*width*.46,1.3,-1.1,.78,1.55,2.2,-.22,0);
                    box(8,s*width*.50,1.24,-1.35,.16,.66,1.1);
                    box(6,s*width*.51,1.25,-1.44,.11,.62,.23);
                    for(int k=0;k<4;k++){
                        box(hot?9:7,s*(4.1+k*.32),2.1+k*.21,1.4+k*.92,1.15,2.8,2,-.60,-s*.47);
                        box(hot?8:5,s*(4.66+k*.32),3.45+k*.21,2+k*.92,.60,.95,1.05,-.65,-s*.55);
                    }
                }
                return;
            }
            p.mulPose(Axis.YP.rotationDegrees(g.yawDegrees()));
            p.mulPose(Axis.XP.rotationDegrees(g.pitchDegrees()));
            span/=Math.max(.15,Math.cos(Math.toRadians(g.yawDegrees()))*Math.cos(Math.toRadians(g.pitchDegrees())));
            if(move==Move.SLASH || move==Move.STONE)p.mulPose(Axis.ZP.rotation((float)(action*.16)));
            box(1,0,width*.27,0,width*.86,width*.55,span+.45);
            for(int k=0;k<2;k++){
                double z=(k-.5)*span*.5;
                box(0,0,width*.37,z,width,width*.57,span*.46,-.035,0);
                for(int s:new int[]{-1,1}) {
                    box(2,s*width*.46,width*.32,z+.5,width*.13,width*.37,span*.37,0,-s*.18);
                    if(hot)box(8,s*width*.512,width*.38,z,.17,width*.09,span*.54);
                }
                if(hot || index%4==0) {
                    double h=width*(hot?.62:.21);
                    box(hot?9:2,0,width*.63+h*.38,z,width*.13,h*.84,span*.14,.41,0);
                    box(hot?9:2,0,width*.63+h*.88,z+h*.24,width*.075,h*.38,span*.075,.54,0);
                }
            }
        }
    }
}
