package kr.randommorph.fabric.core;

/** One ability's remaining wait; changing its duration preserves time already waited. */
public final class Cooldown {
    private int duration,remaining;
    public int remaining(){return remaining;}
    public void start(int ticks){check(ticks);duration=remaining=ticks;}
    public void tick(){if(remaining>0)remaining--;}
    public void duration(int ticks){
        check(ticks);
        if(remaining>0)remaining=Math.max(0,ticks-(duration-remaining));
        duration=ticks; // A ready ability stays ready when an operator increases its configured duration.
    }
    private static void check(int ticks){if(ticks<0||ticks>72_000)throw new IllegalArgumentException("cooldown: 0..72000 ticks");}
}
