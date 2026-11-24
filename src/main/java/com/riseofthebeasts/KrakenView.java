package com.riseofthebeasts;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import static com.riseofthebeasts.BeastState.*;

/** Nearby players receive one pose per animal, even when the mantle's own chunk is asleep. */
record KrakenView(UUID id,Vec3 origin,int age,Move move,int remaining,Section striker,boolean removed) {
    private static final String VERSION="1";
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(Beasts.id("kraken_view"),
        ()->VERSION,VERSION::equals,VERSION::equals);
    static void register(){
        CHANNEL.messageBuilder(KrakenView.class,0,NetworkDirection.PLAY_TO_CLIENT)
            .encoder(KrakenView::write).decoder(KrakenView::read).consumerMainThread(KrakenView::handle).add();
    }
    private void write(FriendlyByteBuf b){
        b.writeUUID(id);b.writeDouble(origin.x);b.writeDouble(origin.y);b.writeDouble(origin.z);
        b.writeVarInt(age);b.writeEnum(move);b.writeVarInt(remaining);b.writeInt(striker.index());b.writeInt(striker.lane());b.writeBoolean(removed);
    }
    private static KrakenView read(FriendlyByteBuf b){
        return new KrakenView(b.readUUID(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readVarInt(),b.readEnum(Move.class),
            b.readVarInt(),new Section(b.readInt(),b.readInt()),b.readBoolean());
    }
    private static void handle(KrakenView update,Supplier<NetworkEvent.Context> context){
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->KrakenDisplay.receive(update));
        context.get().setPacketHandled(true);
    }
    static void send(Animal a,ServerPlayer p,boolean removed){
        CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new KrakenView(a.id,a.origin,a.clock,a.move,a.remaining,a.striker,removed));
    }
    static boolean inRange(Vec3 origin,Vec3 player){
        // A coarse bounding box is sufficient for the visual model. Collision still uses nearby physical segments.
        double dx=Math.max(0,Math.abs(player.x-origin.x)-150);
        double dz=Math.max(0,Math.max(origin.z-KrakenBody.MANTLE-player.z,player.z-origin.z-KrakenBody.REACH));
        return dx*dx+dz*dz<510*510 && Math.abs(player.y-origin.y)<400;
    }
}
