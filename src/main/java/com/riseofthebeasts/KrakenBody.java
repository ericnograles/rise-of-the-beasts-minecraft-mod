package com.riseofthebeasts;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.*;
import static com.riseofthebeasts.BeastState.*;

final class KrakenBody {
    static final int MANTLE=480, REACH=620, LENGTH=MANTLE+REACH, HEIGHT=200, WINDUP=32;
    static final int ARMS=8, SIGHT=245;
    static int reach(int lane) {
        return lane==0 || lane==4?REACH:350+(lane%3)*36;
    }
    static int start(Section s) {return (s.index()-1)*16;}
    static int span(Section s) {return Math.min(16,reach(s.lane())-start(s));}
    static double mantleRadius(double z) {
        double t=Math.max(0,Math.min(1,(z+MANTLE)/MANTLE));
        return Math.max(.6, t<.5?HEIGHT*.5*Math.sin(t*Math.PI):48+(HEIGHT*.5-48)*Math.cos((t-.5)*Math.PI));
    }
    static double fin(double z) {
        double t=(z+MANTLE)/142;
        if(t<0 || t>=1)return 0;
        return t<.76?131*t/.76:131*(1-t)/.24;
    }
    static double armRadius(double z,int lane) {
        double t=Math.max(0,Math.min(1,z/reach(lane)));
        double club=(lane%4==0)?5*Math.exp(-Math.pow((t-.945)/.037,2)):0;
        return Math.max(.45,13*Math.pow(1-t,.69)+club);
    }
    static double width(Section s) {
        if(s.index()<0)return 2*mantleRadius(s.index()*16+8);
        return s.index()==0?96:2*armRadius(start(s)+span(s)*.5,s.lane());
    }
    static double submerge(Move move,double progress) {
        return move==Move.INTRO?-166*(1-Animal.ease(progress/.9)):
               move==Move.DEATH?-182*Animal.ease(progress):0;
    }
    private static Vec3 socket(Animal a) {
        return a.origin.add(0,36+submerge(a.move,a.sceneProgress()),0);
    }
    static Vec3 arm(Animal a,double z,int lane) {
        return socket(a).add(armOffset(z,lane,a.clock,a.move,a.sceneProgress(),a.remaining,a.striker));
    }
    static Vec3 armOffset(double z,int lane,double clock,Move move,double progress,double remaining,Section striker) {
        double t=z/reach(lane), angle=lane*Math.PI/4;
        double phase=clock*.052-z*.029+angle;
        double open=move==Move.INTRO?.20+.80*Animal.ease(progress):1;
        double death=move==Move.DEATH?Animal.ease(progress):0;
        // The two outer feeding tentacles curl their clubs in opposite directions.
        double curl=(lane%4==0?1.1:.65)*Animal.ease((t-.57)/.43);
        angle+=curl*Math.sin(phase*.5)+death*3.9*t;
        double spread=(1-Math.exp(-z/105))*open*(1-.85*death);
        double x=Math.cos(angle)*(35+spread*(100+25*Math.sin(phase*.54)));
        double y=Math.sin(angle)*(32+spread*38)-spread*29;
        double wave=Math.sin(phase)*spread;
        double lash=0;
        if(move==Move.SWEEP && lane==striker.lane()){
            double beat=1-remaining/(double)WINDUP;
            lash=Math.sin(beat*Math.PI*2)*Math.sin(beat*Math.PI)
                *Math.exp(-Math.pow((z-start(striker))/67,2));
        }
        return new Vec3(x+wave*15+lash*33,y+wave*14+lash*27,z);
    }
    static Vec3 axis(Animal a,Section s) {
        int z=start(s);
        return arm(a,z+span(s),s.lane()).subtract(arm(a,z,s.lane()));
    }
    static Vec3 center(Animal a,Section s) {
        if(s.index()<=0)return socket(a).add(0,0,s.index()<0?s.index()*16+8:0);
        Vec3 first=arm(a,start(s),s.lane());
        return first.add(arm(a,start(s)+span(s),s.lane())).scale(.5);
    }
    static AABB bounds(Giant g) {
        int i=g.getEntityData().get(Giant.INDEX);
        double x=g.getX(),y=g.getY(),z=g.getZ();
        if(i==0)return new AABB(x-50,y-50,z-9,x+50,y+50,z+21);
        if(i<0){
            double a=i*16,b=a+16,h=Math.max(mantleRadius(a),mantleRadius(b));
            double w=Math.max(h,Math.max(fin(a),fin(b)))+1;
            return new AABB(x-w,y-h,z-8,x+w,y+h,z+8);
        }
        double r=g.entityDataWidth()*.63+1;
        double dx=Math.abs(g.getEntityData().get(Giant.ARM_DX))*.5+r;
        double dy=Math.abs(g.getEntityData().get(Giant.ARM_DY))*.5+r;
        double dz=g.getEntityData().get(Giant.SPAN)*.5+1;
        return new AABB(x-dx,y-dy,z-dz,x+dx,y+dy,z+dz);
    }
    static void nearby(ServerLevel level,Animal a,Vec3 player,Set<Section> wanted) {
        Vec3 d=player.subtract(a.origin);
        if(Math.abs(d.x)>SIGHT+170 || Math.abs(d.y)>SIGHT+210 || d.z < -MANTLE-SIGHT || d.z>REACH+SIGHT)return;
        int first=Math.max(-MANTLE/16,(int)Math.floor((d.z-SIGHT)/16)-1);
        int last=Math.min((REACH+15)/16,(int)Math.ceil((d.z+SIGHT)/16)+1);
        for(int i=first;i<=last;i++)for(int lane=0;lane<(i>0?ARMS:1);lane++){
            Section key=new Section(i,lane);
            if(i>0 && start(key)>=reach(lane))continue;
            Vec3 pos=center(a,key);
            if(player.distanceToSqr(pos)<=SIGHT*SIGHT && level.isPositionEntityTicking(BlockPos.containing(pos))){
                wanted.add(key);
            }
        }
    }
    static void foam(ServerLevel level,Animal a) {
        if(!a.active.containsKey(new Section(0,0)))return;
        double t=a.sceneProgress(),radius=a.move==Move.DEATH?92*(1-.78*t):26+42*t;
        for(int i=0;i<12;i++){
            double angle=i*Math.PI/6+t*11;
            level.sendParticles(ParticleTypes.SPLASH,a.origin.x+Math.cos(angle)*radius,a.origin.y+1.2,
                a.origin.z+Math.sin(angle)*radius,4,1.5,.25,1.5,.12);
        }
    }
}
