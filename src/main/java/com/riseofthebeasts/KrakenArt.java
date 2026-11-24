package com.riseofthebeasts;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import static com.riseofthebeasts.BeastState.*;

/** Draw the sketch's kite-shaped fins, tapering mantle, square eyes and trailing clubs. */
record KrakenArt(PoseStack pose, VertexConsumer out, int light) {
    private static final int RED=0, FLANK=1, CREAM=2, RUST=3, CUP=4, SOCKET=5, AMBER=6, INK=7, FOAM=8, WATER=9;
    // Both ring resolutions divide 24. Reuse their unit circle for every segment and sucker.
    private static final double[] X=new double[25],Y=new double[25];
    static {
        for(int i=0;i<X.length;i++){X[i]=Math.cos(i*Math.PI/12);Y[i]=Math.sin(i*Math.PI/12);}
    }
    private void vert(int tile,Vec3 p,float u,float v,Vec3 n){
        var m=pose.last();
        out.vertex(m.pose(),(float)p.x,(float)p.y,(float)p.z).color(255,255,255,255)
            .uv((tile%4*32+1+u*29)/128,(tile/4*32+1+v*29)/128).overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(tile==FOAM || tile==AMBER?Math.max(light,0xc000c0):light)
            .normal(m.normal(),(float)n.x,(float)n.y,(float)n.z).endVertex();
    }
    private void quad(int tile,Vec3 a,Vec3 b,Vec3 c,Vec3 d){
        Vec3 n=b.subtract(a).cross(d.subtract(a)).normalize();
        vert(tile,a,0,0,n);vert(tile,b,1,0,n);vert(tile,c,1,1,n);vert(tile,d,0,1,n);
    }
    private void tube(Vec3 a,double r,Vec3 b,double s,boolean mantle){
        int step=mantle?1:2;
        for(int i=0;i<24;i+=step){
            int j=i+step;
            double height=(Y[i]+Y[j])*.5;
            int tile=height<-.50?CREAM:height<.10?FLANK:RED;
            quad(tile,a.add(r*X[j],r*Y[j],0),b.add(s*X[j],s*Y[j],0),
                b.add(s*X[i],s*Y[i],0),a.add(r*X[i],r*Y[i],0));
        }
    }
    private void disk(int tile,double rx,double ry,double z,double inner){
        for(int i=0;i<24;i+=2){
            int j=i+2;
            quad(tile,new Vec3(X[i]*rx,Y[i]*ry,z),new Vec3(X[j]*rx,Y[j]*ry,z),
                new Vec3(X[j]*rx*inner,Y[j]*ry*inner,z-.28),new Vec3(X[i]*rx*inner,Y[i]*ry*inner,z-.28));
        }
    }
    private void square(int tile,double w,double h,double z){
        quad(tile,new Vec3(-w,-h,z),new Vec3(w,-h,z),new Vec3(w,h,z),new Vec3(-w,h,z));
    }
    void draw(KrakenView view,double clock,double t,double remaining,Vec3 eye){
        Move move=view.move();
        Section attack=view.striker();
        for(int index=-KrakenBody.MANTLE/16;index<0;index++){
            pose.pushPose();pose.translate(0,0,index*16+8);mantle(index);pose.popPose();
        }
        face(move==Move.INK?Math.sin(Math.PI*Math.max(0,remaining)/KrakenBody.WINDUP):0);
        for(int lane=0;lane<KrakenBody.ARMS;lane++){
            int reach=KrakenBody.reach(lane);
            Vec3 first=KrakenBody.armOffset(0,lane,clock,move,t,remaining,attack);
            for(int z=0;z<reach;){
                boolean close=first.distanceToSqr(eye)<170*170;
                int next=Math.min(reach,z+(close?16:32));
                Vec3 last=KrakenBody.armOffset(next,lane,clock,move,t,remaining,attack);
                Vec3 center=first.add(last).scale(.5);
                pose.pushPose();pose.translate(center.x,center.y,center.z);
                arm(lane,z,next,last.subtract(first).scale(.5),close);
                pose.popPose();first=last;z=next;
            }
        }
        if(move==Move.DEATH)vortex(t);
    }
    private void mantle(int index){
        double z=index*16,r=KrakenBody.mantleRadius(z),s=KrakenBody.mantleRadius(z+16);
        tube(new Vec3(0,0,-8),r,new Vec3(0,0,8),s,true);
        for(int side:new int[]{-1,1}){
            if((index+30)%3==1 && z> -415 && z< -88){
                double angle=side>0?.42:Math.PI-.42,top=angle+side*.26;
                quad(FLANK,new Vec3((r+.45)*Math.cos(angle),(r+.45)*Math.sin(angle),-8),
                    new Vec3((r+.45)*Math.cos(top),(r+.45)*Math.sin(top),-8),
                    new Vec3((s+.45)*Math.cos(top+side*.15),(s+.45)*Math.sin(top+side*.15),8),
                    new Vec3((s+.45)*Math.cos(angle+side*.20),(s+.45)*Math.sin(angle+side*.20),8));
            }
            double f=KrakenBody.fin(z),h=KrakenBody.fin(z+16);
            for(int face:new int[]{-1,1}){
                quad(face>0?RUST:CREAM,new Vec3(side*r*.58,face*r*.35,-8),new Vec3(side*Math.max(r*.64,f),0,-8),
                    new Vec3(side*Math.max(s*.64,h),0,8),new Vec3(side*s*.58,face*s*.35,8));
            }
        }
    }
    private void face(double gape){
        tube(new Vec3(0,0,-4),48,new Vec3(0,0,9),39,true);
        tube(new Vec3(0,0,9),39,new Vec3(0,0,17),12,true);
        disk(INK,12,12,17,.02);
        // The eyes sit behind the arm sockets, on opposite shoulders of the mantle.
        double radius=KrakenBody.mantleRadius(-55);
        for(int side:new int[]{-1,1}){
            pose.pushPose();pose.translate(side*radius*.805,radius*.615,-55);
            pose.mulPose(Axis.YP.rotationDegrees(side*87));pose.mulPose(Axis.XP.rotationDegrees(-37));
            square(RUST,16,14,.8);square(INK,14.2,12.2,1.8);square(CREAM,12.8,10.8,2.6);
            square(AMBER,8.9,8.6,3.1);square(INK,5.6,6.4,3.7);
            pose.translate(-2.4,3.1,0);square(FOAM,1.7,1.5,4.1);pose.popPose();
            double y=side*(8+gape*2);
            quad(CREAM,new Vec3(-9,y,16),new Vec3(9,y,16),new Vec3(3,side*(2+gape*3),22),new Vec3(-3,side*(2+gape*3),22));
        }
    }
    private void arm(int lane,int z,int next,Vec3 half,boolean close){
        double r=KrakenBody.armRadius(z,lane),s=KrakenBody.armRadius(next,lane);
        tube(half.scale(-1),r,half,s,false);
        if(!close)return;
        for(int side:new int[]{-1,1})for(int row=0;row<2;row++){
            double t=row*.5+.22,rad=r+(s-r)*t;
            Vec3 pos=half.scale(2*t-1).add(side*rad*.76,-rad*.62,0);
            pose.pushPose();pose.translate(pos.x,pos.y,pos.z);
            pose.mulPose(Axis.YP.rotationDegrees(side*90));pose.mulPose(Axis.XP.rotationDegrees(35));
            disk(CUP,rad*.35,Math.min(4,rad*.35),1,.56);disk(SOCKET,rad*.20,Math.min(2.25,rad*.20),.78,.03);
            pose.popPose();
        }
    }
    private void vortex(double t){
        double seaY=-35-KrakenBody.submerge(Move.DEATH,t);
        double fade=Math.sin(Math.PI*t),radius=112*(.65+.35*fade);
        for(int arm=0;arm<6;arm++)for(int k=0;k<36;k++){
            double a=(k+.4)/36,b=(k+1.4)/36;
            double angle=arm*Math.PI/3-t*10;
            double width=(1+6*a)*fade;
            quad(arm%2==0?FOAM:WATER,spiral(a,angle,radius,seaY,-width),spiral(a,angle,radius,seaY,width),
                spiral(b,angle,radius,seaY,width),spiral(b,angle,radius,seaY,-width));
        }
    }
    private static Vec3 spiral(double t,double angle,double radius,double y,double width){
        double r=5+t*radius+width,theta=angle+t*4.8;
        return new Vec3(Math.cos(theta)*r,y+1.8*t+.75*Math.sin(theta*3),Math.sin(theta)*r);
    }
}
