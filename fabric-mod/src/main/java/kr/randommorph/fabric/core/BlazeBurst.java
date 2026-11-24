package kr.randommorph.fabric.core;
/** Native ranged cadence: 60 ticks of charge, three shots six ticks apart, then end charge. */
public final class BlazeBurst {
    private int wait,shots;
    public boolean active(){return wait>0;}
    public boolean begin(){if(active())return false;wait=60;shots=3;return true;}
    public boolean tick(){
        if(!active()||--wait>0)return false;
        if(shots==0){clear();return false;}
        shots--;wait=6;return true;
    }
    public void clear(){wait=0;shots=0;}
}
