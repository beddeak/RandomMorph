package kr.randommorph.fabric.network;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SensoryPayloadTest {
    @Test void invalidSenseCoordinatesAreRejected(){assertThrows(IllegalArgumentException.class,()->new SensePayload(Double.NaN,0,0,-1,false));assertThrows(IllegalArgumentException.class,()->new SensePayload(0,0,Double.POSITIVE_INFINITY,1,false));assertThrows(IllegalArgumentException.class,()->new SensePayload(30000100,0,0,-1,false));}
    @Test void senseLocationAllowsBlockAndEntityEvents(){assertDoesNotThrow(()->new SensePayload(0,64,-1,-1,false));assertDoesNotThrow(()->new SensePayload(0,64,1,10,true));}
    @Test void instinctRequiresValidTargetForMotion(){assertThrows(IllegalArgumentException.class,()->new InstinctPayload(-1,1));assertThrows(IllegalArgumentException.class,()->new InstinctPayload(10,3));assertDoesNotThrow(()->new InstinctPayload(-1,0));assertDoesNotThrow(()->new InstinctPayload(10,2));}
    @Test void soundAndSmellCodecRoundtrip(){
        for(var state:java.util.List.of(new SensePayload(1.25,64,-3.5,-1,false),new SensePayload(-8,72,10,234,true))){
            var b=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),null);
            try{SensePayload.CODEC.encode(b,state);assertEquals(state,SensePayload.CODEC.decode(b));assertEquals(0,b.readableBytes());}finally{b.release();}
        }
    }
    @Test void pursuitAvoidanceAndStopCodecRoundtrip(){
        for(var state:java.util.List.of(new InstinctPayload(-1,0),new InstinctPayload(125,1),new InstinctPayload(17,2))){
            var b=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),null);
            try{InstinctPayload.CODEC.encode(b,state);assertEquals(state,InstinctPayload.CODEC.decode(b));assertEquals(0,b.readableBytes());}finally{b.release();}
        }
    }
}
