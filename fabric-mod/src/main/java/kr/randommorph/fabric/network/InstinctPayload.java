package kr.randommorph.fabric.network;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record InstinctPayload(int target,int mode) implements CustomPacketPayload {
    public static final Type<InstinctPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("randommorph","instinct_v1"));
    public InstinctPayload{if(target< -1||mode<0||mode>2||mode!=0&&target<0)throw new IllegalArgumentException("Invalid instinct");}
    public static final StreamCodec<RegistryFriendlyByteBuf,InstinctPayload> CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.target);b.writeByte(p.mode);},b->new InstinctPayload(b.readVarInt(),b.readUnsignedByte()));
    @Override public Type<InstinctPayload> type(){return TYPE;}
}
