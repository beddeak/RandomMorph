package kr.randommorph.fabric.core;

import java.util.function.IntUnaryOperator;

/** Natural resources survive remorphing; bee injury belongs to the current living bee form. */
public final class AnimalState {
    public static final int STUNG=MorphFlags.STUNG, SHEARED=MorphFlags.SHEARED;
    private boolean sheared, stung,pumpkinRemoved;
    private int eggTicks=-1, stingTicks,woolColor;
    private String form="";
    public void form(String next) {
        if(!form.equals(next)){resetSting();form=next;}
    }
    public void resetSting(){stung=false;stingTicks=0;}
    public boolean stung(){return stung;}
    public int stingTicks(){return stingTicks;}
    public boolean sting(){if(!form.equals("bee")||stung)return false;stung=true;return true;}
    /** Native Bee checks every fifth tick with a shrinking 1..1200 random bound. */
    public boolean stingDeath(IntUnaryOperator random) {
        if(!form.equals("bee")||!stung)return false;
        if(++stingTicks%5!=0)return false;
        return random.applyAsInt(Math.clamp(1200-stingTicks,1,1200))==0;
    }
    public int woolColor(){return woolColor;}
    public boolean dye(int color){if(sheared||color<0||color>15||color==woolColor)return false;woolColor=color;return true;}
    public boolean sheared(){return sheared;}
    public boolean shear(){if(sheared)return false;sheared=true;return true;}
    public void ate(){sheared=false;}
    public boolean pumpkinRemoved(){return pumpkinRemoved;}
    public boolean shearPumpkin(){if(!form.equals("snow_golem")||pumpkinRemoved)return false;pumpkinRemoved=true;return true;}
    public int eggTicks(){return eggTicks;}
    public boolean chickenTick(IntUnaryOperator random) {
        if(!form.equals("chicken"))return false;
        if(eggTicks<0)eggTicks=random.applyAsInt(6000)+6000;
        if(--eggTicks>0)return false;
        eggTicks=random.applyAsInt(6000)+6000;
        return true;
    }
    public int flags(){return (form.equals("bee")&&stung?STUNG:0)|(form.equals("sheep")?MorphFlags.withColor(sheared?SHEARED:0,woolColor):0)|(form.equals("snow_golem")&&pumpkinRemoved?SHEARED:0);}
    public void restoreProduction(boolean woolRemoved,int eggRemaining) {
        restoreProduction(woolRemoved,eggRemaining,0);
    }
    public void restoreProduction(boolean woolRemoved,int eggRemaining,int color){
        restoreProduction(woolRemoved,eggRemaining,color,false);
    }
    public void restoreProduction(boolean woolRemoved,int eggRemaining,int color,boolean removedPumpkin){
        sheared=woolRemoved;eggTicks=eggRemaining>=0&&eggRemaining<12000?eggRemaining:-1;woolColor=color>=0&&color<=15?color:0;
        pumpkinRemoved=removedPumpkin;
    }
    public void copyFrom(AnimalState old,boolean alive) {
        restoreProduction(old.sheared,old.eggTicks,old.woolColor,old.pumpkinRemoved);
        form=alive?old.form:"";stung=alive&&old.stung;stingTicks=alive?old.stingTicks:0;
    }
}
