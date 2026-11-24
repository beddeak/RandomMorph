package kr.randommorph.verify;
import net.minecraft.core.*;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import kr.randommorph.fabric.morph.MorphBodies;
import kr.randommorph.fabric.core.MorphFlags;
import kr.randommorph.fabric.mixin.ShulkerAccess;
/** Reproduces original audio/event duplication and checks actual fixed native data writes. */
public final class ShulkerSoundProbe {
    public static class Body extends Shulker {
        public int sounds,events;private AttributeInstance armor;
        private Body(EntityType<? extends Shulker> type,Level level){super(type,level);}
        @Override public void playSound(SoundEvent sound,float volume,float pitch){sounds++;}
        @Override public void gameEvent(Holder<GameEvent> event){events++;}
        @Override public void onSyncedDataUpdated(EntityDataAccessor<?> data){}
        @Override public AttributeInstance getAttribute(Holder<Attribute> attribute){if(armor==null)armor=new AttributeInstance(Attributes.ARMOR,a->{});return armor;}
    }
    public static void run(String side)throws Exception{
        var body=NativeEntityFixtures.allocate(Body.class);NativeEntityFixtures.initializeData(body);
        var level=Entity.class.getDeclaredField("level");level.setAccessible(true);level.set(body,NativeEntityFixtures.allocate(net.minecraft.server.level.ServerLevel.class));
        var original=Shulker.class.getDeclaredMethod("setRawPeekAmount",int.class);original.setAccessible(true);
        for(int i=0;i<40;i++)original.invoke(body,0);
        if(body.sounds!=40||body.events!=40)throw new AssertionError("Native duplicate sound/event bug not reproduced");
        body.sounds=0;body.events=0;
        int flags=MorphFlags.withFace(1,Direction.NORTH);
        for(int i=0;i<1000;i++)MorphBodies.flags(body,flags);
        if(body.sounds!=0||body.events!=0||body.getEntityData().get(ShulkerAccess.randommorph$peekData())!=0||body.getAttachFace()!=Direction.NORTH)throw new AssertionError("Detached state repeats audio or changes face");
        for(int i=0;i<1000;i++)MorphBodies.flags(body,flags&~1);
        if(body.sounds!=0||body.events!=0||body.getEntityData().get(ShulkerAccess.randommorph$peekData())!=100)throw new AssertionError("Opening state copy produces audio");
        System.out.println("RANDOMMORPH_SHULKER_AUDIO_OK side="+side+" copies=2000 reproduced=40; native data, no world/game started");
    }
}
