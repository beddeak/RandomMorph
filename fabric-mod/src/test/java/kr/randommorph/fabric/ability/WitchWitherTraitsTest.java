package kr.randommorph.fabric.ability;
import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import kr.randommorph.fabric.morph.MorphRegistry;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
class WitchWitherTraitsTest {
    @BeforeAll static void init(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}
    @Test void thrownPoolIncludesEveryVanillaPotionWithoutEffectFiltering(){
        var pool=WitchActions.potions();long count=BuiltInRegistries.POTION.keySet().stream().filter(id->id.getNamespace().equals("minecraft")).count();
        assertEquals(count,pool.size());assertEquals(pool.size(),new HashSet<>(pool).size());
        for(var name:List.of("water","awkward","healing","harming","poison","long_poison","strong_healing"))assertTrue(pool.stream().anyMatch(p->BuiltInRegistries.POTION.getKey(p.value()).getPath().equals(name)),name);
    }
    @Test void everyPotionCanBeSelectedWithComparableFrequency(){
        var count=new HashMap<Object,Integer>();var random=RandomSource.create(46);
        for(int i=0;i<30000;i++)count.merge(WitchActions.randomPotion(random),1,Integer::sum);
        assertEquals(WitchActions.potions().size(),count.size());double expected=30000d/count.size();
        for(var value:count.values())assertTrue(Math.abs(value-expected)<expected*.25,"Unexpected selection bias: "+value);
        assertEquals(Active.WITCH_HEAL,MorphRegistry.find("witch").secondary());
    }
    @Test void witherArmorBeginsAtExactlyHalfHealth(){
        assertTrue(WitherTraits.powered(150,300));assertTrue(WitherTraits.powered(1,300));assertFalse(WitherTraits.powered(150.01f,300));assertFalse(WitherTraits.powered(0,0));
        assertEquals(1,WitherTraits.radius(.9f));assertEquals(3,WitherTraits.height(3.5f));
    }
}
