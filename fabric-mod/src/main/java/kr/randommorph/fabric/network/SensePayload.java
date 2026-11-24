package kr.randommorph.fabric.network;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
/** Owner-only ephemeral sound/smell location, never a global glowing effect. */
public record SensePayload(double x,double y,double z,int entityId,boolean smell) implements CustomPacketPayload {
    public static final Type<SensePayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("randommorph","sense_v1"));
    public SensePayload{if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||Math.abs(x)>30000064||Math.abs(z)>30000064||Math.abs(y)>1000000||entityId< -1)throw new IllegalArgumentException("Invalid sense location");}
    public static final StreamCodec<RegistryFriendlyByteBuf,SensePayload> CODEC=StreamCodec.of((b,p)->{b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);b.writeVarInt(p.entityId);b.writeBoolean(p.smell);},b->new SensePayload(b.readDouble(),b.readDouble(),b.readDouble(),b.readVarInt(),b.readBoolean()));
    @Override public Type<SensePayload> type(){return TYPE;}
}
