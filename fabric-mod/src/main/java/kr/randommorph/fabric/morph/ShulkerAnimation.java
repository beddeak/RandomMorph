package kr.randommorph.fabric.morph;
/** Native peek approach without ticking the detached mob's AI or colliding with other entities. */
public final class ShulkerAnimation {
    private int tick=Integer.MIN_VALUE;
    private float previous,current;
    public void update(int ownerTick,boolean closed){
        if(tick==ownerTick)return;
        tick=ownerTick;previous=current;float target=closed?0:1;
        current=current<target?Math.min(target,current+.05f):Math.max(target,current-.05f);
    }
    public float previous(){return previous;}
    public float current(){return current;}
}
