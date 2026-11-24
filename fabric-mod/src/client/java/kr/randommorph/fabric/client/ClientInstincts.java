package kr.randommorph.fabric.client;
import kr.randommorph.fabric.network.InstinctPayload;
import kr.randommorph.fabric.morph.MorphAccess;
import kr.randommorph.fabric.ability.AbilityHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
public final class ClientInstincts {
    private static int target=-1,mode,remaining;
    private ClientInstincts(){}
    public static void receive(InstinctPayload p){ClientMorphs.refreshContext();target=p.target();mode=p.mode();remaining=30;}
    public static void clear(){target=-1;mode=0;remaining=0;}
    public static void tick(){if(remaining>0)remaining--;else clear();}
    public static void apply(LocalPlayer p){
        ClientMorphs.refreshContext();
        if(AbilityHooks.immobile(p)){if(kr.randommorph.fabric.ability.ShulkerAttachment.shulker(p)){p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);p.setSprinting(false);}p.xxa=0;p.zza=0;p.setJumping(false);var keys=p.input.keyPresses;p.input.keyPresses=new net.minecraft.world.entity.player.Input(false,false,false,false,false,keys.shift(),false);return;}
        var mc=Minecraft.getInstance();if(remaining<=0||mode==0||mc.level==null||mc.gui.screen()!=null||p.isPassenger()||p.isSpectator()||!p.isAlive()||((MorphAccess)p).randommorph$form()==null)return;
        var e=mc.level.getEntity(target);if(e==null||!e.isAlive())return;
        var direction=e.position().subtract(p.position()).multiply(1,0,1).normalize();if(mode==2)direction=direction.scale(-1);
        double yaw=Math.toRadians(p.getYRot());
        p.xxa=(float)(direction.x*Math.cos(yaw)+direction.z*Math.sin(yaw));p.zza=(float)(direction.z*Math.cos(yaw)-direction.x*Math.sin(yaw));
        if(mode==1&&p.distanceToSqr(e)<2){p.xxa=0;p.zza=0;}
        if(p.horizontalCollision&&p.onGround())p.setJumping(true);
        var keys=p.input.keyPresses;
        p.input.keyPresses=new net.minecraft.world.entity.player.Input(p.zza>.05,p.zza< -.05,p.xxa>.05,p.xxa< -.05,keys.jump()||p.horizontalCollision&&p.onGround(),keys.shift(),false);
        if(p.isMovingSlowly()){p.xxa*=.3f;p.zza*=.3f;}
    }
}
