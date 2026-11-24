package kr.randommorph.fabric.network;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server authority only. Empty form is the central clear message. */
public record MorphPayload(UUID player, String form, int remainingTicks, boolean paused, long revision, int flags) implements CustomPacketPayload {
    public static final Type<MorphPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("randommorph", "form_v7"));
    public MorphPayload(UUID player,String form,int remainingTicks,boolean paused,long revision){this(player,form,remainingTicks,paused,revision,0);}
    public MorphPayload {
        if (player == null || form == null || !form.matches("[a-z_]{0,32}") || remainingTicks < 0 || remainingTicks > 72000 || revision < 0 || !kr.randommorph.fabric.core.MorphFlags.valid(flags))
            throw new IllegalArgumentException("Invalid morph state");
    }
    public static final StreamCodec<RegistryFriendlyByteBuf,MorphPayload> CODEC = new StreamCodec<>() {
        public MorphPayload decode(RegistryFriendlyByteBuf buf) {
            return new MorphPayload(buf.readUUID(), buf.readUtf(32), buf.readVarInt(), buf.readBoolean(), buf.readVarLong(),buf.readVarInt());
        }
        public void encode(RegistryFriendlyByteBuf buf, MorphPayload state) {
            buf.writeUUID(state.player); buf.writeUtf(state.form,32); buf.writeVarInt(state.remainingTicks);
            buf.writeBoolean(state.paused); buf.writeVarLong(state.revision);buf.writeVarInt(state.flags);
        }
    };
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
