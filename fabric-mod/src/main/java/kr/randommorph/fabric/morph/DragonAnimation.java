package kr.randommorph.fabric.morph;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory;
import net.minecraft.world.phys.Vec3;

/** Visual state only: a detached dragon must never execute its AI or multipart gameplay tick. */
public final class DragonAnimation {
    private final DragonFlightHistory history;
    private Vec3 lastPosition;
    private int lastTick;
    private float previousFlapTime,flapTime;

    public DragonAnimation(DragonFlightHistory history){this.history=history;}
    public float previousFlapTime(){return previousFlapTime;}
    public float flapTime(){return flapTime;}

    public void update(int playerTick,Vec3 position,float playerYaw){
        // The native dragon's head faces -Z at yaw 0; a player's forward direction is +Z.
        float dragonYaw=Mth.wrapDegrees(playerYaw+180);
        if(lastPosition==null || playerTick<lastTick || playerTick-lastTick>1 || position.distanceToSqr(lastPosition)>64*64){
            // Native record() seeds every sample on its first call. Never seed at world origin
            // or carry a previous world's height/turn across a teleport or replacement player.
            history.copyFrom(new DragonFlightHistory());
            history.record(position.y,dragonYaw);
            previousFlapTime=flapTime=0;
        }else{
            if(playerTick==lastTick)return; // Repeated frames and paused entity ticks cannot advance time.
            var movement=position.subtract(lastPosition);
            // EnderDragon.aiStep's flying flap rate, using observed motion for remote players too.
            float rate=.2f/((float)movement.horizontalDistance()*10+1)*(float)Math.pow(2,movement.y);
            previousFlapTime=flapTime%1; // Keep precision without interpolating backwards at the wrap.
            flapTime=previousFlapTime+rate;
            history.record(position.y,dragonYaw);
        }
        lastTick=playerTick;lastPosition=position;
    }
}
