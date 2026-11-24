package com.riseofthebeasts;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import static com.riseofthebeasts.BeastState.Move;

/** Elliptical cross-sections give the mantle, eyes, teeth and arms distinct silhouettes. */
record Kraken(PoseStack pose, VertexConsumer out, int light) {
    private static final int SIDES=12;
    private static final int SKIN=0, FLANK=1, CUP=2, GUM=3, IVORY=4, TIP=5, PUPIL=6, BELLY=7, IRIS=8, RIM=9, SOCKET=10;
    private void vertex(int tile,double x,double y,double z,float u,float v,double nx,double ny,double nz){
        var part=pose.last();
        float tu=((tile%4)*32+1+u*29)/128f,tv=((tile/4)*32+1+v*29)/128f;
        out.vertex(part.pose(),(float)x,(float)y,(float)z).color(255,255,255,255).uv(tu,tv)
           .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(part.normal(),(float)nx,(float)ny,(float)nz).endVertex();
    }
    // The two ellipses may have different centers and radii. Ends taper to small points or meet the next ring.
    private void band(int top,int bottom,double z0,double y0,double x0,double h0,double z1,double y1,double x1,double h1){
        for(int i=0;i<SIDES;i++){
            double a=Math.PI*2*i/SIDES,b=Math.PI*2*(i+1)/SIDES,mid=(a+b)*.5;
            double ax=Math.cos(a),ay=Math.sin(a),bx=Math.cos(b),by=Math.sin(b);
            int tile=Math.sin(mid)<-.38?bottom:top;
            double nx=Math.cos(mid),ny=Math.sin(mid);
            vertex(tile,x0*ax,y0+h0*ay,z0,0,0,nx,ny,0);
            vertex(tile,x0*bx,y0+h0*by,z0,1,0,nx,ny,0);
            vertex(tile,x1*bx,y1+h1*by,z1,1,1,nx,ny,0);
            vertex(tile,x1*ax,y1+h1*ay,z1,0,1,nx,ny,0);
        }
    }
    void draw(Giant g,Move move,double remaining,double time){
        double intro=move==Move.INTRO?1-remaining/BeastState.Animal.INTRO_TICKS:1;
        double death=move==Move.DEATH?1-remaining/BeastState.Animal.DEATH_TICKS:0;
        double beat=1-Mth.clamp(remaining/(move==Move.SCREECH?34:24),0,1);
        if(g.head()){
            double gape=.23+.045*Math.sin(time*.085);
            if(move==Move.BITE)gape+=.8*Math.sin(Math.PI*Math.min(1,beat*1.21));
            if(move==Move.SCREECH)gape+=.82*BeastState.Animal.ease(beat*1.6)+.045*Math.sin(time*1.8);
            if(move==Move.CHARGE)gape+=.32*Math.sin(beat*Math.PI);
            gape+=.55*Math.sin(Math.PI*intro)-.18*death;
            pose.mulPose(Axis.ZP.rotation((float)(-.31*death)));
            pose.mulPose(Axis.XP.rotation((float)(move==Move.CHARGE?-.17*Math.sin(beat*2*Math.PI):0)));
            band(SKIN,BELLY,-13,14,6,7,-6,15,17,13);
            band(SKIN,BELLY,-6,15,17,13,5,16,22,17);
            band(FLANK,BELLY,5,16,22,17,13,15,20,14);
            band(SKIN,BELLY,13,15,20,14,21,14,11,8);
            band(SKIN,BELLY,21,14,11,8,25,14,.1,.1);
            for(int side:new int[]{-1,1}){
                eye(side,time,move);
                pose.pushPose();pose.translate(0,14,-5);
                pose.mulPose(Axis.XP.rotation((float)(side*gape)));
                jaw(side);pose.popPose();
            }
            return;
        }
        pose.mulPose(Axis.YP.rotationDegrees(g.yawDegrees()));
        pose.mulPose(Axis.XP.rotationDegrees(g.pitchDegrees()));
        double half=g.getEntityData().get(Giant.SPAN)*.5/Math.max(.15,Math.cos(Math.toRadians(g.yawDegrees()))*Math.cos(Math.toRadians(g.pitchDegrees())));
        double radius=g.entityDataWidth()*.5;
        // Orange ventral skin twists into view around the rolling arm.
        pose.mulPose(Axis.ZP.rotation((float)(.55*Math.sin(time*.045+g.getEntityData().get(Giant.INDEX)*.6)+.35)));
        band(SKIN,BELLY,-half-.35,radius*.65,radius,radius*.82,0,radius*.68,radius*1.03,radius*.86);
        band(SKIN,BELLY,0,radius*.68,radius*1.03,radius*.86,half+.35,radius*.65,radius,radius*.82);
        for(int side:new int[]{-1,1})for(int j=-1;j<=1;j++){
            pose.pushPose();
            pose.translate(side*radius*.67,radius*.18,(j+side*.16)*half*.63);
            pose.mulPose(Axis.ZP.rotationDegrees(side*42));
            cup(radius*.38);pose.popPose();
        }
    }
    private void eye(int side,double time,Move move){
        pose.pushPose();pose.translate(side*15.6,19,-6.5);
        pose.mulPose(Axis.YP.rotationDegrees(side*120));
        band(SKIN,BELLY,-1,0,4.9,4.2,1.1,0,4.1,3.4);
        band(IRIS,IRIS,1.1,0,4.1,3.4,2.0,0,3.5,3);
        band(IRIS,IRIS,2.0,0,3.5,3,2.35,0,.02,.02);
        double aperture=move==Move.SCREECH?1.5:.9+.18*Math.sin(time*.12);
        band(PUPIL,PUPIL,2.22,0,aperture,2.45,2.45,0,.04,.04);
        pose.popPose();
    }
    private void jaw(int side){
        // Long narrow snout with a pink inner palate and two irregular tooth rows.
        band(SKIN,BELLY,-29,side*3,.15,.12,-24,side*3.7,5,2.2);
        band(SKIN,BELLY,-24,side*3.7,5,2.2,-12,side*4.4,10.4,3.4);
        band(SKIN,BELLY,-12,side*4.4,10.4,3.4,0,side*3.3,13.2,4.1);
        band(GUM,GUM,-26,side*2.0,.2,.1,-14,side*2,7.2,1.25);
        band(GUM,GUM,-14,side*2,7.2,1.25,0,side*1.2,10,1.65);
        for(int s:new int[]{-1,1})for(int i=0;i<9;i++){
            double r=.50+i*.065,fang=(i%3==0?4:2.8)+i*.13;
            pose.pushPose();pose.translate(s*(2.3+i*.95),side*2.1,-25+i*2.65);
            pose.mulPose(Axis.XP.rotationDegrees(side*78));
            band(IVORY,IVORY,0,0,r,r,fang*.63,0,r*.57,r*.57);
            band(TIP,TIP,fang*.63,0,r*.57,r*.57,fang,.33,.03,.03);
            pose.popPose();
        }
    }
    private void cup(double r){
        pose.mulPose(Axis.XP.rotationDegrees(90));
        band(CUP,CUP,-r*.22,0,r*.62,r*.62,r*.33,0,r,r);
        band(RIM,RIM,r*.33,0,r,r,r*.42,0,r*.66,r*.66);
        band(SOCKET,SOCKET,r*.42,0,r*.66,r*.66,r*.04,0,.03,.03);
    }
}
