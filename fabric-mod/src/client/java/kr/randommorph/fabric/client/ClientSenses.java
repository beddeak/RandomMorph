package kr.randommorph.fabric.client;
import java.util.*;
import kr.randommorph.fabric.morph.*;
import kr.randommorph.fabric.network.SensePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;
/** Opaque world mask with short-lived heard/smelled direction markers; vanilla HUD remains above it. */
public final class ClientSenses {
    private record Echo(SensePayload sound,int expires){}
    private static final List<Echo> ECHOES=new ArrayList<>();private static int tick;
    private ClientSenses(){}
    public static void receive(SensePayload sound){ClientMorphs.refreshContext();if(ECHOES.size()>=16)ECHOES.removeFirst();ECHOES.add(new Echo(sound,tick+80));}
    public static void clear(){ECHOES.clear();tick=0;}
    public static void tick(){tick++;var p=Minecraft.getInstance().player;if(p==null||!p.isAlive()||((MorphAccess)p).randommorph$form()==null||!((MorphAccess)p).randommorph$form().id().equals("warden"))ECHOES.clear();else ECHOES.removeIf(e->e.expires<=tick);}
    public static void draw(GuiGraphicsExtractor g){
        ClientMorphs.refreshContext();
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||!p.isAlive()||p.isSpectator())return;var profile=((MorphAccess)p).randommorph$form();if(profile==null||!profile.id().equals("warden"))return;
        g.fill(0,0,g.guiWidth(),g.guiHeight(),0xFF000000);g.nextStratum();
        for(var echo:ECHOES){
            var s=echo.sound;Vec3 position=new Vec3(s.x(),s.y(),s.z());
            var entity=mc.level==null||s.entityId()<0?null:mc.level.getEntity(s.entityId());
            if(entity!=null&&entity.isAlive())position=entity.position().add(0,entity.getBbHeight()*.5,0);
            Vec3 delta=position.subtract(p.getEyePosition());double distance=delta.length();
            double yaw=Math.atan2(-delta.x,delta.z),offset=Math.toDegrees(yaw)-p.getYRot();offset=net.minecraft.util.Mth.wrapDegrees(offset);
            double pitch=-Math.toDegrees(Math.atan2(delta.y,Math.max(.001,delta.horizontalDistance())))-p.getXRot();
            int x=(int)(g.guiWidth()*(.5+Math.clamp(offset/120,-.43,.43))),y=(int)(g.guiHeight()*(.5+Math.clamp(pitch/120,-.32,.32)));
            int age=80-(echo.expires-tick),size=12+(age%20)/2,color=s.smell()?0xFFDD9DF1:0xFF61DCCE;
            g.outline(x-size/2,y-size/2,size,size,color);
            String text=(Math.abs(offset)>90?"뒤쪽 ":"")+(s.smell()?"냄새 ":"진동 ")+(int)Math.round(distance)+"m";
            g.centeredText(mc.font,text,x,y+size/2+3,color);
        }
        g.centeredText(mc.font,"소리와 냄새에 집중하세요",g.guiWidth()/2,12,0xFF94C6C1);
        g.nextStratum();
    }
}
