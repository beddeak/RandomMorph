package kr.randommorph.fabric.morph;

/** AbstractCubeMob's visual jump/landing spring, without invoking its gameplay tick. */
public final class CubeAnimation {
    private final float damping;
    private boolean initialized,wasOnGround;
    private int lastTick;
    private float previousSquish,squish,targetSquish;

    public CubeAnimation(boolean magma){damping=magma?.9f:.6f;}
    public float previousSquish(){return previousSquish;}
    public float squish(){return squish;}
    public float targetSquish(){return targetSquish;}

    public void update(int playerTick,boolean onGround){
        if(!initialized || playerTick<lastTick || playerTick-lastTick>1){
            previousSquish=squish=targetSquish=0;
            wasOnGround=onGround;initialized=true;
        }else{
            if(playerTick==lastTick)return;
            // Preserve the native order: interpolate, detect contact transition, then damp.
            previousSquish=squish;
            squish+=(targetSquish-squish)*.5f;
            if(onGround&&!wasOnGround)targetSquish=-.5f;
            else if(!onGround&&wasOnGround)targetSquish=1;
            targetSquish*=damping;wasOnGround=onGround;
        }
        lastTick=playerTick;
    }
}
