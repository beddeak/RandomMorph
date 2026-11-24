package kr.randommorph.fabric.core;
import java.util.*;
import java.util.function.Predicate;
/** Scale before summing so hostile/broken huge JSON weights cannot overflow. */
public final class WeightedPool {
    private WeightedPool(){}
    public static <T> T choose(Random random,Map<T,Double> weights,T excluded){
        return chooseMatching(random,weights,excluded,key->true);
    }
    /** Test each positive candidate at most once; a blocked high weight cannot hide a usable form.
     * The current form is the last fallback, and null means every enabled form was rejected. */
    public static <T> T chooseMatching(Random random,Map<T,Double> weights,T excluded,Predicate<T> accepted){
        var choices=new LinkedHashMap<T,Double>();
        weights.forEach((key,value)->{if(value!=null&&Double.isFinite(value)&&value>0)choices.put(key,value);});
        if(choices.isEmpty())throw new IllegalStateException("Random morph pool is empty");
        var previous=choices.remove(excluded);
        while(!choices.isEmpty()){
            var choice=draw(random,choices);choices.remove(choice);
            if(accepted.test(choice))return choice;
        }
        return previous!=null&&accepted.test(excluded)?excluded:null;
    }
    private static <T> T draw(Random random,Map<T,Double> choices){
        double max=Collections.max(choices.values());
        double total=choices.values().stream().mapToDouble(v->v/max).sum(), roll=random.nextDouble()*total;
        T last=null;
        for(var entry:choices.entrySet()){last=entry.getKey();roll-=entry.getValue()/max;if(roll<0)return last;}
        return last;
    }
}
