package kr.randommorph.fabric.morph;
import net.minecraft.util.Mth;
/** Per-body visual motion only; neither AI nor native entity ticks are executed. */
public final class AquaticAnimation {
    private int lastTick=Integer.MIN_VALUE;
    private float phase,tail,tailOld,spikes=1,spikesOld=1,tentacle,tentacleOld,pitch,pitchOld,roll,rollOld;
    public boolean update(int tick,boolean wet,boolean moving,double vertical,double horizontal){
        if(lastTick==tick)return false;
        boolean reset=lastTick==Integer.MIN_VALUE||tick<lastTick||tick-lastTick>20;
        lastTick=tick;
        if(reset){phase=0;tail=tailOld=0;spikes=spikesOld=1;tentacle=tentacleOld=0;pitch=pitchOld=wet?0:-90;roll=rollOld=0;}
        tailOld=tail;spikesOld=spikes;tentacleOld=tentacle;pitchOld=pitch;rollOld=roll;
        phase=(phase+.15f)%(Mth.PI*2);
        if(wet){float fraction=phase/Mth.PI;tentacle=phase<Mth.PI?Mth.sin(fraction*fraction*Mth.PI)*Mth.PI*.25f:0;}
        else tentacle=Mth.abs(Mth.sin(phase))*Mth.PI*.25f;
        tail+=wet?(moving?.5f:.1f):2;
        spikes=Mth.lerp(.06f,spikes,wet&&moving?0:1);
        float target=wet?(moving?(float)(-Math.toDegrees(Math.atan2(vertical,horizontal))):0):-90;
        pitch=Mth.lerp(wet?.1f:.02f,pitch,target);
        roll+=wet&&moving?.3f:0;
        return true;
    }
    public float tail(){return tail;}public float tailOld(){return tailOld;}
    public float spikes(){return spikes;}public float spikesOld(){return spikesOld;}
    public float tentacle(){return tentacle;}public float tentacleOld(){return tentacleOld;}
    public float pitch(){return pitch;}public float pitchOld(){return pitchOld;}
    public float roll(){return roll;}public float rollOld(){return rollOld;}
}
