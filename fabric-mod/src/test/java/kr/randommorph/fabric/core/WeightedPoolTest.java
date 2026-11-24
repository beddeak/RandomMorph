package kr.randommorph.fabric.core;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WeightedPoolTest {
    @Test void disabledAndZeroNeverAppear(){var pool=Map.of("cow",1d,"wither",0d);for(int i=0;i<100;i++)assertEquals("cow",WeightedPool.choose(new Random(i),pool,null));}
    @Test void oneEnabledSpeciesStillWorksWhenItIsCurrent(){assertEquals("cow",WeightedPool.choose(new Random(),Map.of("cow",1d),"cow"));}
    @Test void rerollsExcludeCurrentIfAnAlternativeExists(){assertEquals("bee",WeightedPool.choose(new Random(),Map.of("cow",10d,"bee",1d),"cow"));}
    @Test void enormousFiniteWeightsDoNotOverflow(){var r=new Random(123);var picked=new HashSet<String>();for(int i=0;i<100;i++)picked.add(WeightedPool.choose(r,Map.of("cow",Double.MAX_VALUE,"bee",Double.MAX_VALUE),null));assertEquals(Set.of("cow","bee"),picked);}
    @Test void emptyOrInvalidPoolFails(){assertThrows(IllegalStateException.class,()->WeightedPool.choose(new Random(),Map.of("cow",Double.NaN),null));}
    @Test void configuredProbabilitiesMatter(){var r=new Random(17);int rare=0;for(int i=0;i<10000;i++)if(WeightedPool.choose(r,Map.of("cow",1d,"wither",.1),null).equals("wither"))rare++;assertTrue(rare>700&&rare<1100,"rare="+rare);}
    @Test void blockedHugeWeightCannotHideTheOnlyUsableBody(){
        var weights=new LinkedHashMap<String,Double>();weights.put("dragon",Double.MAX_VALUE);weights.put("chicken",Double.MIN_VALUE);
        var checked=new ArrayList<String>();var random=new Random(){@Override public double nextDouble(){return 0;}};
        assertEquals("chicken",WeightedPool.chooseMatching(random,weights,null,id->{checked.add(id);return id.equals("chicken");}));
        assertEquals(List.of("dragon","chicken"),checked);assertEquals(2,weights.size());
    }
    @Test void currentFormIsOnlyCheckedAfterEveryAlternativeIsBlocked(){
        var checked=new ArrayList<String>();var weights=new LinkedHashMap<String,Double>();weights.put("cow",100d);weights.put("dragon",1d);
        assertEquals("cow",WeightedPool.chooseMatching(new Random(1),weights,"cow",id->{checked.add(id);return id.equals("cow");}));
        assertEquals(List.of("dragon","cow"),checked);
        assertEquals("dragon",WeightedPool.chooseMatching(new Random(1),weights,"cow",id->{assertNotEquals("cow",id);return true;}));
    }
    @Test void allBlockedReturnsNullAndVisitsEachPositiveCandidateOnce(){
        var weights=new LinkedHashMap<String,Double>();weights.put("cow",3d);weights.put("bee",1d);weights.put("zero",0d);weights.put("invalid",Double.NaN);
        var checked=new ArrayList<String>();assertNull(WeightedPool.chooseMatching(new Random(3),weights,"bee",id->{checked.add(id);return false;}));
        assertEquals(List.of("cow","bee"),checked);
    }
    @Test void invalidOrZeroWeightCurrentFormCannotBecomeAFallback(){
        var weights=new LinkedHashMap<String,Double>();weights.put("cow",1d);weights.put("bee",0d);
        assertNull(WeightedPool.chooseMatching(new Random(1),weights,"bee",id->{assertEquals("cow",id);return false;}));
    }
    @Test void fittingSpeciesRetainRelativeWeightsEvenWithBlockedCandidates(){
        var weights=new LinkedHashMap<String,Double>();weights.put("blocked",100d);weights.put("cow",1d);weights.put("bee",.1d);
        var random=new Random(27);int rare=0;
        for(int i=0;i<10000;i++)if(WeightedPool.chooseMatching(random,weights,null,id->!id.equals("blocked")).equals("bee"))rare++;
        assertTrue(rare>700&&rare<1100,"rare="+rare);
    }
}
