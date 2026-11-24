package kr.randommorph.fabric.network;
import java.util.UUID;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MorphPayloadTest {
    @Test void cubeVariantsUseNewProtocolAndRoundtripWithExistingFlags(){
        assertEquals("randommorph:form_v7",MorphPayload.TYPE.id().toString());
        for(int size:new int[]{1,2,4}){
            int flags=kr.randommorph.fabric.core.MorphFlags.withCubeSize((15<<16)|(5<<13)|(40<<7)|127,size);
            var state=new MorphPayload(UUID.randomUUID(),"magma_cube",500,true,44,flags);
            var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),null);
            try{MorphPayload.CODEC.encode(buffer,state);assertEquals(state,MorphPayload.CODEC.decode(buffer));assertEquals(0,buffer.readableBytes());}
            finally{buffer.release();}
        }
        assertThrows(IllegalArgumentException.class,()->new MorphPayload(UUID.randomUUID(),"slime",1,false,1,3<<20));
    }
    @Test void everyWoolColorRoundtripsWithFaceAndGrazingBits(){
        for(int color=0;color<16;color++){
            int flags=kr.randommorph.fabric.core.MorphFlags.withColor((5<<13)|(40<<7)|127,color);
            var expected=new MorphPayload(UUID.randomUUID(),"sheep",1200,false,1,flags);
            var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),null);
            try{MorphPayload.CODEC.encode(buffer,expected);assertEquals(expected,MorphPayload.CODEC.decode(buffer));assertEquals(0,buffer.readableBytes());}
            finally{buffer.release();}
        }
    }
    @Test void physicalFlagsRoundtripAndAreBounded(){
        var state=new MorphPayload(UUID.randomUUID(),"sheep",400,false,33,(40<<7)|127);
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),null);
        try{MorphPayload.CODEC.encode(buffer,state);assertEquals(state,MorphPayload.CODEC.decode(buffer));}finally{buffer.release();}
        assertThrows(IllegalArgumentException.class,()->new MorphPayload(UUID.randomUUID(),"sheep",400,false,33,41<<7));
        assertThrows(IllegalArgumentException.class,()->new MorphPayload(UUID.randomUUID(),"sheep",400,false,33,-1));
    }
    @Test void realMinecraftBufferRoundtrip() {
        for(String form:new String[]{"", "endermite", "enderman"}) {
            var expected=new MorphPayload(UUID.randomUUID(),form,1200,true,17);
            var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),null);
            try {
                MorphPayload.CODEC.encode(buffer,expected);
                assertEquals(expected,MorphPayload.CODEC.decode(buffer));
                assertEquals(0,buffer.readableBytes());
            } finally { buffer.release(); }
        }
    }
    @Test void malformedStateRejected() {
        var uuid=UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,()->new MorphPayload(uuid,"PLAYER",0,false,1));
        assertThrows(IllegalArgumentException.class,()->new MorphPayload(uuid,"cow",-1,false,1));
        assertThrows(IllegalArgumentException.class,()->new MorphPayload(uuid,"cow",0,false,-1));
        assertThrows(IllegalArgumentException.class,()->new MorphPayload(uuid,"cow",72001,false,1));
    }
    @Test void truncatedWireRejected() {
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),null);
        try { buffer.writeLong(5);assertThrows(IndexOutOfBoundsException.class,()->MorphPayload.CODEC.decode(buffer)); }
        finally { buffer.release(); }
    }
}
